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
└── database/schema.sql          → Initial MySQL/MariaDB schema
```

The UI communicates with the logic layer instead of directly accessing the database. Database and other technical-service operations remain separated from JavaFX code.

## Current progress

The project is still in development, but the initial scaffold now includes a first usable file-analysis interface.

### Completed groundwork

- JavaFX/Maven project structure and three-layer package organization.
- Architecture flow diagram in `docs/architecture-diagram.svg`.
- Initial MySQL/MariaDB schema with tables for:
  - words
  - word-following relationships
  - imported files
  - generated sentences
- Plain-text file validation through `ImportFileValidator`.
- File-analysis logic through `TextFileAnalyzer` and `ImportSummary`, including line/token counts, progress callbacks, and cancellation support.
- First-pass tokenization through `ImportTokenizer`.
- In-memory business-logic models:
  - `Word`
  - `Sentence`
  - `Vocabulary`
- `ApplicationController` to keep the UI decoupled from business logic.
- Early `SentenceGenerator` implementation that can start with a known word and append its most common learned follower.
- Basic JavaFX file-analysis screen with:
  - text-file selection
  - file validation
  - background processing
  - progress/status updates
  - cancellation
  - simple success/error feedback

### Still to be implemented or integrated

- JDBC/MySQL repository implementation and database CRUD wiring.
- Connecting analyzed/imported text to the tokenizer, vocabulary, and persistent database storage.
- Additional JavaFX screens and event handling for generation, auto-complete, reports, and word management.
- Complete sentence-generation algorithms, including weighted/probability-based selection.
- Auto-complete behavior.
- Word-data viewing/editing and reporting features.
- Generated-sentence history integration.
- Broader automated testing and final UI/UX polish.

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

The current interface can select and analyze a plain-text file, but it does not yet write the analyzed data to MySQL.

## Contributing

- Work on a feature branch instead of directly on `main` when possible.
- Pull the latest changes before starting new work.
- Open a pull request for review before merging.
- Keep UI, business logic, and technical-service/database responsibilities separated.
- Keep each class and method focused on a clear responsibility.
- Follow the project's documentation and coding standards when adding or modifying code.

## Status

In progress: file validation/analysis, initial business logic, database schema, and a first responsive JavaFX file-analysis screen are present; database persistence and the remaining application features are still under development.
