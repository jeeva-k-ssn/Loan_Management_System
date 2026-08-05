package com.loanmanagement.controller;

import com.loanmanagement.model.User;
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

public class RegisterController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    public void registerUser() {

        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if(name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){

            showAlert("Please fill all fields.");
            return;
        }

        if(!password.equals(confirmPassword)){

            showAlert("Passwords do not match.");
            return;
        }

        User user = new User(name,email,password);

        LoginService service = new LoginService();

        if(service.registerUser(user)){

            showAlert("Registration Successful!");

            nameField.clear();
            emailField.clear();
            passwordField.clear();
            confirmPasswordField.clear();

        }else{

            showAlert("Registration Failed!");
        }

    }

    @FXML
    public void openLogin(ActionEvent event){

        try{

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));

            Stage stage = (Stage)((javafx.scene.Node)event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));

            stage.setTitle("Login");

        }catch(Exception e){

            e.printStackTrace();
        }

    }

    private void showAlert(String message){

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();

    }

}