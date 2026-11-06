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
 * I test import-to-vocabulary integration with temporary text files and an
 * in-memory controller so these tests do not require a live database.
 *
 * <p>I, Jinesh Patel (NetID: JAP220022), started these tests on October 30,
 * 2026 for CS4485.0W1 Senior Design, Team 25.
 * AI prompt: Test that successful imports feed sentence generation and that
 * cancelling a partially processed import leaves shared data unchanged.
 */
class ApplicationControllerImportIntegrationTest {

    @TempDir
    Path tempDirectory;

    /**
     * I verify that a completed import is usable by existing generation logic.
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

        // I check file-local summary counts and confirm that learned words
        // are available to generation through the shared controller.
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
     * I verify that cancelling after staging lines does not partially
     * modify the shared vocabulary.
     */
    @Test
    void cancelledImportDoesNotLeavePartialVocabulary() throws IOException {
        Path source = tempDirectory.resolve("cancelled.txt");
        Files.writeString(source, "one two\nthree four\nfive six\nseven eight");

        ApplicationController controller = new ApplicationController();
        // I delay cancellation into the second pass to verify that partially
        // staged words never reach shared vocabulary.
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
