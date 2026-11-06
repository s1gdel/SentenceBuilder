-- CS4485 Senior Design - Sentence Builder
-- Database schema foundation.
-- I, Jinesh Patel (NetID: JAP220022), contributed the initial schema
-- structure starting September 25, 2026 for CS4485.0W1 Senior Design,
-- Team 25. Later JDBC implementation and schema maintenance belong
-- to the team's database contributors.
-- AI prompt: Design a MySQL schema for words, follower relationships,
-- imported files, and generated sentences with primary and foreign keys.
--
-- Surrogate integer keys identify referenced records. Variable-length text
-- fields store words, paths, and sentences; numeric count fields support
-- arithmetic and aggregation.

CREATE DATABASE IF NOT EXISTS sentence_builder
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE sentence_builder;

CREATE TABLE IF NOT EXISTS words (
    word_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    word_text VARCHAR(100) NOT NULL,
    occurrence_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    sentence_start_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    sentence_end_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    user_choice_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (word_id),
    UNIQUE KEY uq_words_text (word_text)
);

CREATE TABLE IF NOT EXISTS word_followers (
    word_id INT UNSIGNED NOT NULL,
    following_word_id INT UNSIGNED NOT NULL,
    occurrence_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    user_choice_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (word_id, following_word_id),
    CONSTRAINT fk_word_followers_word
        FOREIGN KEY (word_id) REFERENCES words (word_id),
    CONSTRAINT fk_word_followers_following
        FOREIGN KEY (following_word_id) REFERENCES words (word_id)
);

CREATE TABLE IF NOT EXISTS imported_files (
    import_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    word_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    file_size_bytes BIGINT UNSIGNED NOT NULL DEFAULT 0,
    imported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (import_id),
    KEY idx_imported_files_name (file_name)
);

CREATE TABLE IF NOT EXISTS generated_sentences (
    sentence_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sentence_text TEXT NOT NULL,
    starting_word_id INT UNSIGNED NULL,
    generation_method VARCHAR(50) NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sentence_id),
    KEY idx_generated_sentences_starting_word (starting_word_id),
    CONSTRAINT fk_generated_sentences_starting_word
        FOREIGN KEY (starting_word_id) REFERENCES words (word_id)
);
