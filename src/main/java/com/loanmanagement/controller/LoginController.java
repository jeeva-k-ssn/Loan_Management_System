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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class LoginController {

@FXML private ImageView heroImage;
@FXML private Label heroCaption;
@FXML private HBox heroDots;
private Timeline heroCarousel;


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

    startHeroCarousel();
}

private void startHeroCarousel() {
    Image[] images = {
            new Image(getClass().getResource("/images/login-hero-1.png").toExternalForm()),
            new Image(getClass().getResource("/images/login-hero-2.png").toExternalForm()),
            new Image(getClass().getResource("/images/login-hero-3.png").toExternalForm())
    };
    String[] captions = {"Build your next chapter.", "Make room for what matters.", "Plans that move with you."};
    heroCarousel = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
        int next = (int) ((heroCarousel.getCurrentTime().toSeconds() / 5) % images.length);
        heroImage.setImage(images[next]);
        heroCaption.setText(captions[next]);
        for (int i = 0; i < heroDots.getChildren().size(); i++) {
            heroDots.getChildren().get(i).getStyleClass().remove("active");
            if (i == next) heroDots.getChildren().get(i).getStyleClass().add("active");
        }
    }));
    heroCarousel.setCycleCount(Timeline.INDEFINITE);
    heroCarousel.play();
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

    User user =
            service.loginUser(
                    email,
                    password,
                    databaseRole
            );

    if (user != null) {

        openDashboard(user);

    } else {

        String message = service.getLastErrorMessage();

        showError(
                "Login Failed",
                message == null
                        ? "The email, password, or selected role is incorrect."
                        : message
        );
    }
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

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/dashboard.fxml"
                        )
                );

        Parent root = loader.load();

        DashboardController controller =
                loader.getController();

        controller.setCurrentUser(user);

        Stage stage =
                (Stage) emailField
                        .getScene()
                        .getWindow();

        Scene scene =
                new Scene(root, 1280, 760);

        stage.setScene(scene);

        stage.setTitle(
                "LoanFlow - " + user.getDisplayRole()
        );

        stage.setMinWidth(1100);
        stage.setMinHeight(680);

        stage.centerOnScreen();

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

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/fxml/register.fxml"
                        )
                );

        Parent root = loader.load();

        Stage stage =
                (Stage)
                ((javafx.scene.Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setScene(
                new Scene(root, 1050, 700)
        );

        stage.setTitle(
                "LoanFlow - Create Account"
        );

        stage.centerOnScreen();

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
