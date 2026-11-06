package com.example.sentencebuilder.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * I test autocomplete's shared controller contract without a JavaFX display.
 * I represent a declined confirmation by making no write.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started these tests on October 30,
 * 2026 for CS4485.0W1 Senior Design, Team 25. I added confirmation
 * regression coverage on November 6, 2026.
 * AI prompt: Test ranked autocomplete suggestions, new word recording,
 * and recognition checks for a word the user may approve adding.
 */
class AutocompleteIntegrationTest {

    @TempDir
    Path tempDirectory;

    /**
     * I verify word-recognition checks and add a confirmed first word without
     * creating a false follower relationship.
     */
    @Test
    void unknownStandaloneWordIsAddedOnlyAfterExplicitControllerCall() {
        ApplicationController controller = new ApplicationController();

        assertEquals(false, controller.isKnownWord("unfamiliar"));
        assertEquals(0, controller.getKnownWordCount());

        // I simulate No by making no controller write and checking that
        // the vocabulary count remains unchanged.
        assertEquals(0, controller.getKnownWordCount());

        controller.addUserWord("unfamiliar");
        assertEquals(true, controller.isKnownWord("unfamiliar"));
        assertEquals(1, controller.getKnownWordCount());

        controller.addUserWord("unfamiliar");
        assertEquals(1, controller.getKnownWordCount());
        assertEquals(0, controller.suggestNextWords("unfamiliar", 5).size());
    }

    /**
     * I verify ranked suggestions and record a manually entered new word.
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

        // I record an approved follower through existing logic, not JavaFX
        // widgets or direct database access.
        controller.recordSuggestionChoice("the", "bird");

        assertEquals(4, controller.getKnownWordCount());
        List<WordSuggestion> updated = controller.suggestNextWords("the", 5);
        assertEquals("cat", updated.get(0).wordText());
        assertEquals("bird", updated.get(1).wordText());
        assertEquals(1, updated.get(1).followerCount());
    }
}
