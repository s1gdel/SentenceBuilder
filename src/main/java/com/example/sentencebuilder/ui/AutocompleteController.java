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
 * Provides next-word autocomplete through JavaFX without duplicating word logic.
 *
 * <p>After a user completes a word with a space, period, or comma, this screen
 * asks the shared application controller for ranked follower suggestions. If a
 * manually entered word is unknown, it asks permission before learning it.
 *
 * <p>Sentence Builder, CS4485.0W1 Senior Design, Team 25.
 * UI implementation by Jinesh Patel, started October 30, 2026.
 * NetID: JAP220022.
 * Unknown-word confirmation added November 6, 2026.
 */
public class AutocompleteController {
    /** Limit the list to a useful size without overwhelming the text editor. */
    private static final int MAX_SUGGESTIONS = 5;

    @FXML private Label vocabularyLabel;
    @FXML private TextArea sentenceArea;
    @FXML private ListView<String> suggestionList;
    @FXML private Button useSuggestionButton;
    @FXML private Label statusLabel;

    /** Shared application logic is injected by startup, never created by this UI. */
    private ApplicationController applicationController;

    /** Keep the model objects aligned with the displayed strings by list index. */
    private List<WordSuggestion> currentSuggestions = List.of();

    /** Ignore changes caused by inserting a selected suggestion programmatically. */
    private boolean suppressTextEvents;

    /**
     * Makes the shared vocabulary and suggestion service available to this screen.
     *
     * @param applicationController logic layer used by Import/Generate/Autocomplete
     */
    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = Objects.requireNonNull(
                applicationController, "applicationController");
        refresh();
    }

    /**
     * Subscribes to editor and selection changes and sets initial control states.
     * Listening for completed tokens avoids guessing a word mid-typing.
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
     * Updates availability and suggestions after an import or tab switch.
     * The editor is unavailable if no vocabulary data exists yet.
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
     * Applies the selected known follower and sends the choice to the logic layer.
     * Internal text changes are suppressed to avoid recording a second choice.
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

        // A selected suggestion always follows the completed word on which the
        // suggestion list was based; recording keeps its ranking up to date.
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
     * Handles a completed user-typed word. Unknown words require confirmation,
     * including when that word is the first one in the editor. No response or a
     * No response leaves the vocabulary unchanged.
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

        // This initial UI supports normal typing at the end of the text area.
        // Editing earlier text does not silently replay old learning events.
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

        // Extra whitespace/punctuation after a delimiter is not another word.
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
                // This records either a known manually typed follower or an
                // unfamiliar word that the user explicitly approved.
                applicationController.recordSuggestionChoice(previousWord, completedWord);
            } else if (!knownWord) {
                // The first word (or a word after a declined unknown word) has
                // no valid previous follower; save it without inventing a pair.
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
     * Gives the user control before adding any unfamiliar word to storage.
     * The dialog defaults to No so dismissing it does not approve a new word.
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
     * Refreshes suggestions for the latest completed word when this tab is opened.
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
     * Displays a ranked list supplied by business logic and clears the previous
     * selection so the button cannot apply a stale suggestion.
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
     * Normalizes words using the project's existing import rules. Separating at
     * commas and periods agrees with the keys used for autocomplete completion.
     *
     * @param text editor content
     * @return normalized word tokens, in reading order
     */
    private List<String> completedWords(String text) {
        return ImportTokenizer.tokenizeLine(text.replace(',', ' ').replace('.', ' '));
    }

    /**
     * Recognizes exactly the three completion keys specified for the UI.
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
     * Removes stale choices when the input no longer ends at a completed word.
     */
    private void clearSuggestions() {
        currentSuggestions = List.of();
        suggestionList.getItems().clear();
        useSuggestionButton.setDisable(true);
    }

    /**
     * Disables dependent controls until a vocabulary has been imported or loaded.
     *
     * @param disabled true when there are no words to offer
     */
    private void setControlsDisabled(boolean disabled) {
        sentenceArea.setDisable(disabled);
        suggestionList.setDisable(disabled);
        useSuggestionButton.setDisable(disabled || suggestionList.getSelectionModel().isEmpty());
    }
}
