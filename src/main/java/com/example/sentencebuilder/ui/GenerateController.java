package com.example.sentencebuilder.ui;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.GenerationMethod;
import com.example.sentencebuilder.logic.Sentence;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

/**
 * Handles events for the sentence-generation screen.
 * Written by Merit Bhusal (NetID: mxb220125) for CS4485.0W1 Senior Design,
 * Fall 2026, Team 25, Sentence Builder, starting October 26, 2026.
 *
 * <p>This class only reads input and shows results. Every decision about how a
 * sentence is built is delegated to ApplicationController in the logic layer.
 */
public class GenerateController {

    @FXML
    private Label vocabularyLabel;

    @FXML
    private TextField startWordField;

    @FXML
    private ComboBox<GenerationMethod> methodChoice;

    @FXML
    private Spinner<Integer> maxWordsSpinner;

    @FXML
    private Button generateButton;

    @FXML
    private Label messageLabel;

    @FXML
    private Label resultLabel;

    @FXML
    private ListView<String> historyList;

    private ApplicationController applicationController;

    /**
     * Receives the business-logic controller from the application startup code.
     *
     * @param applicationController controller this screen calls to generate sentences
     */
    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
        refresh();
    }

    /**
     * Fills the algorithm drop-down and sets the starting screen text.
     */
    @FXML
    public void initialize() {
        methodChoice.getItems().setAll(GenerationMethod.values());
        methodChoice.getSelectionModel().selectFirst();
        methodChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(GenerationMethod method) {
                if (method == null) {
                    return "";
                }
                return method == GenerationMethod.MOST_COMMON ? "Most common" : "Weighted random";
            }

            @Override
            public GenerationMethod fromString(String text) {
                return null;
            }
        });

        messageLabel.setText("");
        resultLabel.setText("");
        refresh();
    }

    /**
     * Updates the vocabulary size shown at the top. Called when the tab is opened,
     * because a file may have been imported since the last visit.
     */
    public void refresh() {
        if (applicationController == null) {
            vocabularyLabel.setText("Application services are unavailable.");
            setGenerationControlsDisabled(true);
            return;
        }

        int wordCount = applicationController.getKnownWordCount();
        boolean noVocabulary = wordCount == 0;
        vocabularyLabel.setText(noVocabulary
                ? "No words learned yet. Import a text file on the Import tab first."
                : "Known words: " + String.format("%,d", wordCount));
        setGenerationControlsDisabled(noVocabulary);
    }

    /**
     * Keeps actions unavailable until the application has enough data to perform them.
     *
     * @param disabled true when sentence generation should not be available
     */
    private void setGenerationControlsDisabled(boolean disabled) {
        startWordField.setDisable(disabled);
        methodChoice.setDisable(disabled);
        maxWordsSpinner.setDisable(disabled);
        generateButton.setDisable(disabled);
    }

    /**
     * Generates one sentence from the start word, method, and length the user chose.
     */
    @FXML
    protected void onGenerate() {
        if (applicationController == null) {
            messageLabel.setText("Application services are unavailable.");
            return;
        }

        String startWord = startWordField.getText().trim();
        if (startWord.isEmpty()) {
            messageLabel.setText("Type a start word first.");
            return;
        }
        if (applicationController.getKnownWordCount() == 0) {
            messageLabel.setText("Import a text file on the Import tab before generating.");
            return;
        }

        try {
            maxWordsSpinner.commitValue();
            int maxWords = maxWordsSpinner.getValue();
            GenerationMethod method = methodChoice.getValue();

            Sentence sentence = applicationController.generateSentence(startWord, maxWords, method);
            String text = sentence.asText();

            long timesSeen = applicationController.getGeneratedSentences().stream()
                    .filter(earlier -> earlier.asText().equals(text))
                    .count();

            resultLabel.setText(text);
            messageLabel.setText(timesSeen > 1
                    ? "This exact sentence has been generated before."
                    : "");
            historyList.getItems().add(0, text + "   [" + methodChoice.getConverter().toString(method) + "]");
        } catch (IllegalArgumentException exception) {
            resultLabel.setText("");
            messageLabel.setText(exception.getMessage());
        }
    }
}