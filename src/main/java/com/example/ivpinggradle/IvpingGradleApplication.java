package com.example.ivpinggradle;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class IvpingGradleApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(IvpingGradleApplication.class.getResource("ivpinggradle-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 522);
        stage.setTitle("IvpingGradle");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
