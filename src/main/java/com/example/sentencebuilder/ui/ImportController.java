package com.example.sentencebuilder.ui;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.ImportFileValidator;
import com.example.sentencebuilder.logic.ImportSummary;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;

/**
 * I handle file validation, import progress, cancellation, and results for
 * Sentence Builder's Import tab. I delegate file reads and repository work
 * to a JavaFX Task so the interface stays responsive.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started this module on October 9,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I integrated vocabulary on
 * October 16 and 23, and coordinated the screens on October 30, 2026.
 * AI prompt: Create a JavaFX text-file import screen with validation,
 * cancellation, background progress, status messages, and result summaries.
 */
public class ImportController {
    private static final String WORKER_THREAD_NAME = "sentence-builder-file-import";

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

    /** I retain the active task to prevent overlapping imports. */
    private TextImportTask activeTask;
    /** I receive shared application logic from startup. */
    private ApplicationController applicationController;
    /** I notify the app shell without depending on another screen's controller. */
    private Runnable onImportCompleted = () -> { };

    /**
     * I receive the shared business-logic controller from application startup.
     *
     * @param applicationController controller this screen should call instead of
     *                              reading files or touching storage itself
     */
    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
    }

    /**
     * I register a callback to notify the application shell after an import.
     * I leave cross-screen navigation in the shell to avoid tight coupling.
     *
     * @param onImportCompleted action to run after a successful import
     */
    public void setOnImportCompleted(Runnable onImportCompleted) {
        this.onImportCompleted = Objects.requireNonNull(onImportCompleted, "onImportCompleted");
    }

    /**
     * I initialize control states and watch the file path for validation changes.
     */
    @FXML
    public void initialize() {
        filePathField.textProperty().addListener(
                (observable, oldValue, newValue) -> refreshValidation());

        progressBar.setProgress(0);
        cancelButton.setDisable(true);
        statusLabel.setText("Ready.");
        summaryArea.setText("Choose a plain-text file to import.");
        refreshValidation();
    }

    /**
     * I open a file chooser when the user selects Browse.
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
     * I validate the selected file and start a worker thread for processing.
     */
    @FXML
    protected void onAnalyzeFile() {
        Path sourceFile = getValidatedPath();
        if (sourceFile == null) {
            return;
        }

        if (applicationController == null) {
            statusLabel.setText("Application services are unavailable.");
            summaryArea.setText("The file could not be processed because the application controller is not connected.");
            return;
        }

        // I create a separate Task for each import because JavaFX Tasks are
        // single-use. I lock the controls until the task finishes.
        TextImportTask task = new TextImportTask(sourceFile, applicationController);
        activeTask = task;
        setBusy(true);

        // I bind Task properties so worker progress is displayed safely in JavaFX.
        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        // I handle task completion on the JavaFX thread so I can restore the
        // controls and trigger the next screen refresh safely.
        task.setOnSucceeded(event -> {
            ImportSummary summary = task.getValue();
            finishTask();
            progressBar.setProgress(1);
            statusLabel.setText("Imported " + summary.sourceFile().getFileName() + ".");
            summaryArea.setText(formatSummary(summary));
            onImportCompleted.run();
        });

        task.setOnFailed(event -> {
            Throwable failure = task.getException();
            finishTask();
            progressBar.setProgress(0);
            statusLabel.setText("File processing failed.");
            summaryArea.setText(formatFailure(failure));
        });

        task.setOnCancelled(event -> {
            finishTask();
            progressBar.setProgress(0);
            statusLabel.setText("File processing cancelled.");
        });

        // I use a daemon thread so an import cannot keep the app alive after exit.
        Thread worker = new Thread(task, WORKER_THREAD_NAME);
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * I request cancellation of the active file-processing task.
     */
    @FXML
    protected void onCancelAnalysis() {
        if (activeTask != null) {
            activeTask.cancel();
        }
    }

    /**
     * I validate the current path field and convert it into a Path.
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
     * I update validation feedback and disable imports when the input is invalid.
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
     * I lock controls that must not change while a file is being processed.
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
     * I remove task bindings and restore the normal control state.
     */
    private void finishTask() {
        // I unbind Task properties before resetting the display so I can
        // prepare for a new attempt after success, failure, or cancellation.
        progressBar.progressProperty().unbind();
        statusLabel.textProperty().unbind();
        activeTask = null;
        setBusy(false);
        refreshValidation();
    }

    /**
     * I format a completed file-import summary for display.
     *
     * @param summary result returned by the background task
     * @return user-facing summary text
     */
    private String formatSummary(ImportSummary summary) {
        return String.format(
                "File: %s%n"
                        + "Lines read: %,d%n"
                        + "Recognized words: %,d%n"
                        + "Distinct words: %,d%n"
                        + "File size: %,d bytes%n"
                        + "Elapsed time: %,d ms",
                summary.sourceFile().toAbsolutePath(),
                summary.lineCount(),
                summary.tokenCount(),
                summary.distinctWordCount(),
                summary.fileSizeBytes(),
                summary.elapsedMilliseconds());
    }

    /**
     * I show a short error message without exposing a stack trace in the UI.
     *
     * @param failure exception raised during file processing
     * @return user-facing error text
     */
    private String formatFailure(Throwable failure) {
        if (failure == null || failure.getMessage() == null || failure.getMessage().isBlank()) {
            return "The selected file could not be processed.";
        }
        return "The selected file could not be processed: " + failure.getMessage();
    }
}
