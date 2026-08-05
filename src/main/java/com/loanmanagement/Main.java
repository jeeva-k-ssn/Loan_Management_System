package com.loanmanagement;

import com.loanmanagement.database.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.Connection;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));

        Scene scene = new Scene(loader.load(), 500, 400);

        stage.setTitle("Loan Management System");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {

        // Test Oracle Database Connection
        Connection connection = DatabaseConnection.getConnection();

        if (connection != null) {
            System.out.println("Connected to Oracle Database!");
        }

        launch(args);
    }
}