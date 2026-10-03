package com.loanmanagement.controller;

import com.loanmanagement.model.User;
import com.loanmanagement.service.LoginService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Modality;
import com.loanmanagement.navigation.NavigationManager;

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
private Button registerButton;

@FXML
private Label registerStatusLabel;

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

    if (registerButton != null) {
        registerButton.setDisable(true);
        registerButton.setText("Creating your account...");
    }
    setRegistrationStatus("Creating your account securely...", false);

    Task<RegistrationResult> registrationTask = new Task<>() {
        @Override
        protected RegistrationResult call() {
            LoginService service = new LoginService();
            boolean registered = service.registerUser(user);
            return new RegistrationResult(registered, service.getLastErrorMessage());
        }
    };

    registrationTask.setOnSucceeded(event -> {
        restoreRegisterButton();
        RegistrationResult result = registrationTask.getValue();
        if (result.success()) {
            setRegistrationStatus("Account created successfully. You can now sign in.", true);
            showSuccess();
            clearFields();
        } else {
            setRegistrationStatus(result.message() == null
                    ? "This email may already be registered."
                    : result.message(), false);
            showError("Registration Failed", result.message() == null
                    ? "This email may already be registered."
                    : result.message());
        }
    });

    registrationTask.setOnFailed(event -> {
        restoreRegisterButton();
        setRegistrationStatus("Registration could not be completed. Please try again.", false);
        showError("Registration Failed",
                "LoanFlow could not complete registration. Please check the database connection and try again.");
    });

    Thread registrationThread = new Thread(registrationTask, "loanflow-register-customer");
    registrationThread.setDaemon(true);
    registrationThread.start();
}

private void restoreRegisterButton() {
    if (registerButton != null) {
        registerButton.setDisable(false);
        registerButton.setText("→   Create Customer Account");
    }
}

private void setRegistrationStatus(String message, boolean success) {
    if (registerStatusLabel != null) {
        registerStatusLabel.setText(message);
        registerStatusLabel.getStyleClass().removeAll("registration-status-success", "registration-status-error");
        registerStatusLabel.getStyleClass().add(success ? "registration-status-success" : "registration-status-error");
    }
}

private record RegistrationResult(boolean success, String message) { }

private void clearFields() {

    nameField.clear();
    emailField.clear();
    passwordField.clear();
    confirmPasswordField.clear();
}

private void showSuccess() {
    Stage owner = nameField.getScene() == null
            ? null
            : (Stage) nameField.getScene().getWindow();

    Stage dialog = new Stage(StageStyle.TRANSPARENT);
    if (owner != null) {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
    }

    StackPane root = new StackPane();
    root.getStyleClass().add("success-dialog-root");

    VBox card = new VBox(14);
    card.setAlignment(Pos.CENTER);
    card.setPadding(new Insets(28, 34, 30, 34));
    card.getStyleClass().add("success-dialog-card");

    StackPane icon = new StackPane();
    icon.getStyleClass().add("success-dialog-icon");
    Label checkmark = new Label("✓");
    checkmark.getStyleClass().add("success-dialog-checkmark");
    icon.getChildren().add(checkmark);

    Label kicker = new Label("ACCOUNT CREATED");
    kicker.getStyleClass().add("success-dialog-kicker");

    HBox title = new HBox(6);
    title.setAlignment(Pos.CENTER);
    Label welcome = new Label("Welcome to");
    welcome.getStyleClass().add("success-dialog-title-white");
    Label loanFlow = new Label("LoanFlow");
    loanFlow.getStyleClass().add("success-dialog-title-cyan");
    title.getChildren().addAll(welcome, loanFlow);

    Label message = new Label("Your customer account is ready.");
    message.getStyleClass().add("success-dialog-message");

    Label nextStep = new Label("You can now return to the login page and sign in.");
    nextStep.setWrapText(true);
    nextStep.setMaxWidth(340);
    nextStep.setAlignment(Pos.CENTER);
    nextStep.getStyleClass().add("success-dialog-subtitle");

    Button closeButton = new Button("Continue to Login");
    closeButton.setDefaultButton(true);
    closeButton.setMaxWidth(Double.MAX_VALUE);
    closeButton.getStyleClass().add("success-dialog-button");
    closeButton.setOnAction(event -> dialog.close());

    card.getChildren().addAll(icon, kicker, title, message, nextStep, closeButton);
    root.getChildren().add(card);

    Scene scene = new Scene(root, 470, 350);
    scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
    scene.getStylesheets().add(getClass().getResource("/css/login.css").toExternalForm());
    dialog.setScene(scene);
    dialog.setResizable(false);
    dialog.setTitle("Account Created");
    dialog.showAndWait();
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

        NavigationManager.navigate(stage, "/fxml/login.fxml", "LoanFlow - Login", null);

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
