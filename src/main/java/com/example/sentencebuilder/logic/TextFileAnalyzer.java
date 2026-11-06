package com.example.sentencebuilder.logic;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * I read UTF-8 files using the team's tokenizer and vocabulary rules.
 * I count lines first for determinate progress, then analyze a second pass
 * and stage vocabulary changes in the supplied object.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this module on September 25,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I integrated vocabulary on
 * October 16 and the shared controller on October 23, 2026.
 * AI prompt: Analyze UTF-8 text files with the project's tokenizer and
 * vocabulary rules, progress callbacks, and cancellation support.
 */
public class TextFileAnalyzer {

    /**
     * I read a text file into a temporary vocabulary and report progress.
     * I keep this overload for callers that do not supply a shared vocabulary.
     *
     * @param source file to read using UTF-8
     * @param cancelled callback used to stop work when the user cancels
     * @param progress callback receiving completed lines and total lines
     * @return immutable summary of the completed read
     * @throws IOException if the file cannot be read
     * @throws CancellationException if cancellation is requested
     */
    public ImportSummary analyze(
            Path source,
            BooleanSupplier cancelled,
            BiConsumer<Long, Long> progress) throws IOException {
        return analyze(source, new Vocabulary(), cancelled, progress);
    }

    /**
     * I read a text file into the supplied vocabulary while reporting progress.
     * I return counts for this file alone, even if the shared vocabulary
     * contains words learned from earlier files.
     *
     * @param source file to read using UTF-8
     * @param vocabulary application vocabulary that should learn the imported text
     * @param cancelled callback used to stop work when the user cancels
     * @param progress callback receiving completed lines and total lines
     * @return immutable summary of the completed read
     * @throws IOException if the file cannot be read
     * @throws CancellationException if cancellation is requested
     */
    public ImportSummary analyze(
            Path source,
            Vocabulary vocabulary,
            BooleanSupplier cancelled,
            BiConsumer<Long, Long> progress) throws IOException {

        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(vocabulary, "vocabulary");
        Objects.requireNonNull(cancelled, "cancelled");
        Objects.requireNonNull(progress, "progress");

        // I count lines before analysis to make progress percentages useful
        // even for large books processed on a background thread.
        long startNanos = System.nanoTime();
        long totalLines = countLines(source, cancelled);
        long completedLines = 0;
        long tokenCount = 0;
        Set<String> distinctWordsInFile = new HashSet<>();

        progress.accept(0L, totalLines);
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                checkCancelled(cancelled);

                // I reuse the Vocabulary tokenization rules for file-local counts.
                var tokens = ImportTokenizer.tokenizeLine(line);
                tokenCount += tokens.size();
                distinctWordsInFile.addAll(tokens);

                // I learn into the supplied (typically staged) vocabulary;
                // I leave the decision to merge only on success to the controller.
                vocabulary.learnFromLine(line);

                completedLines++;
                progress.accept(completedLines, totalLines);
            }
        }

        long elapsedMilliseconds = (System.nanoTime() - startNanos) / 1_000_000L;
        return new ImportSummary(
                source,
                completedLines,
                tokenCount,
                distinctWordsInFile.size(),
                Files.size(source),
                elapsedMilliseconds);
    }

    /**
     * I count lines in a first pass so the second can report progress.
     *
     * @param source file being analyzed
     * @param cancelled cancellation callback
     * @return number of lines in the file
     * @throws IOException if the file cannot be read
     */
    private long countLines(Path source, BooleanSupplier cancelled) throws IOException {
        long lineCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
            while (reader.readLine() != null) {
                // I check cancellation in the counting pass as well as analysis
                // so the user can stop long imports promptly.
                checkCancelled(cancelled);
                lineCount++;
            }
        }
        return lineCount;
    }

    /**
     * I stop analysis promptly when the JavaFX task has been cancelled.
     *
     * @param cancelled callback that reports cancellation state
     */
    private void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Text file analysis was cancelled.");
        }
    }
}
