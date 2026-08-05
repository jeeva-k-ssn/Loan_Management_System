package com.loanmanagement.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class DashboardController {

    public void logout(ActionEvent event){

        try{

            Parent root=FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

            Stage stage=(Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Login");

        }catch(Exception e){
            e.printStackTrace();
        }

    }

}