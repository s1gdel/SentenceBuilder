package com.example.sentencebuilder.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests import-to-vocabulary integration using temporary text files and an
 * in-memory application controller, without requiring a live database.
 *
 * <p>Written by Jinesh Patel for CS4485.0W1 Senior Design, Team 25,
 * starting October 30, 2026. NetID: JAP220022.
 */
class ApplicationControllerImportIntegrationTest {

    @TempDir
    Path tempDirectory;

    /**
     * Verifies that a completed import becomes usable by the existing generation logic.
     */
    @Test
    void completedImportFeedsSharedVocabularyAndGeneration() throws IOException {
        Path source = tempDirectory.resolve("sample.txt");
        Files.writeString(source, "hello world\nhello world\nhello there");

        ApplicationController controller = new ApplicationController();
        ImportSummary summary = controller.importFile(
                source,
                () -> false,
                (completed, total) -> { });

        // The summary belongs to this imported file, but the result must also
        // be reusable by sentence generation through shared application state.
        assertEquals(3, summary.lineCount());
        assertEquals(6, summary.tokenCount());
        assertEquals(3, summary.distinctWordCount());
        assertEquals(3, controller.getKnownWordCount());
        assertEquals(
                "hello world",
                controller.generateSentence(
                        "hello",
                        2,
                        GenerationMethod.MOST_COMMON).asText());
    }

    /**
     * Verifies that cancelling after some lines were staged does not partially
     * modify the shared application vocabulary.
     */
    @Test
    void cancelledImportDoesNotLeavePartialVocabulary() throws IOException {
        Path source = tempDirectory.resolve("cancelled.txt");
        Files.writeString(source, "one two\nthree four\nfive six\nseven eight");

        ApplicationController controller = new ApplicationController();
        // Delay cancellation into the second pass to test that partially
        // staged words never reach the shared application vocabulary.
        AtomicInteger cancellationChecks = new AtomicInteger();

        assertThrows(
                CancellationException.class,
                () -> controller.importFile(
                        source,
                        () -> cancellationChecks.incrementAndGet() > 5,
                        (completed, total) -> { }));

        assertEquals(0, controller.getKnownWordCount());
    }
}
