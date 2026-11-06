package com.example.sentencebuilder;

import com.example.sentencebuilder.data.WordRepository;
import com.example.sentencebuilder.logic.ApplicationController;
import com.example.sentencebuilder.ui.AutocompleteController;
import com.example.sentencebuilder.ui.GenerateController;
import com.example.sentencebuilder.ui.ImportController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Starts the JavaFX application and displays the Sentence Builder screens.
 *
 * <p>This class is the one place where the layers are assembled: it builds the
 * ApplicationController and hands it to the screen, so the UI never creates
 * business-logic or data objects itself.
 */
public class HelloApplication extends Application {
    private static final double WINDOW_WIDTH = 720;
    private static final double WINDOW_HEIGHT = 560;

    /** False when the database could not be reached and data is kept in memory only. */
    private boolean databaseConnected;

    /**
     * Builds the application controller with database storage when the database is
     * reachable. If it is not, the app still runs, but learned words stay in memory
     * only and are lost when the window closes.
     *
     * @return controller connected to storage, or a memory-only controller
     */
    private ApplicationController createApplicationController() {
        try {
            ApplicationController controller =
                    new ApplicationController(WordRepository.fromEnvironment());
            databaseConnected = true;
            return controller;
        } catch (RuntimeException exception) {
            System.err.println("Database unavailable, running in memory only: " + exception.getMessage());
            databaseConnected = false;
            return new ApplicationController();
        }
    }

    /**
     * Builds the controller, loads the file-analysis view, connects the two, and
     * shows the primary application window.
     *
     * @param stage primary JavaFX stage
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        ApplicationController applicationController = createApplicationController();

        FXMLLoader importLoader = new FXMLLoader(
                HelloApplication.class.getResource("import-view.fxml"));
        Tab importTab = new Tab("Import", importLoader.load());
        importTab.setClosable(false);

        FXMLLoader generateLoader = new FXMLLoader(
                HelloApplication.class.getResource("generate-view.fxml"));
        Tab generateTab = new Tab("Generate", generateLoader.load());
        generateTab.setClosable(false);

        FXMLLoader autocompleteLoader = new FXMLLoader(
                HelloApplication.class.getResource("autocomplete-view.fxml"));
        Tab autocompleteTab = new Tab("Auto-complete", autocompleteLoader.load());
        autocompleteTab.setClosable(false);

        TabPane tabs = new TabPane(importTab, generateTab, autocompleteTab);

        // FXML creates each screen's controller itself, so hand them the shared
        // application controller afterward.
        ImportController importController = importLoader.getController();
        importController.setApplicationController(applicationController);

        GenerateController generateController = generateLoader.getController();
        generateController.setApplicationController(applicationController);

        AutocompleteController autocompleteController = autocompleteLoader.getController();
        autocompleteController.setApplicationController(applicationController);

        // Contribution (October 30, 2026): I, Jinesh Patel, added this guard.
        // Generate and Autocomplete remain unavailable until vocabulary exists,
        // preventing navigation to screens without usable data.
        // AI prompt: Disable feature tabs until the shared vocabulary has words.
        boolean noVocabulary = applicationController.getKnownWordCount() == 0;
        generateTab.setDisable(noVocabulary);
        autocompleteTab.setDisable(noVocabulary);

        // Contribution (October 30, 2026): I, Jinesh Patel, connected these tabs.
        // Navigation stays in the application shell rather than coupling the
        // Import controller to other screens. Success refreshes views and selects Generate.
        // AI prompt: Refresh and enable generation/autocomplete after an import,
        // then navigate to Generate through an application-level callback.
        importController.setOnImportCompleted(() -> {
            generateController.refresh();
            autocompleteController.refresh();

            boolean vocabularyEmpty = applicationController.getKnownWordCount() == 0;
            generateTab.setDisable(vocabularyEmpty);
            autocompleteTab.setDisable(vocabularyEmpty);

            if (!generateTab.isDisable()) {
                tabs.getSelectionModel().select(generateTab);
            }
        });

        // A file may have been imported since the tab was last shown.
        generateTab.setOnSelectionChanged(event -> {
            if (generateTab.isSelected()) {
                generateController.refresh();
            }
        });

        autocompleteTab.setOnSelectionChanged(event -> {
            if (autocompleteTab.isSelected()) {
                autocompleteController.refresh();
            }
        });

        Scene scene = new Scene(tabs, WINDOW_WIDTH, WINDOW_HEIGHT);

        stage.setTitle(databaseConnected
                ? "Sentence Builder"
                : "Sentence Builder (database offline: data will not be saved)");
        stage.setScene(scene);
        stage.show();
    }
}