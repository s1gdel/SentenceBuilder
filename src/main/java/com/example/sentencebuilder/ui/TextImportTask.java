package com.example.sentencebuilder.ui;

import com.example.sentencebuilder.logic.ImportSummary;
import com.example.sentencebuilder.logic.TextFileAnalyzer;
import javafx.concurrent.Task;

import java.nio.file.Path;

/**
 * Runs text-file analysis away from the JavaFX application thread.
 */
public class TextImportTask extends Task<ImportSummary> {
    private final Path sourceFile;
    private final TextFileAnalyzer analyzer;

    /**
     * Creates a background task for one validated text file.
     *
     * @param sourceFile text file selected by the user
     */
    public TextImportTask(Path sourceFile) {
        this.sourceFile = sourceFile;
        this.analyzer = new TextFileAnalyzer();
    }

    /**
     * Reads the selected file and publishes progress/status updates to JavaFX.
     *
     * @return summary of the completed file analysis
     * @throws Exception if the selected file cannot be read
     */
    @Override
    protected ImportSummary call() throws Exception {
        updateMessage("Preparing " + sourceFile.getFileName() + "...");

        return analyzer.analyze(
                sourceFile,
                this::isCancelled,
                (completedLines, totalLines) -> {
                    if (totalLines == 0) {
                        updateProgress(1, 1);
                    } else {
                        updateProgress(completedLines, totalLines);
                    }

                    updateMessage(String.format(
                            "Reading %s: %,d of %,d lines",
                            sourceFile.getFileName(),
                            completedLines,
                            totalLines));
                });
    }
}
