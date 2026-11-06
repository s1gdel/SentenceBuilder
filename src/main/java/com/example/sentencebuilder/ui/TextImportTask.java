package com.example.sentencebuilder.ui;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.ImportSummary;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Runs file reading and any associated repository writes off the JavaFX
 * application thread. Task message/progress properties are observed by the
 * controller, so workers never modify scene controls directly.
 *
 * <p>Written by Jinesh Patel for CS4485.0W1 Senior Design, Team 25,
 * starting October 9, 2026. NetID: JAP220022.
 * Shared-controller delegation added October 23, 2026.
 */
public class TextImportTask extends Task<ImportSummary> {
    private final Path sourceFile;
    private final ApplicationController applicationController;

    /**
     * Creates a background task for one validated text file.
     *
     * @param sourceFile text file selected by the user
     * @param applicationController shared business-logic controller
     */
    public TextImportTask(Path sourceFile, ApplicationController applicationController) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.applicationController = Objects.requireNonNull(
                applicationController,
                "applicationController");
    }

    /**
     * Imports the selected file through the business-logic controller and
     * publishes progress/status updates to JavaFX.
     *
     * @return summary of the completed file import
     * @throws Exception if the selected file cannot be read or stored
     */
    @Override
    protected ImportSummary call() throws Exception {
        updateMessage("Preparing " + sourceFile.getFileName() + "...");

        // The task handles long-running work and publishes a progress fraction
        // to JavaFX; the UI does not need to manage file read operations.
        return applicationController.importFile(
                sourceFile,
                this::isCancelled,
                (completedLines, totalLines) -> {
                    // Empty files should still complete the progress bar rather
                    // than dividing by zero or staying indeterminate.
                    if (totalLines == 0) {
                        updateProgress(1, 1);
                    } else {
                        updateProgress(completedLines, totalLines);
                    }

                    updateMessage(String.format(
                            "Processing %s: %,d of %,d lines",
                            sourceFile.getFileName(),
                            completedLines,
                            totalLines));
                });
    }
}
