package com.example.sentencebuilder.logic;

import java.nio.file.Path;

/**
 * I return immutable import results to the JavaFX summary screen.
 * I keep the file-local counts separate from the vocabulary so the UI does
 * not need to reread the source or inspect internal data structures.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this record on September 25,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I added distinct-word
 * reporting on October 16, 2026.
 * AI prompt: Create an immutable import summary with source path, line and
 * word counts, unique word count, file size, and elapsed time.
 *
 * @param sourceFile imported text file
 * @param lineCount number of source lines processed
 * @param tokenCount total tokens recognized by the project tokenizer
 * @param distinctWordCount unique normalized words in this file
 * @param fileSizeBytes source file size
 * @param elapsedMilliseconds total analysis duration
 */
public record ImportSummary(
        Path sourceFile,
        long lineCount,
        long tokenCount,
        int distinctWordCount,
        long fileSizeBytes,
        long elapsedMilliseconds) {
}
