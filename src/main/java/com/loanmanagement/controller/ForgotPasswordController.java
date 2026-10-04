package com.loanmanagement.controller;

import com.loanmanagement.service.LoginService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

public class ForgotPasswordController {

    @FXML private TextField emailField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;
    @FXML private Button resetButton;

    private Dialog<Void> dialog;

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("Customer", "Loan Officer", "Administrator");
        roleComboBox.setValue("Customer");
    }

    public void setDialog(Dialog<Void> dialog) {
        this.dialog = dialog;
    }

    @FXML
    private void resetPassword() {
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        String role = roleComboBox.getValue();
        String password = newPasswordField.getText();
        String confirmation = confirmPasswordField.getText();

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showStatus("Enter a valid registered email address.", true);
            emailField.requestFocus();
            return;
        }
        if (role == null) {
            showStatus("Select the role associated with your account.", true);
            return;
        }
        if (!isStrongPassword(password)) {
            showStatus("Use at least 8 characters with uppercase, lowercase, and a number.", true);
            newPasswordField.requestFocus();
            return;
        }
        if (!password.equals(confirmation)) {
            showStatus("The passwords do not match.", true);
            confirmPasswordField.requestFocus();
            return;
        }

        resetButton.setDisable(true);
        statusLabel.setText("Verifying account and updating password...");
        statusLabel.getStyleClass().removeAll("forgot-status-error", "forgot-status-success");

        LoginService service = new LoginService();
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return service.resetPassword(email, toDatabaseRole(role), password);
            }
        };
        task.setOnSucceeded(event -> {
            resetButton.setDisable(false);
            if (task.getValue()) {
                statusLabel.setText("Password updated. You can now sign in with the new password.");
                statusLabel.getStyleClass().add("forgot-status-success");
                emailField.setDisable(true);
                roleComboBox.setDisable(true);
                newPasswordField.setDisable(true);
                confirmPasswordField.setDisable(true);
                resetButton.setVisible(false);
                resetButton.setManaged(false);
                if (dialog != null) {
                    dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setVisible(true);
                    dialog.getDialogPane().lookupButton(ButtonType.CLOSE).setManaged(true);
                }
            } else {
                showStatus(service.getLastErrorMessage() == null
                        ? "Unable to reset the password. Please try again." : service.getLastErrorMessage(), true);
            }
        });
        task.setOnFailed(event -> {
            resetButton.setDisable(false);
            showStatus("Unable to reset the password. Please try again later.", true);
        });
        Thread thread = new Thread(task, "loanflow-password-reset");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void cancel() {
        if (dialog != null) {
            dialog.close();
        }
    }

    private boolean isStrongPassword(String password) {
        return password != null && password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*\\d.*");
    }

    private String toDatabaseRole(String role) {
        return switch (role) {
            case "Administrator" -> "ADMIN";
            case "Loan Officer" -> "LOAN_OFFICER";
            default -> "CUSTOMER";
        };
    }

    private void showStatus(String message, boolean error) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("forgot-status-error", "forgot-status-success");
        statusLabel.getStyleClass().add(error ? "forgot-status-error" : "forgot-status-success");
    }
}
