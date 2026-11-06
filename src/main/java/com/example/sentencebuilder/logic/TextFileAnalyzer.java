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
 * Reads UTF-8 text using the shared tokenizer and vocabulary rules. One pass
 * counts lines for a determinate progress bar; a second pass builds the summary
 * and stages vocabulary changes in the object supplied by the application.
 *
 * <p>Written by Jinesh Patel for CS4485.0W1 Senior Design, Team 25,
 * starting September 25, 2026. NetID: JAP220022.
 * Vocabulary integration revised October 16, 2026 and shared-controller import
 * integration revised October 23, 2026.
 */
public class TextFileAnalyzer {

    /**
     * Reads a text file into a temporary vocabulary and reports progress.
     * This overload preserves the standalone analysis behavior used before the
     * application controller began owning the import vocabulary.
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
     * Reads a text file into the supplied application vocabulary and reports progress.
     * The returned counts describe only the current file even when the vocabulary
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

        // Counting first makes progress percentages meaningful for large books
        // while leaving the JavaFX application thread responsive.
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

                // Count this file using the same tokenization rules used by Vocabulary.
                var tokens = ImportTokenizer.tokenizeLine(line);
                tokenCount += tokens.size();
                distinctWordsInFile.addAll(tokens);

                // Learn into the supplied vocabulary, normally a temporary
                // staging object. The controller merges it only on success.
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
     * Counts lines in a first pass so the second pass can report determinate progress.
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
                // Cancellation also applies to the counting pass, not only the
                // later analysis pass, so the user can stop large imports early.
                checkCancelled(cancelled);
                lineCount++;
            }
        }
        return lineCount;
    }

    /**
     * Ends analysis promptly when the JavaFX task has been cancelled.
     *
     * @param cancelled callback that reports cancellation state
     */
    private void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) {
            throw new CancellationException("Text file analysis was cancelled.");
        }
    }
}
