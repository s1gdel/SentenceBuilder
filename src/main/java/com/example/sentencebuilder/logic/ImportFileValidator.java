package com.example.sentencebuilder.logic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Validates plain-text paths selected for the Sentence Builder import screen.
 * Both the JavaFX UI and the application controller use this same validation
 * so file imports cannot bypass the required input checks.
 *
 * <p>Written by Jinesh Patel for CS4485.0W1 Senior Design, Team 25,
 * starting September 25, 2026. NetID: JAP220022.
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
        // Report a specific failed precondition so the screen does not start
        // an import that cannot succeed or merely display a generic error.
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

        // The project currently accepts plain-text filenames only; checking the
        // extension is an input rule, not a claim to inspect file contents.
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!fileName.endsWith(".txt")) {
            return "Choose a plain-text (.txt) file.";
        }
        return "";
    }
}
