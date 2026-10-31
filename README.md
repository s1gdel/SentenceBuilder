# Sentence Builder

CS4485 Senior Design — Fall 2026 (Prof. John Cole)

A JavaFX application that reads plain-text files, learns word and word-following statistics, stores project data in MySQL/MariaDB, and will generate new sentences using word-probability algorithms and auto-complete.

## Tech stack

- **Language:** Java
- **UI:** JavaFX
- **Database:** MySQL / MariaDB
- **Build tool:** Maven
- **IDE:** IntelliJ IDEA (Community Edition) recommended

## Architecture

The project follows a three-layer architecture:

```
User Interface
      ↓
Business Logic
      ↓
Technical Services
      ↓
MySQL / MariaDB
```

![Three-layer architecture](docs/architecture-diagram.svg)

The main source packages are:

```
src/main/java/com/example/sentencebuilder/
├── ui/       → JavaFX controllers, background UI tasks, user-interface behavior
├── logic/    → Word/sentence models, vocabulary, tokenization,
│               sentence generation, import analysis, application controller
└── data/     → Database access layer (currently a repository placeholder)

src/main/resources/
├── com/example/sentencebuilder/ → FXML resources
└── database/                    → SQL schema resources
```

The UI communicates with the logic layer instead of directly accessing the database. Database and other technical-service operations remain separated from JavaFX code.

## Current progress

The project is still in development, and the import, generation, and early auto-complete screens now operate through the same shared business-logic controller.

### Completed groundwork

- JavaFX/Maven project structure and three-layer package organization.
- Architecture flow diagram in `docs/architecture-diagram.svg`.
- Initial MySQL/MariaDB schema groundwork for words, follower relationships, imported files, and generated sentences.
- Plain-text file validation through `ImportFileValidator`.
- Background file processing with progress updates and cancellation.
- File processing uses the shared `ImportTokenizer` and `Vocabulary` rules and reports recognized/distinct word counts.
- The JavaFX background import task now goes through `ApplicationController.importFile(...)` instead of creating its own analyzer.
- Imported text is learned into the application's shared in-memory vocabulary, making it available to generation and suggestion logic during the current session.
- `VocabularyRepository` defines the contract the future JDBC data layer can use for vocabulary, import, sentence-history, and autocomplete persistence.
- Sentence generation supports weighted-random and most-common follower selection with configurable maximum length.
- Business logic can rank next-word suggestions from learned follower counts.
- Generated sentences are tracked in memory for later duplicate/history integration.
- Import and generation are presented as coordinated JavaFX tabs that share application state.
- Generation controls stay unavailable until vocabulary data exists, and a successful import refreshes and opens the Generate tab.
- Initial auto-complete UI:
  - becomes available after text has been imported
  - requests next-word suggestions only after space, period, or comma
  - shows ranked follower choices from the shared vocabulary
  - lets the user insert a suggestion or type a different next word
  - records selected/typed follower choices through `ApplicationController.recordSuggestionChoice(...)`
- Basic JavaFX file-analysis screen includes:
  - text-file selection
  - file validation
  - background processing
  - progress/status updates
  - cancellation
  - recognized-word and distinct-word results
  - simple success/error feedback

### Still to be implemented or integrated

- JDBC/MySQL implementation of `VocabularyRepository` / database CRUD wiring.
- Loading persisted vocabulary at application startup.
- Persisting imported-file metadata and learned word/follower statistics.
- JavaFX screens/event handling for reports and word management.
- Complete auto-complete UX/polish and persistence through the future JDBC repository.
- Persisting generated-sentence history and duplicate-reporting data.
- Additional automated testing around database persistence and the remaining application flows.
- Final UI/UX polish.

## Getting started

1. Clone the repository:
   ```bash
   git clone https://github.com/s1gdel/SentenceBuilder.git
   cd SentenceBuilder
   ```

2. Open the project in IntelliJ IDEA.

3. Let Maven download the dependencies from `pom.xml`.

4. Use JDK 17 or newer.

5. Run `HelloApplication.java`.

The current interface can select and process a plain-text file into the application's in-memory vocabulary while keeping the work off the JavaFX thread. MySQL persistence is not active yet because the JDBC repository implementation is still pending.

## Contributing

- Work on a feature branch instead of directly on `main` when possible.
- Pull the latest changes before starting new work.
- Open a pull request for review before merging.
- Keep UI, business logic, and technical-service/database responsibilities separated.
- Keep each class and method focused on a clear responsibility.
- Follow the project's documentation and coding standards when adding or modifying code.

## Status

In progress: file processing flows through the shared application controller, Import/Generate are integrated, and an initial event-driven auto-complete screen now uses the existing suggestion and user-choice logic. Integration tests cover imports, cancellation safety, suggestion ranking, and manual next-word choices. JDBC persistence and the remaining JavaFX features are still under development.
