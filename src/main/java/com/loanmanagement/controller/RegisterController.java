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

    String name = nameField.getText().trim();
    String email = emailField.getText().trim();
    String password = passwordField.getText();
    String confirmPassword =
            confirmPasswordField.getText();

    if (name.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty() ||
            confirmPassword.isEmpty()) {

        showError(
                "Incomplete Form",
                "Please fill in all fields."
        );

        return;
    }

    if (name.length() < 3) {

        showError(
                "Invalid Name",
                "Please enter your full name."
        );

        return;
    }

    if (!email.matches(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

        showError(
                "Invalid Email",
                "Please enter a valid email address."
        );

        return;
    }

    if (password.length() < 6) {

        showError(
                "Weak Password",
                "Password must contain at least 6 characters."
        );

        return;
    }

    if (!password.equals(confirmPassword)) {

        showError(
                "Password Mismatch",
                "Passwords do not match."
        );

        return;
    }

    User user =
            new User(
                    name,
                    email,
                    password
            );

    LoginService service =
            new LoginService();

    if (service.registerUser(user)) {

        showSuccess();

        clearFields();

    } else {

        String message = service.getLastErrorMessage();

        showError(
                "Registration Failed",
                message == null
                        ? "This email may already be registered."
                        : message
        );
    }
}

private void clearFields() {

    nameField.clear();
    emailField.clear();
    passwordField.clear();
    confirmPasswordField.clear();
}

private void showSuccess() {

    Alert alert =
            new Alert(Alert.AlertType.INFORMATION);

    alert.setTitle("Account Created");
    alert.setHeaderText("Welcome to LoanFlow");
    alert.setContentText(
            "Your customer account has been created successfully.\n\n" +
            "You can now return to the login page and sign in."
    );

    alert.showAndWait();
}

@FXML
public void openLogin(ActionEvent event) {

    try {

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/login.fxml"
                        )
                );

        Parent root = loader.load();

        Stage stage =
                (Stage)
                ((javafx.scene.Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setScene(
                new Scene(root, 1100, 700)
        );

        stage.setTitle(
                "LoanFlow - Login"
        );

        stage.centerOnScreen();

    } catch (Exception e) {

        e.printStackTrace();

        showError(
                "Navigation Error",
                "Unable to return to the login page."
        );
    }
}

private void showError(
        String title,
        String message) {

    Alert alert =
            new Alert(Alert.AlertType.ERROR);

    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);

    alert.showAndWait();
}


}
