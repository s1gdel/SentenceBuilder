package com.example.sentencebuilder.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * I validate paths selected for the Sentence Builder file-import screen.
 * I reuse these checks in both JavaFX and the application controller so
 * callers cannot bypass file prerequisites.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this class on September 25,
 * 2026 for CS4485.0W1 Senior Design, Team 25.
 * AI prompt: Validate selected text-file paths for existence,
 * readability, file type, and the .txt extension.
 */
public final class ImportFileValidator {
    private ImportFileValidator() {
    }

    /**
     * I check whether a path points to a readable plain-text file.
     *
     * @param path path selected by the user
     * @return an empty string when valid, otherwise a user-facing error message
     */
    public static String validate(Path path) {
        // I report a specific failed precondition before starting the import
        // instead of failing later with an unhelpful generic error.
        if (path == null) {
            return "Choose a text file before importing.";
        }
        if (!Files.exists(path)) {
            return "The selected file does not exist.";
        }
        if (!Files.isRegularFile(path)) {
            return "The selected path is not a file.";
        }
        if (!Files.isReadable(path)) {
            return "The selected file cannot be read.";
        }

        // I enforce the current .txt filename requirement, but I do not
        // assume that an extension alone proves the file's contents.
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".txt")) {
            return "Choose a plain-text (.txt) file.";
        }
        return "";
    }
}
