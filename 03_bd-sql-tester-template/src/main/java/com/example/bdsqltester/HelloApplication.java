package com.example.bdsqltester;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    private static HelloApplication applicationInstance;
    private Stage primaryStage;
    private String loggedInUsername; // Tambahkan variabel untuk menyimpan username

    public static HelloApplication getApplicationInstance () {
        return applicationInstance;
    }

    public Stage getPrimaryStage () {
        return primaryStage;
    }

    public String getLoggedInUsername() {
        return loggedInUsername;
    }

    public void setLoggedInUsername(String loggedInUsername) {
        this.loggedInUsername = loggedInUsername;
    }

    @Override
    public void start(Stage stage) throws IOException {
        HelloApplication.applicationInstance = this;

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("login-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        primaryStage = stage;

        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}