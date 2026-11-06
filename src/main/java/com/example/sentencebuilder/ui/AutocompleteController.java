package com.example.sentencebuilder.ui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.ImportTokenizer;
import com.example.sentencebuilder.logic.StorageException;
import com.example.sentencebuilder.logic.WordSuggestion;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

/**
 * I provide next-word autocomplete through JavaFX without duplicating business logic.
 *
 * <p>I wait for a completed word (space, period, or comma), then request ranked
 * follower suggestions from the shared application controller. When I detect
 * an unfamiliar typed word, I ask the user before learning it.
 *
 * <p>Sentence Builder, CS4485.0W1 Senior Design, Team 25.
 * I, Jinesh Patel (NetID: JAP220022), started this UI on October 30, 2026.
 * I added unknown-word confirmation on November 6, 2026.
 * AI prompt: Build a JavaFX autocomplete tab using the existing next-word
 * suggestions and record confirmed user choices after completed words.
 */
public class AutocompleteController {
    /** I limit the suggestions to avoid overwhelming the text editor. */
    private static final int MAX_SUGGESTIONS = 5;

    @FXML private Label vocabularyLabel;
    @FXML private TextArea sentenceArea;
    @FXML private ListView<String> suggestionList;
    @FXML private Button useSuggestionButton;
    @FXML private Label statusLabel;

    /** I receive shared application logic from startup rather than creating it here. */
    private ApplicationController applicationController;

    /** I keep each suggestion model aligned with its displayed list index. */
    private List<WordSuggestion> currentSuggestions = List.of();

    /** I ignore text events triggered when I insert a selected suggestion. */
    private boolean suppressTextEvents;

