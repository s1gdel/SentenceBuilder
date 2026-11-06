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
 * Reads UTF-8 files using the team's tokenizer and vocabulary rules.
 * An initial pass counts lines for determinate progress; a second pass
 * analyzes text and stages vocabulary changes in the supplied object.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this module on September 25,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I integrated vocabulary on
 * October 16 and the shared controller on October 23, 2026.
 * AI prompt: Analyze UTF-8 text files with the project's tokenizer and
 * vocabulary rules, progress callbacks, and cancellation support.
 */
public class TextFileAnalyzer {

    /**
     * Reads a text file into a temporary vocabulary and reports progress.
     * This overload supports callers without a shared vocabulary instance.
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
     * Reads a text file into the supplied vocabulary while reporting progress.
     * Returned counts describe only the current file, even if the vocabulary
     * already contains words learned from earlier imports.
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

        // Count lines before analysis to enable meaningful progress percentages
        // for large files processed on a background thread.
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

                // Reuse the Vocabulary tokenization rules for file-local counts.
                var tokens = ImportTokenizer.tokenizeLine(line);
                tokenCount += tokens.size();
                distinctWordsInFile.addAll(tokens);

                // Learn into the supplied (typically staged) vocabulary. The
                // application controller determines when a successful merge occurs.
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
     * Counts lines in the first pass to support progress during the second.
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
                // Check cancellation during both counting and analysis so long
                // imports can be stopped promptly.
                checkCancelled(cancelled);
                lineCount++;
            }
        }
        return lineCount;
    }

    /**
     * Stops analysis promptly when cancellation is requested.
     *
     * @param cancelled callback that reports cancellation state
     */
    private void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Text file analysis was cancelled.");
        }
    }
}
