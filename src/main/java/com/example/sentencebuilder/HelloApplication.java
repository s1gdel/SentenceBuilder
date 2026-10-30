package com.example.sentencebuilder;

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

    /**
     * Builds the controller, loads the file-analysis view, connects the two, and
     * shows the primary application window.
     *
     * @param stage primary JavaFX stage
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        // Person 3: once WordRepository implements VocabularyRepository, pass it here,
        // for example new ApplicationController(new WordRepository(...)).
        ApplicationController applicationController = new ApplicationController();

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

        // Features that depend on learned words stay unavailable until vocabulary exists.
        boolean noVocabulary = applicationController.getKnownWordCount() == 0;
        generateTab.setDisable(noVocabulary);
        autocompleteTab.setDisable(noVocabulary);

        // A successful import immediately refreshes the next screen and moves the
        // user forward instead of requiring a manual refresh or extra navigation.
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

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }
}