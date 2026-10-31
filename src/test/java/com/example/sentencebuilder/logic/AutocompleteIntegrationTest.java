package com.example.sentencebuilder.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for the controller methods used by the auto-complete UI.
 */
class AutocompleteIntegrationTest {

    @TempDir
    Path tempDirectory;

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

        controller.recordSuggestionChoice("the", "bird");

        assertEquals(4, controller.getKnownWordCount());
        List<WordSuggestion> updated = controller.suggestNextWords("the", 5);
        assertEquals("cat", updated.get(0).wordText());
        assertEquals("bird", updated.get(1).wordText());
        assertEquals(1, updated.get(1).followerCount());
    }
}
