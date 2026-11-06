package com.example.sentencebuilder.logic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

/**
 * Controller between the UI and business logic (Low Coupling from JavaFX/DB).
 * Written by Subom Sigdel (NetID: sxs220473) for CS4485.0W1 Senior Design,
 * Fall 2026, Team 25, Sentence Builder, starting September 18, 2026.
 * AI-assisted draft; see docs/AI-Usage-Person2-Subom-Sigdel.md.
 *
 * <p>Persistence hooks (repository field, second constructor, and importFile)
 * added by Merit Bhusal (NetID: mxb220125) on
 * October 19, 2026.
 * All earlier code in this class is unchanged.
 */
public class ApplicationController {

    private final VocabularyRepository repository;
    private final Vocabulary vocabulary;
    private final SentenceGenerator sentenceGenerator;
    private final List<Sentence> generatedSentences;

    /**
     * Creates the controller with default business-logic collaborators and no
     * storage, so everything stays in memory (the current behavior).
     */
    public ApplicationController() {
        this(null);
    }

    /**
     * Creates the controller with default business-logic collaborators and a
     * storage contract that the data layer implements.
     *
     * @param repository where learned data is saved; null means memory only
     */
    public ApplicationController(VocabularyRepository repository) {
        this.repository = repository;
        Vocabulary loadedVocabulary = repository == null ? null : repository.loadVocabulary();
        this.vocabulary = loadedVocabulary == null ? new Vocabulary() : loadedVocabulary;
        this.sentenceGenerator = new SentenceGenerator();
        this.generatedSentences = new ArrayList<>();
    }

    /**
     * Learns words and counts from one line of imported text.
     *
     * @param line single line from a text file
     */
    public void learnFromLine(String line) {
        vocabulary.learnFromLine(line);
    }

    /**
     * Returns how many distinct words have been learned.
     *
     * @return vocabulary size
     */
    public int getKnownWordCount() {
        return vocabulary.getWordCount();
    }

    /**
     * Checks whether the shared vocabulary recognizes a typed word before
     * the autocomplete UI requests confirmation. This avoids exposing
     * direct database access to the UI.
     * AI prompt: Expose a read-only known-word check for autocomplete.
     *
     * @param wordText normalized or unnormalized word supplied by the user
     * @return true if the word is currently known
     */
    public boolean isKnownWord(String wordText) {
        return vocabulary.findWord(wordText) != null;
    }

    /**
     * Adds a confirmed unfamiliar word, including the first word in the editor.
     * No artificial follower relationship is created without a previous word.
     * AI prompt: Save a confirmed standalone word without creating a fake
     * previous-word relationship.
     *
     * @param wordText word approved by the user in the auto-complete dialog
     */
    public void addUserWord(String wordText) {
        vocabulary.addUserWord(wordText);
        if (repository != null) {
            repository.saveVocabulary(vocabulary);
        }
    }

    /**
     * Starts sentence generation for the UI layer using weighted random selection.
     *
     * @param startWordText word selected by the user
     * @return generated sentence
     */
    public Sentence generateSentence(String startWordText) {
        Sentence generated = sentenceGenerator.generateFromStartWord(vocabulary, startWordText);
        return rememberGenerated(generated, GenerationMethod.WEIGHTED_RANDOM);
    }

    /**
     * Generates a sentence with a caller-defined maximum length.
     *
     * @param startWordText word selected by the user
     * @param maxWords maximum number of words, including the start word
     * @return generated sentence
     */
    public Sentence generateSentence(String startWordText, int maxWords) {
        return generateSentence(startWordText, maxWords, GenerationMethod.WEIGHTED_RANDOM);
    }

    /**
     * Generates a sentence using an explicit algorithm and length limit.
     *
     * @param startWordText word selected by the user
     * @param maxWords maximum number of words, including the start word
     * @param method generation algorithm to apply
     * @return generated sentence
     */
    public Sentence generateSentence(String startWordText, int maxWords, GenerationMethod method) {
        Sentence generated = sentenceGenerator.generateFromStartWord(
                vocabulary,
                startWordText,
                maxWords,
                method);
        return rememberGenerated(generated, method);
    }

    /**
     * Returns ranked next-word suggestions after a completed previous word.
     *
     * @param previousWordText the finished word before the cursor
     * @param maxSuggestions maximum number of choices to return
     * @return ranked suggestions for the UI layer
     */
    public List<WordSuggestion> suggestNextWords(String previousWordText, int maxSuggestions) {
        return vocabulary.suggestNextWords(previousWordText, maxSuggestions);
    }

    /**
     * Adds a generated sentence back into the learned vocabulary.
     *
     * @param sentence sentence produced by generation or chosen by the user
     */
    public void learnFromSentence(Sentence sentence) {
        vocabulary.learnFromSentence(sentence);
        if (repository != null) {
            repository.saveVocabulary(vocabulary);
        }
    }

    /**
     * Returns generated sentences stored for duplicate checking later.
     *
     * @return unmodifiable history of generated sentences
     */
    public List<Sentence> getGeneratedSentences() {
        return Collections.unmodifiableList(generatedSentences);
    }

    /**
     * Imports one text file into the application's shared vocabulary.
     * When a repository implementation is available, the same operation also
     * saves the updated vocabulary and records the imported-file metadata.
     *
     * @param source text file chosen by the user
     * @param cancelled callback that reports whether the user cancelled
     * @param progress callback receiving completed lines and total lines
     * @return summary of the completed import
     * @throws IOException if the file cannot be read
     */
    public ImportSummary importFile(
            Path source,
            BooleanSupplier cancelled,
            BiConsumer<Long, Long> progress) throws IOException {

        Objects.requireNonNull(source, "source");
        String validationMessage = ImportFileValidator.validate(source);
        if (!validationMessage.isEmpty()) {
            throw new IllegalArgumentException(validationMessage);
        }

        TextFileAnalyzer analyzer = new TextFileAnalyzer();
        Vocabulary stagedVocabulary = new Vocabulary();
        ImportSummary summary = analyzer.analyze(
                source,
                stagedVocabulary,
                cancelled,
                progress);

        // Contribution (October 23, 2026): I, Jinesh Patel, added staged importing.
        // Vocabulary changes remain isolated until the full import succeeds,
        // preventing cancelled imports from leaking partial data to other tabs.
        // AI prompt: Stage imported words and merge only after success so
        // cancelling a background import does not change shared vocabulary.
        vocabulary.mergeFrom(stagedVocabulary);

        if (repository != null) {
            repository.saveVocabulary(vocabulary);
            repository.recordImportedFile(summary);
        }

        return summary;
    }

    /**
     * Records that the user picked or typed a word after a completed word in
     * autocomplete. Unknown words are added; known pairs have their user-choice
     * count increased.
     *
     * @param previousWordText completed word before the cursor
     * @param chosenWordText word the user chose or typed next
     */
    public void recordSuggestionChoice(String previousWordText, String chosenWordText) {
        vocabulary.recordUserChoice(previousWordText, chosenWordText);
        if (repository != null) {
            repository.recordUserChoice(previousWordText, chosenWordText);
            repository.saveVocabulary(vocabulary);
        }
    }

    /**
     * Stores a generated sentence so reports can look for duplicates.
     *
     * @param sentence newly generated sentence
     * @return the same sentence for the UI layer
     */
    private Sentence rememberGenerated(Sentence sentence, GenerationMethod method) {
        Objects.requireNonNull(sentence, "sentence");
        Objects.requireNonNull(method, "method");
        generatedSentences.add(sentence);
        if (repository != null) {
            repository.saveGeneratedSentence(sentence, method);
        }
        return sentence;
    }
}
