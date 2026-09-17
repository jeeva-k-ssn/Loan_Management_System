package com.loanmanagement.navigation;

import java.io.IOException;
import java.util.function.Consumer;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Centralized navigation for the single LoanFlow desktop window. */
public final class NavigationManager {
    private NavigationManager() { }

    public static <T> T navigate(Stage stage, String resource, String title,
                                  Consumer<T> controllerSetup) throws IOException {
        FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(resource));
        Parent root = loader.load();
        T controller = loader.getController();
        if (controllerSetup != null) controllerSetup.accept(controller);

        if (stage.getScene() == null) {
            stage.setScene(new Scene(root));
        } else {
            stage.getScene().setRoot(root);
        }
        stage.setTitle(title);
        stage.setMaximized(true);
        return controller;
    }

    public static Stage stageOf(javafx.scene.Node node) {
        return (Stage) node.getScene().getWindow();
    }
}
