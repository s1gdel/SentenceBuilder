package com.example.sentencebuilder.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Information Expert for the in-memory word list built from imported text.
 * Written by Subom Sigdel (NetID: sxs220473) for CS4485.0W1 Senior Design,
 * Fall 2026, Team 25, Sentence Builder, starting September 24, 2026.
 * AI-assisted draft; see docs/AI-Usage-Person2-Subom-Sigdel.md.
 */
public class Vocabulary {

    private final Map<String, Word> wordsByText = new HashMap<>();
    private final Map<String, Map<String, Integer>> followerCounts = new HashMap<>();

    /** Last word of the sentence still in progress, or null at a sentence boundary. */
    private Word previousWord;

    /**
     * Updates word and follower statistics from one line of imported text.
     *
     * <p>A sentence ends at a period, question mark, or exclamation mark, not at the
     * end of a line. A sentence that wraps onto the next line keeps going, and one line
     * may hold several sentences. A blank line also ends the current sentence.
     *
     * @param line one line from a source file; blank lines end the current sentence
     */
    public void learnFromLine(String line) {
        Objects.requireNonNull(line, "line");
        List<ImportTokenizer.SentenceToken> tokens = ImportTokenizer.tokenizeWithBoundaries(line);
        if (tokens.isEmpty()) {
            previousWord = null;
            return;
        }

        for (ImportTokenizer.SentenceToken token : tokens) {
            Word currentWord = getOrCreateWord(token.text());
            currentWord.recordOccurrence();
            if (previousWord == null) {
                currentWord.recordSentenceStart();
            } else {
                recordFollower(previousWord.getText(), currentWord.getText());
            }
            if (token.endsSentence()) {
                currentWord.recordSentenceEnd();
                previousWord = null;
            } else {
                previousWord = currentWord;
            }
        }
    }

    /**
     * Merges successfully staged imports into the shared vocabulary.
     * Contribution (October 23, 2026): I, Jinesh Patel, added this merge
     * integration to prevent cancelled imports from leaving partial counts.
     * AI prompt: Merge staged vocabulary word and follower counts after a
     * successful file import.
     *
     * @param importedVocabulary completed vocabulary built from one imported file
     */
    void mergeFrom(Vocabulary importedVocabulary) {
        Objects.requireNonNull(importedVocabulary, "importedVocabulary");

        // Accumulate new import counts instead of replacing words learned
        // from earlier imports in the same session.
        for (Word importedWord : importedVocabulary.wordsByText.values()) {
            Word targetWord = getOrCreateWord(importedWord.getText());
            targetWord.addCounts(
                    importedWord.getOccurrenceCount(),
                    importedWord.getSentenceStartCount(),
                    importedWord.getSentenceEndCount());
        }

        // Merge followers only after a completed import so cancellation
        // cannot leave partial word relationships.
        for (Map.Entry<String, Map<String, Integer>> wordEntry
                : importedVocabulary.followerCounts.entrySet()) {
            Map<String, Integer> targetFollowers = followerCounts.computeIfAbsent(
                    wordEntry.getKey(),
                    ignored -> new LinkedHashMap<>());

            for (Map.Entry<String, Integer> followerEntry : wordEntry.getValue().entrySet()) {
                targetFollowers.merge(
                        followerEntry.getKey(),
                        followerEntry.getValue(),
                        Integer::sum);
            }
        }
    }

    /**
     * Returns how many distinct words are stored.
     *
     * @return vocabulary size
     */
    public int getWordCount() {
        return wordsByText.size();
    }

    /**
     * Finds a word already learned from imported text.
     *
     * @param rawText token text to look up
     * @return matching word, or null when the word is not in the vocabulary
     */
    public Word findWord(String rawText) {
        String key = Word.normalize(rawText);
        if (key.isEmpty()) {
            return null;
        }
        return wordsByText.get(key);
    }

