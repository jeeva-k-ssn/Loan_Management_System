package com.loanmanagement.controller;

import com.loanmanagement.service.LoginService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.scene.Scene;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    
    public void loginUser(){

    String email=emailField.getText();
    String password=passwordField.getText();

    LoginService service=new LoginService();

    if(service.loginUser(email,password)){

        try{

            Parent root=FXMLLoader.load(getClass().getResource("/fxml/dashboard.fxml"));

            Stage stage=(Stage)emailField.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");

        }catch(Exception e){

            e.printStackTrace();

        }

    }else{

        Alert alert=new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText("Invalid Email or Password");
        alert.showAndWait();

    }

}

    @FXML
    public void openRegister(ActionEvent event) {

        try {

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/register.fxml"));

            Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));

            stage.setTitle("Register");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String message) {

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}