package com.example.sentencebuilder.ui;

import java.util.List;

import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.logic.ImportTokenizer;
import com.example.sentencebuilder.logic.WordSuggestion;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;

/**
 * Handles the event-driven auto-complete screen.
 *
 * <p>Suggestions are requested only after the user completes a word with a
 * space, period, or comma. The controller delegates ranking and learning to
 * ApplicationController instead of duplicating vocabulary logic in the UI.
 */
public class AutocompleteController {
    private static final int MAX_SUGGESTIONS = 5;

    @FXML
    private Label vocabularyLabel;

    @FXML
    private TextArea sentenceArea;

    @FXML
    private ListView<String> suggestionList;

    @FXML
    private Button useSuggestionButton;

    @FXML
    private Label statusLabel;

    private ApplicationController applicationController;
    private List<WordSuggestion> currentSuggestions = List.of();
    private boolean suppressTextEvents;

    /**
     * Connects this screen to the shared application/business-logic controller.
     *
     * @param applicationController controller that owns vocabulary and suggestion logic
     */
    public void setApplicationController(ApplicationController applicationController) {
        this.applicationController = applicationController;
        refresh();
    }

    /**
     * Sets up event listeners for typed text and suggestion selection.
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
     * Refreshes vocabulary-dependent controls when this tab becomes visible.
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
                    "Type a word, then press space, period, or comma to see next-word choices.");
        } else if (endsWithCompletionDelimiter(sentenceArea.getText())) {
            showSuggestionsForLastCompletedWord();
        }
    }

    /**
     * Applies the currently selected suggestion and records it as a user choice.
     */
    @FXML
    protected void onUseSuggestion() {
        if (applicationController == null) {
            return;
        }

        int selectedIndex = suggestionList.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0 || selectedIndex >= currentSuggestions.size()) {
            statusLabel.setText("Select a suggested word first.");
            return;
        }

        List<String> completedWords = completedWords(sentenceArea.getText());
        if (completedWords.isEmpty()) {
            statusLabel.setText("Complete a word before choosing a suggestion.");
            return;
        }

        String previousWord = completedWords.get(completedWords.size() - 1);
        String chosenWord = currentSuggestions.get(selectedIndex).wordText();

        applicationController.recordSuggestionChoice(previousWord, chosenWord);

        String text = sentenceArea.getText();
        StringBuilder updatedText = new StringBuilder(text);
        if (!text.isEmpty() && !Character.isWhitespace(text.charAt(text.length() - 1))) {
            updatedText.append(' ');
        }
        updatedText.append(chosenWord).append(' ');

        suppressTextEvents = true;
        sentenceArea.setText(updatedText.toString());
        sentenceArea.positionCaret(sentenceArea.getText().length());
        suppressTextEvents = false;

        statusLabel.setText("Added \"" + chosenWord + "\" after \"" + previousWord + "\".");
        showSuggestions(chosenWord);
    }

    /**
     * Watches for completed words without trying to predict while a word is still being typed.
     *
     * @param oldText text before the edit
     * @param newText text after the edit
     */
    private void onTextChanged(String oldText, String newText) {
        if (suppressTextEvents
                || applicationController == null
                || applicationController.getKnownWordCount() == 0) {
            return;
        }

        if (newText.isBlank()) {
            clearSuggestions();
            statusLabel.setText(
                    "Type a word, then press space, period, or comma to see next-word choices.");
            return;
        }

        if (!endsWithCompletionDelimiter(newText)) {
            return;
        }

        List<String> words = completedWords(newText);
        if (words.isEmpty()) {
            clearSuggestions();
            return;
        }

        // When the user types a word instead of clicking a suggestion, completing
        // that word records it as the chosen follower of the previous word.
        boolean newlyCompletedWord = !endsWithCompletionDelimiter(oldText);
        if (newlyCompletedWord && words.size() >= 2) {
            String previousWord = words.get(words.size() - 2);
            String chosenWord = words.get(words.size() - 1);
            applicationController.recordSuggestionChoice(previousWord, chosenWord);
        }

        showSuggestions(words.get(words.size() - 1));
    }

    /**
     * Displays suggestions for the most recently completed word.
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
     * Requests ranked followers from the logic layer and displays them.
     *
     * @param previousWord completed word to find followers for
     */
    private void showSuggestions(String previousWord) {
        currentSuggestions = applicationController.suggestNextWords(
                previousWord,
                MAX_SUGGESTIONS);

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

        if (currentSuggestions.isEmpty()) {
            statusLabel.setText(
                    "No saved followers for \"" + previousWord
                            + "\". Type the next word yourself.");
        } else {
            statusLabel.setText(
                    "Choose a next word for \"" + previousWord
                            + "\", or type your own.");
        }
    }

    /**
     * Converts the text into normalized words using the same basic import rules.
     * Commas and periods are converted to separators because they are completion
     * triggers in the auto-complete requirement.
     *
     * @param text current text in the editor
     * @return normalized words in reading order
     */
    private List<String> completedWords(String text) {
        String separated = text.replace(',', ' ').replace('.', ' ');
        return ImportTokenizer.tokenizeLine(separated);
    }

    /**
     * Returns whether the latest character is one of the required completion triggers.
     *
     * @param text editor text
     * @return true for space, period, or comma
     */
    private boolean endsWithCompletionDelimiter(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        char lastCharacter = text.charAt(text.length() - 1);
        return lastCharacter == ' ' || lastCharacter == '.' || lastCharacter == ',';
    }

    /**
     * Clears suggestions when there is no valid completed word.
     */
    private void clearSuggestions() {
        currentSuggestions = List.of();
        suggestionList.getItems().clear();
        useSuggestionButton.setDisable(true);
    }

    /**
     * Disables the editor until vocabulary data is available.
     *
     * @param disabled true when the feature cannot be used
     */
    private void setControlsDisabled(boolean disabled) {
        sentenceArea.setDisable(disabled);
        suggestionList.setDisable(disabled);
        useSuggestionButton.setDisable(disabled || suggestionList.getSelectionModel().isEmpty());
    }
}
