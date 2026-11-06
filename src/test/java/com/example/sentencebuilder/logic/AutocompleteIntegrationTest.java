package com.example.sentencebuilder.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests autocomplete's shared controller contract without requiring a JavaFX
 * display. A declined confirmation is represented by performing no write.
 *
 * <p>Sentence Builder, CS4485.0W1 Senior Design, Team 25.
 * Tests by Jinesh Patel, started October 30, 2026.
 * NetID: JAP220022.
 * Confirmation regression added November 6, 2026.
 */
class AutocompleteIntegrationTest {

    @TempDir
    Path tempDirectory;

    /**
     * Verifies that the confirmation flow can check whether a user-entered word
     * is already recognized and can add a first word without a fake follower.
     */
    @Test
    void unknownStandaloneWordIsAddedOnlyAfterExplicitControllerCall() {
        ApplicationController controller = new ApplicationController();

        assertEquals(false, controller.isKnownWord("unfamiliar"));
        assertEquals(0, controller.getKnownWordCount());

        // A declined JavaFX confirmation intentionally makes no controller
        // write, so the application's word count remains unchanged.
        assertEquals(0, controller.getKnownWordCount());

        controller.addUserWord("unfamiliar");
        assertEquals(true, controller.isKnownWord("unfamiliar"));
        assertEquals(1, controller.getKnownWordCount());

        controller.addUserWord("unfamiliar");
        assertEquals(1, controller.getKnownWordCount());
        assertEquals(0, controller.suggestNextWords("unfamiliar", 5).size());
    }

    /**
     * Verifies ranked suggestions and recording of a manually entered new word.
     */
    @Test
    void suggestionsAndManualChoiceUseSharedVocabulary() throws IOException {
        Path source = tempDirectory.resolve("autocomplete.txt");
        Files.writeString(source, "the cat\nthe dog\nthe cat");

        ApplicationController controller = new ApplicationController();
        controller.importFile(source, () -> false, (completed, total) -> { });

        List<WordSuggestion> suggestions = controller.suggestNextWords("the", 5);
        assertEquals("cat", suggestions.get(0).wordText());
        assertEquals(2, suggestions.get(0).followerCount());
        assertEquals("dog", suggestions.get(1).wordText());

        // The selected/accepted follower is recorded through existing logic,
        // rather than through JavaFX widgets or direct database access.
        controller.recordSuggestionChoice("the", "bird");

        assertEquals(4, controller.getKnownWordCount());
        List<WordSuggestion> updated = controller.suggestNextWords("the", 5);
        assertEquals("cat", updated.get(0).wordText());
        assertEquals("bird", updated.get(1).wordText());
        assertEquals(1, updated.get(1).followerCount());
    }
}
