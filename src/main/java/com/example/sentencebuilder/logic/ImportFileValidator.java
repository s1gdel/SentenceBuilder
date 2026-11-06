package com.example.sentencebuilder.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Validates paths selected for the Sentence Builder file-import screen.
 * Validation is shared by JavaFX and the application controller so callers
 * cannot bypass required file checks.
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
     * Checks whether a path points to a readable plain-text file.
     *
     * @param path path selected by the user
     * @return an empty string when valid, otherwise a user-facing error message
     */
    public static String validate(Path path) {
        // Report a specific failed precondition before starting the import
        // rather than failing later with a generic error.
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

        // Enforce the current .txt filename requirement without assuming
        // that the extension proves the file's actual contents.
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".txt")) {
            return "Choose a plain-text (.txt) file.";
        }
        return "";
    }
}
