package com.example.sentencebuilder.ui;

import com.example.sentencebuilder.logic.ImportFileValidator;
import com.example.sentencebuilder.logic.ImportSummary;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Handles event-driven behavior for the current text-file analysis screen.
 * File reading is delegated to a background task so the JavaFX thread stays responsive.
 */
public class ImportController {
    private static final String WORKER_THREAD_NAME = "sentence-builder-file-analysis";

    @FXML
    private TextField filePathField;

    @FXML
    private Button browseButton;

    @FXML
    private Button analyzeButton;

    @FXML
    private Button cancelButton;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label validationLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextArea summaryArea;

    private TextImportTask activeTask;

    /**
     * Sets the initial control state and watches the path field for validation changes.
     */
    @FXML
    public void initialize() {
        filePathField.textProperty().addListener(
                (observable, oldValue, newValue) -> refreshValidation());

        progressBar.setProgress(0);
        cancelButton.setDisable(true);
        statusLabel.setText("Ready.");
        summaryArea.setText("Choose a plain-text file to analyze.");
        refreshValidation();
    }

    /**
     * Opens a file chooser when the user presses Browse.
     */
    @FXML
    protected void onBrowseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a text file");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));

        java.io.File selectedFile = chooser.showOpenDialog(
                filePathField.getScene().getWindow());

        if (selectedFile != null) {
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Validates the selected file and starts analysis on a worker thread.
     */
    @FXML
    protected void onAnalyzeFile() {
        Path sourceFile = getValidatedPath();
        if (sourceFile == null) {
            return;
        }

        TextImportTask task = new TextImportTask(sourceFile);
        activeTask = task;
        setBusy(true);

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            ImportSummary summary = task.getValue();
            finishTask();
            progressBar.setProgress(1);
            statusLabel.setText("Finished reading " + summary.sourceFile().getFileName() + ".");
            summaryArea.setText(formatSummary(summary));
        });

        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            finishTask();
            progressBar.setProgress(0);
            statusLabel.setText("File analysis failed.");
            summaryArea.setText(formatFailure(failure));
        });

        task.setOnCancelled(event -> {
            finishTask();
            progressBar.setProgress(0);
            statusLabel.setText("File analysis cancelled.");
        });

        Thread worker = new Thread(task, WORKER_THREAD_NAME);
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Requests cancellation of the active file-analysis task.
     */
    @FXML
    protected void onCancelAnalysis() {
        if (activeTask != null) {
            activeTask.cancel();
        }
    }

    /**
     * Validates and converts the current path field to a Path.
     *
     * @return valid text-file path, or null when validation fails
     */
    private Path getValidatedPath() {
        String rawPath = filePathField.getText().trim();
        if (rawPath.isEmpty()) {
            validationLabel.setText("Choose or type a plain-text (.txt) file.");
            analyzeButton.setDisable(true);
            return null;
        }

        try {
            Path path = Path.of(rawPath);
            String validationMessage = ImportFileValidator.validate(path);
            if (!validationMessage.isEmpty()) {
                validationLabel.setText(validationMessage);
                analyzeButton.setDisable(true);
                return null;
            }
            return path;
        } catch (InvalidPathException exception) {
            validationLabel.setText("The file path is not valid.");
            analyzeButton.setDisable(true);
            return null;
        }
    }

    /**
     * Updates validation feedback and prevents analysis when the input is invalid.
     */
    private void refreshValidation() {
        if (activeTask != null) {
            analyzeButton.setDisable(true);
            return;
        }

        String rawPath = filePathField.getText().trim();
        if (rawPath.isEmpty()) {
            validationLabel.setText("Choose or type a plain-text (.txt) file.");
            analyzeButton.setDisable(true);
            return;
        }

        try {
            String validationMessage = ImportFileValidator.validate(Path.of(rawPath));
            validationLabel.setText(
                    validationMessage.isEmpty()
                            ? "File is ready to analyze."
                            : validationMessage);
            analyzeButton.setDisable(!validationMessage.isEmpty());
        } catch (InvalidPathException exception) {
            validationLabel.setText("The file path is not valid.");
            analyzeButton.setDisable(true);
        }
    }

    /**
     * Locks controls that should not change while a file is being processed.
     *
     * @param busy true while a background task is active
     */
    private void setBusy(boolean busy) {
        filePathField.setDisable(busy);
        browseButton.setDisable(busy);
        analyzeButton.setDisable(busy);
        cancelButton.setDisable(!busy);
    }

    /**
     * Removes task bindings and restores the normal control state.
     */
    private void finishTask() {
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        activeTask = null;
        setBusy(false);
        refreshValidation();
    }

    /**
     * Formats a completed analysis result for display.
     *
     * @param summary result returned by the background task
     * @return user-facing summary text
     */
    private String formatSummary(ImportSummary summary) {
        return String.format(
                "File: %s%n"
                        + "Lines read: %,d%n"
                        + "Tokens read: %,d%n"
                        + "File size: %,d bytes%n"
                        + "Elapsed time: %,d ms",
                summary.sourceFile().toAbsolutePath(),
                summary.lineCount(),
                summary.tokenCount(),
                summary.fileSizeBytes(),
                summary.elapsedMilliseconds());
    }

    /**
     * Produces a short error message without exposing a stack trace in the interface.
     *
     * @param failure exception raised during file analysis
     * @return user-facing error text
     */
    private String formatFailure(Throwable failure) {
        if (failure == null || failure.getMessage() == null || failure.getMessage().isBlank()) {
            return "The selected file could not be analyzed.";
        }
        return "The selected file could not be analyzed: " + failure.getMessage();
    }
}
