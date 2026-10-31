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
 * Integration tests for the file-import flow that feeds the shared application vocabulary.
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
