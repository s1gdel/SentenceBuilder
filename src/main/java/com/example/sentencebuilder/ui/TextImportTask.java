package com.example.sentencebuilder.ui;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.ImportSummary;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.Objects;

/**
 * I run file reads and repository writes off the JavaFX application thread.
 * I publish progress and status through Task properties instead of touching
 * JavaFX controls from the worker.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this task on October 9,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I connected it to the
 * shared application controller on October 23, 2026.
 * AI prompt: Run file import on a JavaFX background Task that reports
 * progress, supports cancellation, and uses the shared application controller.
 */
public class TextImportTask extends Task<ImportSummary> {
    private final Path sourceFile;
    private final ApplicationController applicationController;

    /**
     * I create a background task for a previously validated text file.
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
     * I import the selected file through shared business logic and publish
     * progress and status updates for the JavaFX screen.
     *
     * @return summary of the completed file import
     * @throws Exception if the selected file cannot be read or stored
     */
    @Override
    protected ImportSummary call() throws Exception {
        updateMessage("Preparing " + sourceFile.getFileName() + "...");

        // I keep long-running file work inside the Task and publish progress
        // so the UI never has to manage the file-reading loop.
        return applicationController.importFile(
                sourceFile,
                this::isCancelled,
                (completedLines, totalLines) -> {
                    // I treat an empty file as completed progress rather than
                    // dividing by zero or leaving the bar indeterminate.
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
