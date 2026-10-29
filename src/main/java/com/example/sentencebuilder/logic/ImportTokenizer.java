package com.example.sentencebuilder.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Tokenization policy for imported plain-text lines (logic layer only).
 * Written by Subom Sigdel (NetID: sxs220473) for CS4485.0W1 Senior Design,
 * Fall 2026, Team 25, Sentence Builder, starting September 26, 2026.
 * AI-assisted draft; see docs/AI-Usage-Person2-Subom-Sigdel.md.
 */
public final class ImportTokenizer {

    private static final String TRAILING_PUNCTUATION = ".,;:!?\"'()";

    /** Closing quotes and brackets that may follow a sentence-ending mark, as in: said."  */
    private static final String CLOSERS = "\"')\u201d\u2019";

    /** Abbreviations whose trailing period does not end a sentence. */
    private static final Set<String> ABBREVIATIONS =
            Set.of("mr", "mrs", "ms", "dr", "st", "jr", "sr", "vs");

    /**
     * One word from a line, plus whether it ended a sentence.
     *
     * @param text normalized word text
     * @param endsSentence true when the word was followed by a period, question mark,
     *                     or exclamation mark (not counting abbreviations like "Mr.")
     */
    public record SentenceToken(String text, boolean endsSentence) {
    }

    private ImportTokenizer() {
    }

    /**
     * Same word splitting as {@link #tokenizeLine(String)}, but also reports which
     * words end a sentence, so the caller can keep sentences from running together.
     *
     * @param line one line from an imported text file
     * @return words in order with sentence-end flags; empty when the line has no words
     */
    public static List<SentenceToken> tokenizeWithBoundaries(String line) {
        Objects.requireNonNull(line, "line");
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) {
            return List.of();
        }

        List<SentenceToken> tokens = new ArrayList<>();
        for (String rawToken : trimmedLine.split("\\s+")) {
            String normalized = Word.normalize(stripEdgePunctuation(rawToken));
            if (!normalized.isEmpty()) {
                tokens.add(new SentenceToken(normalized, endsSentence(rawToken, normalized)));
            }
        }
        return tokens;
    }

    /**
     * Decides whether a raw token finishes a sentence.
     *
     * @param rawToken token exactly as it appeared in the file
     * @param normalized the same token after cleanup
     * @return true for a trailing . ! or ? (ignoring closing quotes), except abbreviations
     */
    private static boolean endsSentence(String rawToken, String normalized) {
        int end = rawToken.length();
        while (end > 0 && CLOSERS.indexOf(rawToken.charAt(end - 1)) >= 0) {
            end--;
        }
        if (end == 0) {
            return false;
        }
        char last = rawToken.charAt(end - 1);
        if (last == '?' || last == '!') {
            return true;
        }
        return last == '.' && !ABBREVIATIONS.contains(normalized);
    }

    /**
     * Splits a line into word tokens using the project's first-pass import rules.
     * Whitespace separates tokens; leading and trailing punctuation on each token is removed.
     *
     * @param line one line from an imported text file
     * @return normalized tokens in order; empty when the line is blank
     */
    public static List<String> tokenizeLine(String line) {
        Objects.requireNonNull(line, "line");
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty()) {
            return List.of();
        }

        String[] rawTokens = trimmedLine.split("\\s+");
        List<String> tokens = new ArrayList<>(rawTokens.length);
        for (String rawToken : rawTokens) {
            String cleaned = stripEdgePunctuation(rawToken);
            String normalized = Word.normalize(cleaned);
            if (!normalized.isEmpty()) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }

    /**
     * Removes punctuation attached to the start or end of a raw token.
     *
     * @param rawToken token before normalization
     * @return token with edge punctuation removed
     */
    private static String stripEdgePunctuation(String rawToken) {
        int startIndex = 0;
        int endIndex = rawToken.length();
        while (startIndex < endIndex && TRAILING_PUNCTUATION.indexOf(rawToken.charAt(startIndex)) >= 0) {
            startIndex++;
        }
        while (endIndex > startIndex && TRAILING_PUNCTUATION.indexOf(rawToken.charAt(endIndex - 1)) >= 0) {
            endIndex--;
        }
        return rawToken.substring(startIndex, endIndex);
    }
}