    /**
     * Returns follower words and how often each follows the given word.
     *
     * @param wordText word to look up
     * @return map of follower text to count; empty when unknown or no followers
     */
    public Map<String, Integer> getFollowers(String wordText) {
        String key = Word.normalize(wordText);
        Map<String, Integer> followers = followerCounts.get(key);
        if (followers == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(followers);
    }

    /**
     * Learns word and follower statistics from a generated or imported sentence.
     *
     * @param sentence sentence whose word order should update the vocabulary
     */
    public void learnFromSentence(Sentence sentence) {
        Objects.requireNonNull(sentence, "sentence");
        // Generated text has no punctuation, so end it explicitly and keep it separate
        // from whatever sentence was in progress.
        previousWord = null;
        learnFromLine(sentence.asText() + ".");
    }

    /**
     * Records a word chosen after another word in autocomplete.
     * The chosen word is added when it has not been learned from an import yet.
     *
     * @param previousWordText completed word before the cursor
     * @param chosenWordText word selected or entered by the user
     */
    public void recordUserChoice(String previousWordText, String chosenWordText) {
        String previous = normalizeRequired(previousWordText, "previousWordText");
        String chosen = normalizeRequired(chosenWordText, "chosenWordText");
        getOrCreateWord(previous);
        Word chosenWord = getOrCreateWord(chosen);
        chosenWord.recordOccurrence();
        recordFollower(previous, chosen);
    }

    /**
     * Adds a user-confirmed unfamiliar word without creating a follower pair.
     * Previously known words remain unchanged.
     * AI prompt: Add a confirmed new word without a follower when the editor
     * has no previous word.
     *
     * @param wordText confirmed word typed by the user
     */
    public void addUserWord(String wordText) {
        String normalized = normalizeRequired(wordText, "wordText");
        if (!wordsByText.containsKey(normalized)) {
            getOrCreateWord(normalized).recordOccurrence();
        }
    }

    /**
     * Restores one stored word with its saved counts. Used by the data layer when
     * loading from storage; counts are added to whatever the word already has.
     *
     * @param text word text
     * @param occurrenceCount saved occurrence count
     * @param sentenceStartCount saved sentence-start count
     * @param sentenceEndCount saved sentence-end count
     */
    public void restoreWord(
            String text,
            long occurrenceCount,
            long sentenceStartCount,
            long sentenceEndCount) {
        getOrCreateWord(normalizeRequired(text, "text"))
                .addCounts(occurrenceCount, sentenceStartCount, sentenceEndCount);
    }

    /**
     * Restores one stored follower count. Used by the data layer when loading from
     * storage; both words are created if they are not present yet.
     *
     * @param fromText word that comes first
     * @param toText word that follows it
     * @param count saved number of times the second word followed the first; at least 1
     */
    public void restoreFollower(String fromText, String toText, int count) {
        if (count < 1) {
            throw new IllegalArgumentException("A follower count must be at least 1.");
        }
        String from = normalizeRequired(fromText, "fromText");
        String to = normalizeRequired(toText, "toText");
        getOrCreateWord(from);
        getOrCreateWord(to);
        followerCounts
                .computeIfAbsent(from, ignored -> new LinkedHashMap<>())
                .merge(to, count, Integer::sum);
    }

    /**
     * Returns next-word suggestions ranked by follower count, then alphabetically.
     * Suggestions are offered only after a complete previous word, matching
     * the assignment rule that autocomplete starts after space, period, or comma.
     *
     * @param previousWordText the completed word before the cursor
     * @param maxSuggestions maximum number of suggestions to return
     * @return ranked suggestions; empty when the previous word has no followers
     */
    public List<WordSuggestion> suggestNextWords(String previousWordText, int maxSuggestions) {
        if (maxSuggestions < 1) {
            throw new IllegalArgumentException("maxSuggestions must be at least 1.");
        }
        Map<String, Integer> followers = getFollowers(previousWordText);
        if (followers.isEmpty()) {
            return List.of();
        }

        List<WordSuggestion> suggestions = new ArrayList<>(followers.size());
        for (Map.Entry<String, Integer> entry : followers.entrySet()) {
            suggestions.add(new WordSuggestion(entry.getKey(), entry.getValue()));
        }
        suggestions.sort(Comparator
                .comparingInt(WordSuggestion::followerCount)
                .reversed()
                .thenComparing(WordSuggestion::wordText));

        int resultSize = Math.min(maxSuggestions, suggestions.size());
        return List.copyOf(suggestions.subList(0, resultSize));
    }

    /**
     * Returns the most common follower for a word, for simple generation tests.
     *
     * @param wordText word to look up
     * @return follower text with the highest count, or null when none exist
     */
    public String getMostCommonFollower(String wordText) {
        Map<String, Integer> followers = getFollowers(wordText);
        if (followers.isEmpty()) {
            return null;
        }
        String bestFollower = null;
        int bestCount = -1;
        for (Map.Entry<String, Integer> entry : followers.entrySet()) {
            if (entry.getValue() > bestCount
                    || (entry.getValue() == bestCount
                    && (bestFollower == null || entry.getKey().compareTo(bestFollower) < 0))) {
                bestCount = entry.getValue();
                bestFollower = entry.getKey();
            }
        }
        return bestFollower;
    }

    /**
     * Returns a read-only view of all words keyed by normalized text.
     *
     * @return unmodifiable word map
     */
    public Map<String, Word> snapshotWords() {
        return Collections.unmodifiableMap(wordsByText);
    }

    private Word getOrCreateWord(String normalizedToken) {
        Word existing = wordsByText.get(normalizedToken);
        if (existing != null) {
            return existing;
        }
        Word created = new Word(normalizedToken);
        wordsByText.put(normalizedToken, created);
        return created;
    }

    private void recordFollower(String fromWordText, String toWordText) {
        Map<String, Integer> followers = followerCounts.computeIfAbsent(
                fromWordText,
                ignored -> new LinkedHashMap<>());
        int updatedCount = followers.getOrDefault(toWordText, 0) + 1;
        followers.put(toWordText, updatedCount);
    }

    /**
     * Normalizes a required user-entered word and reports a useful argument error.
     *
     * @param rawText word supplied by a caller
     * @param argumentName argument name used in the error message
     * @return normalized nonblank word text
     */
    private String normalizeRequired(String rawText, String argumentName) {
        Objects.requireNonNull(rawText, argumentName);
        String normalized = Word.normalize(rawText);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(argumentName + " cannot be blank.");
        }
        return normalized;
    }
}