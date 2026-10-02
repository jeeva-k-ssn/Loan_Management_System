package com.loanmanagement;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

@Override
public void start(Stage stage) throws Exception {

    FXMLLoader loader =
            new FXMLLoader(
                    getClass().getResource(
                            "/fxml/login.fxml"
                    )
            );

    Scene scene = new Scene(loader.load());

    stage.setTitle(
            "LoanFlow - Loan Management System"
    );

    stage.setScene(scene);

    stage.setMinWidth(950);
    stage.setMinHeight(620);
    stage.setResizable(true);
    stage.setFullScreen(false);
    stage.setMaximized(false);
    // Start at a comfortable desktop size instead of forcing an immediate
    // maximization, which makes the responsive login artwork appear to zoom.
    stage.setWidth(1440);
    stage.setHeight(860);
    stage.centerOnScreen();
    stage.show();
}

public static void main(String[] args) {
    launch(args);
}


}