    /**
     * I connect the shared vocabulary and suggestion service to this screen.
     *
     * @param applicationController logic layer used by Import/Generate/Autocomplete
     */
    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = Objects.requireNonNull(
                applicationController, "applicationController");
        refresh();
    }

    /**
     * I subscribe to editor and selection changes, and initialize control states.
     * I listen for completed words instead of guessing in the middle of typing.
     */
    @FXML
    public void initialize() {
        sentenceArea.textProperty().addListener(
                (observable, oldText, newText) -> onTextChanged(oldText, newText));
        suggestionList.getSelectionModel().selectedIndexProperty().addListener(
                (observable, oldIndex, newIndex) ->
                        useSuggestionButton.setDisable(
                                newIndex == null || newIndex.intValue() < 0));
        useSuggestionButton.setDisable(true);
        statusLabel.setText("Import text first, then type a word to begin.");
    }

    /**
     * I refresh availability and suggestions after imports or tab changes.
     * I disable the editor when no vocabulary is available.
     */
    public void refresh() {
        if (applicationController == null) {
            vocabularyLabel.setText("Application services are unavailable.");
            setControlsDisabled(true);
            clearSuggestions();
            return;
        }

        int wordCount = applicationController.getKnownWordCount();
        boolean noVocabulary = wordCount == 0;
        vocabularyLabel.setText(noVocabulary
                ? "No words learned yet. Import a text file first."
                : "Known words: " + String.format("%,d", wordCount));
        setControlsDisabled(noVocabulary);

        if (noVocabulary) {
            clearSuggestions();
            statusLabel.setText("Import a text file before using auto-complete.");
        } else if (sentenceArea.getText().isBlank()) {
            statusLabel.setText(
                    "Type a word, then press space, period, or comma for next-word choices.");
        } else if (endsWithCompletionDelimiter(sentenceArea.getText())) {
            showSuggestionsForLastCompletedWord();
        }
    }

    /**
     * I insert the selected follower and record the choice through shared logic.
     * I suppress my own text changes to avoid counting the same choice twice.
     */
    @FXML
    protected void onUseSuggestion() {
        if (applicationController == null) {
            return;
        }

        int index = suggestionList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= currentSuggestions.size()) {
            statusLabel.setText("Select a suggested word first.");
            return;
        }

        List<String> words = completedWords(sentenceArea.getText());
        if (words.isEmpty()) {
            statusLabel.setText("Complete a word before choosing a suggestion.");
            return;
        }

        String previousWord = words.get(words.size() - 1);
        String selectedWord = currentSuggestions.get(index).wordText();

        // I record the selected follower of the last completed word so the
        // next-word ranking can learn from this choice.
        try {
            applicationController.recordSuggestionChoice(previousWord, selectedWord);
        } catch (StorageException exception) {
            statusLabel.setText("Could not save the selected word: " + exception.getMessage());
            return;
        }

        String text = sentenceArea.getText();
        StringBuilder updated = new StringBuilder(text);
        if (!text.isEmpty() && !Character.isWhitespace(text.charAt(text.length() - 1))) {
            updated.append(' ');
        }
        updated.append(selectedWord).append(' ');

        suppressTextEvents = true;
        try {
            sentenceArea.setText(updated.toString());
            sentenceArea.positionCaret(sentenceArea.getText().length());
        } finally {
            suppressTextEvents = false;
        }

        showSuggestions(selectedWord);
    }

    /**
     * I handle each newly completed typed word, including the first word.
     * I ask before adding an unknown word; No or dismissal causes no write.
     *
     * @param oldText content before the user edit
     * @param newText content after the user edit
     */
    private void onTextChanged(String oldText, String newText) {
        if (suppressTextEvents || applicationController == null
                || applicationController.getKnownWordCount() == 0) {
            return;
        }

        if (newText.isBlank()) {
            clearSuggestions();
            statusLabel.setText("Type a word, then press space, period, or comma.");
            return;
        }

        // I process append-only typing for now. If a user rewrites earlier
        // text, I avoid replaying old learning events as new word choices.
        if (!newText.startsWith(oldText)) {
            clearSuggestions();
            return;
        }
        if (!endsWithCompletionDelimiter(newText)) {
            clearSuggestions();
            return;
        }

        List<String> words = completedWords(newText);
        if (words.isEmpty()) {
            clearSuggestions();
            return;
        }

        String completedWord = words.get(words.size() - 1);

        // I do not count extra whitespace or punctuation as another word.
        if (endsWithCompletionDelimiter(oldText)) {
            showSuggestions(completedWord);
            return;
        }

        String previousWord = words.size() >= 2 ? words.get(words.size() - 2) : null;
        boolean knownWord = applicationController.isKnownWord(completedWord);

        if (!knownWord && !confirmAddUnknownWord(completedWord)) {
            clearSuggestions();
            statusLabel.setText(
                    "\"" + completedWord + "\" was not added to the vocabulary.");
            return;
        }

        try {
            if (previousWord != null && applicationController.isKnownWord(previousWord)) {
                // I record an existing word or a new follower that the user
                // explicitly approved in the confirmation dialog.
                applicationController.recordSuggestionChoice(previousWord, completedWord);
            } else if (!knownWord) {
                // I save a confirmed first word without inventing a follower
                // link when there is no valid previous word.
                applicationController.addUserWord(completedWord);
            }
        } catch (StorageException exception) {
            clearSuggestions();
            statusLabel.setText("The word could not be saved: " + exception.getMessage());
            return;
        }

        showSuggestions(completedWord);
    }

    /**
     * I request confirmation before an unfamiliar word is added to storage.
     * I use No as the default so dismissing the dialog does not approve it.
     * AI prompt: Ask Yes or No when autocomplete encounters an unknown word,
     * and add the word only when the user explicitly agrees.
     *
     * @param wordText unfamiliar normalized word
     * @return true only when the user explicitly chooses Yes
     */
    private boolean confirmAddUnknownWord(String wordText) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Unknown word");
        confirmation.setHeaderText("Add \"" + wordText + "\" to the vocabulary?");
        confirmation.setContentText(
                "This word is not recognized. Would you like to add it?");
        confirmation.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
        confirmation.getDialogPane().lookupButton(ButtonType.NO).requestFocus();

        Optional<ButtonType> response = confirmation.showAndWait();
        return response.isPresent() && response.get() == ButtonType.YES;
    }

    /**
     * I refresh suggestions for the latest completed word when this tab opens.
     */
    private void showSuggestionsForLastCompletedWord() {
        List<String> words = completedWords(sentenceArea.getText());
        if (words.isEmpty()) {
            clearSuggestions();
            return;
        }
        showSuggestions(words.get(words.size() - 1));
    }

    /**
     * I display the ranked followers supplied by shared logic and clear the
     * previous selection so a stale suggestion cannot be applied.
     *
     * @param previousWord normalized completed word
     */
    private void showSuggestions(String previousWord) {
        currentSuggestions = applicationController.suggestNextWords(
                previousWord, MAX_SUGGESTIONS);

        suggestionList.getItems().clear();
        for (WordSuggestion suggestion : currentSuggestions) {
            suggestionList.getItems().add(String.format(
                    "%s  (seen %,d time%s)",
                    suggestion.wordText(),
                    suggestion.followerCount(),
                    suggestion.followerCount() == 1 ? "" : "s"));
        }

        suggestionList.getSelectionModel().clearSelection();
        useSuggestionButton.setDisable(true);
        statusLabel.setText(currentSuggestions.isEmpty()
                ? "No saved followers for \"" + previousWord + "\". Type the next word yourself."
                : "Choose a next word for \"" + previousWord + "\", or type your own.");
    }

    /**
     * I normalize words with the project's existing import rules. I treat
     * commas and periods as word separators for autocomplete completion.
     *
     * @param text editor content
     * @return normalized word tokens, in reading order
     */
    private List<String> completedWords(String text) {
        return ImportTokenizer.tokenizeLine(text.replace(',', ' ').replace('.', ' '));
    }

    /**
     * I recognize only space, period, and comma as completion keys.
     *
     * @param text current editor text
     * @return whether the last character is a completion delimiter
     */
    private boolean endsWithCompletionDelimiter(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char last = text.charAt(text.length() - 1);
        return last == ' ' || last == '.' || last == ',';
    }

    /**
     * I clear stale choices when the input no longer ends at a completed word.
     */
    private void clearSuggestions() {
        currentSuggestions = List.of();
        suggestionList.getItems().clear();
        useSuggestionButton.setDisable(true);
    }

    /**
     * I disable dependent controls until vocabulary has been imported or loaded.
     *
     * @param disabled true when there are no words to offer
     */
    private void setControlsDisabled(boolean disabled) {
        sentenceArea.setDisable(disabled);
        suggestionList.setDisable(disabled);
        useSuggestionButton.setDisable(disabled || suggestionList.getSelectionModel().isEmpty());
    }
}
