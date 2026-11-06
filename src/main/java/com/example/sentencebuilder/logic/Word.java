package com.example.sentencebuilder.logic;

import java.util.Locale;
import java.util.Objects;

/**
 * Information Expert for one vocabulary word.
 * Written by Subom Sigdel (NetID: sxs220473) for CS4485.0W1 Senior Design,
 * Fall 2026, Team 25, Sentence Builder, starting September 18, 2026.
 * AI-assisted draft; see docs/AI-Usage-Person2-Subom-Sigdel.md.
 */
public class Word {

    private final String text;
    private long occurrenceCount;
    private long sentenceStartCount;
    private long sentenceEndCount;

    /**
     * Creates a word from imported or user-entered text.
     *
     * @param rawText token text before normalization
     */
    public Word(String rawText) {
        this.text = normalize(rawText);
        if (this.text.isEmpty()) {
            throw new IllegalArgumentException("Word text cannot be blank.");
        }
    }

    /**
     * Returns the normalized word text.
     *
     * @return lowercase trimmed text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns how many times this word has been seen.
     *
     * @return occurrence count
     */
    public long getOccurrenceCount() {
        return occurrenceCount;
    }

    /**
     * Records one more occurrence of this word.
     */
    public void recordOccurrence() {
        occurrenceCount++;
    }

    /**
     * Records that this word appeared at the start of a line or sentence span.
     */
    public void recordSentenceStart() {
        sentenceStartCount++;
    }

    /**
     * Records that this word appeared at the end of a line or sentence span.
     */
    public void recordSentenceEnd() {
        sentenceEndCount++;
    }

    /**
     * Adds previously staged word counts after a completed import.
     * Contribution (October 23, 2026): I, Jinesh Patel, added this merge
     * support to prevent cancelled imports from changing shared totals.
     * AI prompt: Add completed import counts to an existing word with
     * nonnegative validation.
     *
     * @param occurrences occurrence count to add
     * @param sentenceStarts sentence-start count to add
     * @param sentenceEnds sentence-end count to add
     */
    void addCounts(long occurrences, long sentenceStarts, long sentenceEnds) {
        if (occurrences < 0 || sentenceStarts < 0 || sentenceEnds < 0) {
            throw new IllegalArgumentException("Word counts cannot be negative.");
        }
        occurrenceCount += occurrences;
        sentenceStartCount += sentenceStarts;
        sentenceEndCount += sentenceEnds;
    }

    /**
     * Returns how many times this word started a learned line.
     *
     * @return sentence-start count
     */
    public long getSentenceStartCount() {
        return sentenceStartCount;
    }

    /**
     * Returns how many times this word ended a learned line.
     *
     * @return sentence-end count
     */
    public long getSentenceEndCount() {
        return sentenceEndCount;
    }

    /**
     * Normalizes text so words can be matched consistently later.
     *
     * @param rawText unprocessed token text
     * @return normalized text
     */
    public static String normalize(String rawText) {
        Objects.requireNonNull(rawText, "rawText");
        return rawText.trim().toLowerCase(Locale.ROOT);
    }
}
