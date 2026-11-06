package com.example.sentencebuilder.logic;

import java.nio.file.Path;

/**
 * Immutable values returned from the import worker for the JavaFX summary.
 * The UI can display progress results without rereading the source file or
 * knowing the internal vocabulary data structure.
 *
 * <p>Written by Jinesh Patel for CS4485.0W1 Senior Design, Team 25,
 * starting September 25, 2026. NetID: JAP220022.
 * Distinct-word reporting added October 16, 2026.
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
