package com.loanmanagement.controller;

import com.loanmanagement.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    // ============================================================
    // HEADER
    // ============================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;


    // ============================================================
    // PAGE HEADER
    // ============================================================

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageDescriptionLabel;


    // ============================================================
    // STAT CARDS
    // ============================================================

    @FXML
    private Label customersValueLabel;

    @FXML
    private Label applicationsValueLabel;

    @FXML
    private Label loansValueLabel;

    @FXML
    private Label paymentsValueLabel;


    // ============================================================
    // ACTIVITY
    // ============================================================

    @FXML
    private VBox activityContainer;


    // ============================================================
    // QUICK ACTION BUTTONS
    // ============================================================

    @FXML
    private Button applyLoanButton;

    @FXML
    private Button viewLoansButton;

    @FXML
    private Button viewPaymentsButton;

    @FXML
    private Button pendingApplicationsButton;


    // ============================================================
    // CURRENT USER
    // ============================================================

    private User currentUser;


    // ============================================================
    // INITIALIZE
    // ============================================================

    @FXML
    public void initialize() {

        /*
         * The dashboard is loaded first and the actual user is
         * supplied through setCurrentUser().
         *
         * Therefore, do not load role-specific information here.
         */

        configureDefaultButtons();
    }


    // ============================================================
    // SET CURRENT USER
    // ============================================================

    public void setCurrentUser(User user) {

        this.currentUser = user;

        updateUserInformation();

        configureRoleButtons();

        loadDashboardData();
    }


    // ============================================================
    // UPDATE USER INFORMATION
    // ============================================================

    private void updateUserInformation() {

        if (currentUser == null) {
            return;
        }

        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "Welcome, " + currentUser.getFullName()
            );
        }

        if (roleLabel != null) {

            roleLabel.setText(
                    currentUser.getRole()
            );
        }
    }


    // ============================================================
    // DEFAULT BUTTON CONFIGURATION
    // ============================================================

    private void configureDefaultButtons() {

        if (applyLoanButton != null) {
            applyLoanButton.setVisible(false);
            applyLoanButton.setManaged(false);
        }

        if (viewLoansButton != null) {
            viewLoansButton.setVisible(false);
            viewLoansButton.setManaged(false);
        }

        if (viewPaymentsButton != null) {
            viewPaymentsButton.setVisible(false);
            viewPaymentsButton.setManaged(false);
        }

        if (pendingApplicationsButton != null) {
            pendingApplicationsButton.setVisible(false);
            pendingApplicationsButton.setManaged(false);
        }
    }


    // ============================================================
    // ROLE BASED BUTTON CONFIGURATION
    // ============================================================

    private void configureRoleButtons() {

        if (currentUser == null) {
            return;
        }

        configureDefaultButtons();

        String role = currentUser.getRole();

        if (role == null) {
            return;
        }

        role = role.trim().toUpperCase();


        // ========================================================
        // CUSTOMER
        // ========================================================

        if (role.equals("CUSTOMER")) {

            showButton(applyLoanButton);

            showButton(viewLoansButton);

            showButton(viewPaymentsButton);

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Customer Dashboard"
                );
            }

            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Manage your loan applications and payments."
                );
            }
        }


        // ========================================================
        // LOAN OFFICER
        // ========================================================

        else if (role.equals("LOAN_OFFICER")) {

            showButton(pendingApplicationsButton);

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Loan Officer Dashboard"
                );
            }

            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Review customer applications and manage loans."
                );
            }
        }


        // ========================================================
        // ADMIN
        // ========================================================

        else if (role.equals("ADMIN")) {

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Admin Dashboard"
                );
            }

            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Monitor customers, applications, loans and payments."
                );
            }
        }
    }


    // ============================================================
    // SHOW BUTTON
    // ============================================================

    private void showButton(Button button) {

        if (button == null) {
            return;
        }

        button.setVisible(true);
        button.setManaged(true);
    }


    // ============================================================
    // LOAD DASHBOARD DATA
    // ============================================================

    private void loadDashboardData() {

        if (currentUser == null) {
            return;
        }

        loadCustomersCount();

        loadApplicationsCount();

        loadLoansCount();

        loadPaymentsCount();

        loadRecentActivity();
    }


    // ============================================================
    // CUSTOMERS COUNT
    // ============================================================

    private void loadCustomersCount() {

        String query =
                "SELECT COUNT(*) FROM LMS_CUSTOMER";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (resultSet.next()) {

                customersValueLabel.setText(
                        String.valueOf(
                                resultSet.getInt(1)
                        )
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            setLabelValue(
                    customersValueLabel,
                    "0"
            );
        }
    }


    // ============================================================
    // APPLICATIONS COUNT
    // ============================================================

    private void loadApplicationsCount() {

        String query =
                "SELECT COUNT(*) FROM LOAN_APPLICATION";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (resultSet.next()) {

                applicationsValueLabel.setText(
                        String.valueOf(
                                resultSet.getInt(1)
                        )
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            setLabelValue(
                    applicationsValueLabel,
                    "0"
            );
        }
    }


    // ============================================================
    // LOANS COUNT
    // ============================================================

    private void loadLoansCount() {

        String query =
                "SELECT COUNT(*) FROM LOAN";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (resultSet.next()) {

                loansValueLabel.setText(
                        String.valueOf(
                                resultSet.getInt(1)
                        )
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            setLabelValue(
                    loansValueLabel,
                    "0"
            );
        }
    }


    // ============================================================
    // PAYMENTS COUNT
    // ============================================================

    private void loadPaymentsCount() {

        String query =
                "SELECT COUNT(*) FROM PAYMENT";

        try (Connection connection = getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet =
                     statement.executeQuery()) {

            if (resultSet.next()) {

                paymentsValueLabel.setText(
                        String.valueOf(
                                resultSet.getInt(1)
                        )
                );
            }

        } catch (SQLException e) {

            e.printStackTrace();

            setLabelValue(
                    paymentsValueLabel,
                    "0"
            );
        }
    }


    // ============================================================
    // RECENT ACTIVITY
    // ============================================================

    private void loadRecentActivity() {

        if (activityContainer == null) {
            return;
        }

        activityContainer.getChildren().clear();

        Label emptyLabel =
                new Label(
                        "No recent activity available."
                );

        emptyLabel.getStyleClass().add(
                "empty-label"
        );

        activityContainer.getChildren().add(
                emptyLabel
        );
    }


    // ============================================================
    // REFRESH DASHBOARD
    // ============================================================

    @FXML
    private void refreshDashboard() {

        if (currentUser == null) {
            return;
        }

        updateUserInformation();

        configureRoleButtons();

        loadDashboardData();
    }


    // ============================================================
    // APPLY LOAN
    // ============================================================

    @FXML
    private void applyLoan() {

        if (currentUser == null) {
            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/apply-loan.fxml"
                            )
                    );

            Parent root = loader.load();

            LoanApplicationController controller =
                    loader.getController();

            controller.setCurrentUser(
                    currentUser
            );

            Stage currentStage =
                    getCurrentStage();

            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );

            currentStage.setScene(scene);

            currentStage.setTitle(
                    "LoanFlow - Apply for Loan"
            );

            currentStage.setMaximized(true);

        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Unable to open loan application page.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // VIEW LOANS
    // ============================================================

    @FXML
    private void viewLoans() {

        if (currentUser == null) {
            return;
        }

        showInformation(
                "Not Available",
                "The loans page has not been added yet."
        );
    }


    // ============================================================
    // VIEW PAYMENTS
    // ============================================================

    @FXML
    private void viewPayments() {

        if (currentUser == null) {
            return;
        }

        showInformation(
                "Not Available",
                "The payment history page has not been added yet."
        );
    }


    // ============================================================
    // OPEN PENDING APPLICATIONS
    // ============================================================

    @FXML
    private void openPendingApplications() {

        if (currentUser == null) {

            showError(
                    "Navigation Error",
                    "No logged-in user found."
            );

            return;
        }

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/pending-applications.fxml"
                            )
                    );

            Parent root = loader.load();


            // ----------------------------------------------------
            // GET PENDING APPLICATION CONTROLLER
            // ----------------------------------------------------

            PendingApplicationsController controller =
                    loader.getController();


            // ----------------------------------------------------
            // VERY IMPORTANT:
            // PASS THE CURRENT LOAN OFFICER
            // ----------------------------------------------------

            controller.setCurrentUser(
                    currentUser
            );


            Stage currentStage =
                    (Stage) pendingApplicationsButton
                            .getScene()
                            .getWindow();


            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );


            currentStage.setScene(scene);

            currentStage.setTitle(
                    "LoanFlow - Pending Applications"
            );

            currentStage.setMaximized(true);


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Unable to open pending applications.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    @FXML
    private void logout() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/login.fxml"
                            )
                    );

            Parent root = loader.load();


            Stage currentStage =
                    getCurrentStage();


            Scene scene =
                    new Scene(
                            root,
                            900,
                            600
                    );


            currentStage.setScene(scene);

            currentStage.setTitle(
                    "LoanFlow - Login"
            );

            currentStage.setMaximized(false);

            currentStage.centerOnScreen();


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Logout Error",
                    "Unable to return to login page.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // OPEN PAGE HELPER
    // ============================================================

    private void openPage(
            String fxmlPath,
            String title
    ) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    fxmlPath
                            )
                    );

            Parent root = loader.load();


            Stage currentStage =
                    getCurrentStage();


            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );


            currentStage.setScene(scene);

            currentStage.setTitle(title);

            currentStage.setMaximized(true);


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Navigation Error",
                    "Unable to open page.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // GET CURRENT STAGE
    // ============================================================

    private Stage getCurrentStage() {

        if (welcomeLabel != null &&
                welcomeLabel.getScene() != null) {

            return (Stage)
                    welcomeLabel
                            .getScene()
                            .getWindow();
        }

        if (pageTitleLabel != null &&
                pageTitleLabel.getScene() != null) {

            return (Stage)
                    pageTitleLabel
                            .getScene()
                            .getWindow();
        }

        throw new IllegalStateException(
                "Unable to find current application window."
        );
    }


    // ============================================================
    // SET LABEL VALUE
    // ============================================================

    private void setLabelValue(
            Label label,
            String value
    ) {

        if (label != null) {

            label.setText(value);
        }
    }


    // ============================================================
    // DATABASE CONNECTION
    // ============================================================

    private Connection getConnection()
            throws SQLException {

        String password =
                System.getenv(
                        "LMS_DB_PASSWORD"
                );


        if (password == null ||
                password.isBlank()) {

            throw new SQLException(
                    "Database password is not configured.\n"
                            + "Please set the LMS_DB_PASSWORD "
                            + "environment variable."
            );
        }


        return DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe",
                "system",
                password
        );
    }


    // ============================================================
    // ERROR ALERT
    // ============================================================

    private void showError(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.ERROR
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }


    // ============================================================
    // INFORMATION ALERT
    // ============================================================

    private void showInformation(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(
                        Alert.AlertType.INFORMATION
                );

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();
    }
}
