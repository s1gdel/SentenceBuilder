package com.example.sentencebuilder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Starts the JavaFX application and displays the current Sentence Builder screen.
 */
public class HelloApplication extends Application {
    private static final double WINDOW_WIDTH = 720;
    private static final double WINDOW_HEIGHT = 420;

    /**
     * Loads the file-analysis view and shows the primary application window.
     *
     * @param stage primary JavaFX stage
     * @throws IOException if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("import-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), WINDOW_WIDTH, WINDOW_HEIGHT);

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }
}
