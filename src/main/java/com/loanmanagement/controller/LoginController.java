package com.loanmanagement.controller;

import com.loanmanagement.model.User;
import com.loanmanagement.service.LoginService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import com.loanmanagement.navigation.NavigationManager;

public class LoginController {

@FXML
private TextField emailField;

@FXML
private PasswordField passwordField;

@FXML
private TextField visiblePasswordField;

@FXML
private ToggleButton passwordToggle;

@FXML
private ComboBox<String> roleComboBox;

@FXML
public void initialize() {

    roleComboBox.getItems().clear();

    roleComboBox.getItems().addAll(
            "Customer",
            "Loan Officer",
            "Administrator"
    );

    roleComboBox.setValue("Customer");

}

@FXML
public void loginUser() {

    String email = emailField.getText().trim();
    String password = passwordField.getText();
    String selectedRole = roleComboBox.getValue();

    if (email.isEmpty()) {
        showError(
                "Missing Email",
                "Please enter your email address."
        );
        emailField.requestFocus();
        return;
    }

    if (password.isEmpty()) {
        showError(
                "Missing Password",
                "Please enter your password."
        );
        passwordField.requestFocus();
        return;
    }

    if (selectedRole == null) {
        showError(
                "Role Required",
                "Please select how you want to login."
        );
        return;
    }

    String databaseRole =
            convertRoleToDatabaseValue(selectedRole);

    LoginService service = new LoginService();
    passwordField.setDisable(true);
    emailField.setDisable(true);
    roleComboBox.setDisable(true);
    Task<User> loginTask = new Task<>() {
        @Override protected User call() { return service.loginUser(email, password, databaseRole); }
    };
    loginTask.setOnSucceeded(event -> {
        passwordField.setDisable(false); emailField.setDisable(false); roleComboBox.setDisable(false);
        User user = loginTask.getValue();
        if (user != null) openDashboard(user);
        else showError("Login Failed", service.getLastErrorMessage() == null
                ? "The email, password, or selected role is incorrect." : service.getLastErrorMessage());
    });
    loginTask.setOnFailed(event -> {
        passwordField.setDisable(false); emailField.setDisable(false); roleComboBox.setDisable(false);
        showError("Login Failed", "LoanFlow could not complete sign in. Please try again.");
    });
    Thread loginThread = new Thread(loginTask, "loanflow-login");
    loginThread.setDaemon(true);
    loginThread.start();
}

@FXML
private void togglePassword() {
    boolean visible = passwordToggle.isSelected();
    if (visible) {
        visiblePasswordField.setText(passwordField.getText());
        passwordField.setManaged(false);
        passwordField.setVisible(false);
        visiblePasswordField.setManaged(true);
        visiblePasswordField.setVisible(true);
        visiblePasswordField.requestFocus();
        passwordToggle.setText("Hide");
    } else {
        passwordField.setText(visiblePasswordField.getText());
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        passwordField.setManaged(true);
        passwordField.setVisible(true);
        passwordField.requestFocus();
        passwordToggle.setText("Show");
    }
}

private String convertRoleToDatabaseValue(String role) {

    switch (role) {

        case "Administrator":
            return "ADMIN";

        case "Loan Officer":
            return "LOAN_OFFICER";

        case "Customer":
            return "CUSTOMER";

        default:
            return "";
    }
}

private void openDashboard(User user) {

    try {

        Stage stage = (Stage) emailField.getScene().getWindow();
        NavigationManager.navigate(stage, "/fxml/dashboard.fxml", "LoanFlow - " + user.getDisplayRole(),
                controller -> ((DashboardController) controller).setCurrentUser(user));

    } catch (Exception e) {

        e.printStackTrace();

        showError(
                "Dashboard Error",
                "Unable to open the dashboard."
        );
    }
}

@FXML
public void openRegister(ActionEvent event) {

    try {

        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        NavigationManager.navigate(stage, "/fxml/register.fxml", "LoanFlow - Create Account", null);

    } catch (Exception e) {

        e.printStackTrace();

        showError(
                "Navigation Error",
                "Unable to open the registration page."
        );
    }
}

private void showError(String title, String message) {

    Alert alert =
            new Alert(Alert.AlertType.ERROR);

    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);

    alert.showAndWait();
}


}
