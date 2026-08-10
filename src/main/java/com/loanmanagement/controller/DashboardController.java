package com.loanmanagement.controller;

import com.loanmanagement.model.User;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DashboardController {

    // ============================================================
    // FXML COMPONENTS
    // ============================================================

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label pageTitleLabel;

    @FXML
    private Label pageDescriptionLabel;

    @FXML
    private Label customersValueLabel;

    @FXML
    private Label applicationsValueLabel;

    @FXML
    private Label loansValueLabel;

    @FXML
    private Label paymentsValueLabel;

    @FXML
    private VBox activityContainer;

    @FXML
    private Button applyLoanButton;

    @FXML
    private Button viewLoansButton;

    @FXML
    private Button viewPaymentsButton;


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
         * The logged-in user is supplied by LoginController
         * through setCurrentUser().
         */
    }


    // ============================================================
    // SET CURRENT USER
    // ============================================================

    public void setCurrentUser(User user) {

        this.currentUser = user;

        if (user == null) {
            return;
        }

        updateUserInformation();
        loadDashboardData();
    }


    // ============================================================
    // UPDATE USER INFORMATION
    // ============================================================

    private void updateUserInformation() {

        if (currentUser == null) {
            return;
        }

        String fullName = currentUser.getFullName();
        String role = currentUser.getRole();


        // ----------------------------
        // Welcome message
        // ----------------------------

        if (welcomeLabel != null) {

            welcomeLabel.setText(
                    "Welcome, " + fullName
            );
        }


        // ----------------------------
        // Role
        // ----------------------------

        if (roleLabel != null) {

            roleLabel.setText(
                    currentUser.getDisplayRole()
            );
        }


        // ----------------------------
        // Dashboard title
        // ----------------------------

        if (pageTitleLabel != null) {

            if ("ADMIN".equalsIgnoreCase(role)) {

                pageTitleLabel.setText(
                        "Administrator Dashboard"
                );

            } else if ("LOAN_OFFICER".equalsIgnoreCase(role)) {

                pageTitleLabel.setText(
                        "Loan Officer Dashboard"
                );

            } else {

                pageTitleLabel.setText(
                        "Customer Dashboard"
                );
            }
        }


        // ----------------------------
        // Dashboard description
        // ----------------------------

        if (pageDescriptionLabel != null) {

            if ("ADMIN".equalsIgnoreCase(role)) {

                pageDescriptionLabel.setText(
                        "Monitor customers, applications, loans and payments"
                );

            } else if ("LOAN_OFFICER".equalsIgnoreCase(role)) {

                pageDescriptionLabel.setText(
                        "Review applications and manage loan activities"
                );

            } else {

                pageDescriptionLabel.setText(
                        "Manage your loan applications, loans and payments"
                );
            }
        }


        configureRoleButtons();
    }


    // ============================================================
    // ROLE BASED BUTTONS
    // ============================================================

    private void configureRoleButtons() {

        if (currentUser == null) {
            return;
        }

        String role = currentUser.getRole();


        /*
         * CUSTOMER
         *
         * Customer can apply for a loan.
         */

        if ("CUSTOMER".equalsIgnoreCase(role)) {

            if (applyLoanButton != null) {

                applyLoanButton.setVisible(true);
                applyLoanButton.setManaged(true);
            }

        } else {

            /*
             * ADMIN and LOAN OFFICER
             */

            if (applyLoanButton != null) {

                applyLoanButton.setVisible(false);
                applyLoanButton.setManaged(false);
            }
        }


        /*
         * Loans button
         */

        if (viewLoansButton != null) {

            viewLoansButton.setVisible(true);
            viewLoansButton.setManaged(true);
        }


        /*
         * Payments button
         */

        if (viewPaymentsButton != null) {

            viewPaymentsButton.setVisible(true);
            viewPaymentsButton.setManaged(true);
        }
    }


    // ============================================================
    // LOAD DASHBOARD DATA
    // ============================================================

    private void loadDashboardData() {

        if (currentUser == null) {
            return;
        }

        String role = currentUser.getRole();


        try (Connection connection = getConnection()) {

            if ("ADMIN".equalsIgnoreCase(role)) {

                loadAdminDashboard(connection);

            } else if ("LOAN_OFFICER".equalsIgnoreCase(role)) {

                loadLoanOfficerDashboard(connection);

            } else {

                loadCustomerDashboard(connection);
            }

        } catch (SQLException e) {

            System.out.println(
                    "Dashboard database error: "
                            + e.getMessage()
            );

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load dashboard data.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    private void loadAdminDashboard(
            Connection connection
    ) throws SQLException {

        int customers = getCount(
                connection,
                "SELECT COUNT(*) FROM CUSTOMER"
        );


        int applications = getCount(
                connection,
                "SELECT COUNT(*) FROM LOAN_APPLICATION"
        );


        int loans = getCount(
                connection,
                "SELECT COUNT(*) FROM LOAN"
        );


        int payments = getCount(
                connection,
                "SELECT COUNT(*) FROM PAYMENT"
        );


        setDashboardValues(
                customers,
                applications,
                loans,
                payments
        );


        updateActivity(
                "System Overview",
                "Monitor customers, loan applications, "
                        + "loans and payments from one place."
        );
    }


    // ============================================================
    // LOAN OFFICER DASHBOARD
    // ============================================================

    private void loadLoanOfficerDashboard(
            Connection connection
    ) throws SQLException {

        int customers = getCount(
                connection,
                "SELECT COUNT(*) FROM CUSTOMER"
        );


        int applications = getCount(
                connection,
                "SELECT COUNT(*) FROM LOAN_APPLICATION"
        );


        int loans = getCount(
                connection,
                "SELECT COUNT(*) FROM LOAN"
        );


        int payments = getCount(
                connection,
                "SELECT COUNT(*) FROM PAYMENT"
        );


        setDashboardValues(
                customers,
                applications,
                loans,
                payments
        );


        updateActivity(
                "Loan Officer Workspace",
                "Review applications, monitor approved loans "
                        + "and track customer payments."
        );
    }


    // ============================================================
    // CUSTOMER DASHBOARD
    // ============================================================

    private void loadCustomerDashboard(
            Connection connection
    ) throws SQLException {

        int userId = currentUser.getUserId();


        int customerId = findCustomerId(
                connection,
                userId
        );


        /*
         * If a customer record is not connected to
         * the USERS table yet, don't crash the dashboard.
         */

        if (customerId == -1) {

            setDashboardValues(
                    1,
                    0,
                    0,
                    0
            );


            updateActivity(
                    "Welcome to LoanFlow",
                    "Your loan applications, loans and "
                            + "payments will appear here."
            );

            return;
        }


        int applications = getCount(
                connection,
                "SELECT COUNT(*) "
                        + "FROM LOAN_APPLICATION "
                        + "WHERE CUSTOMER_ID = ?",
                customerId
        );


        int loans = getCount(
                connection,
                "SELECT COUNT(*) "
                        + "FROM LOAN "
                        + "WHERE CUSTOMER_ID = ?",
                customerId
        );


        int payments = getPaymentCountForCustomer(
                connection,
                customerId
        );


        setDashboardValues(
                1,
                applications,
                loans,
                payments
        );


        updateActivity(
                "Your Loan Activity",
                "Applications: "
                        + applications
                        + "   •   Loans: "
                        + loans
                        + "   •   Payments: "
                        + payments
        );
    }


    // ============================================================
    // FIND CUSTOMER ID
    // ============================================================

    private int findCustomerId(
            Connection connection,
            int userId
    ) {

        /*
         * First try CUSTOMER table.
         */

        String sql =
                "SELECT CUSTOMER_ID "
                        + "FROM CUSTOMER "
                        + "WHERE USER_ID = ?";


        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);


            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(
                            "CUSTOMER_ID"
                    );
                }
            }

        } catch (SQLException e) {

            /*
             * Try LMS_CUSTOMER below.
             */
        }


        /*
         * Try LMS_CUSTOMER table.
         */

        sql =
                "SELECT CUSTOMER_ID "
                        + "FROM LMS_CUSTOMER "
                        + "WHERE USER_ID = ?";


        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, userId);


            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(
                            "CUSTOMER_ID"
                    );
                }
            }

        } catch (SQLException e) {

            /*
             * No matching customer.
             */
        }


        return -1;
    }


    // ============================================================
    // PAYMENT COUNT
    // ============================================================

    private int getPaymentCountForCustomer(
            Connection connection,
            int customerId
    ) throws SQLException {

        String sql =
                "SELECT COUNT(*) "
                        + "FROM PAYMENT P "
                        + "JOIN LOAN L "
                        + "ON P.LOAN_ID = L.LOAN_ID "
                        + "WHERE L.CUSTOMER_ID = ?";


        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, customerId);


            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(1);
                }
            }
        }


        return 0;
    }


    // ============================================================
    // GENERIC COUNT
    // ============================================================

    private int getCount(
            Connection connection,
            String sql
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(1);
                }
            }
        }


        return 0;
    }


    // ============================================================
    // COUNT WITH PARAMETER
    // ============================================================

    private int getCount(
            Connection connection,
            String sql,
            int parameter
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setInt(1, parameter);


            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return result.getInt(1);
                }
            }
        }


        return 0;
    }


    // ============================================================
    // SET DASHBOARD VALUES
    // ============================================================

    private void setDashboardValues(
            int customers,
            int applications,
            int loans,
            int payments
    ) {

        if (customersValueLabel != null) {

            customersValueLabel.setText(
                    String.valueOf(customers)
            );
        }


        if (applicationsValueLabel != null) {

            applicationsValueLabel.setText(
                    String.valueOf(applications)
            );
        }


        if (loansValueLabel != null) {

            loansValueLabel.setText(
                    String.valueOf(loans)
            );
        }


        if (paymentsValueLabel != null) {

            paymentsValueLabel.setText(
                    String.valueOf(payments)
            );
        }
    }


    // ============================================================
    // ACTIVITY
    // ============================================================

    private void updateActivity(
            String title,
            String description
    ) {

        if (activityContainer == null) {
            return;
        }


        activityContainer.getChildren().clear();


        VBox activityBox =
                new VBox(5);


        activityBox.setStyle(
                "-fx-background-color: #F7FAFC;"
                        + "-fx-background-radius: 10;"
                        + "-fx-padding: 14;"
        );


        Label titleLabel =
                new Label(title);


        titleLabel.setStyle(
                "-fx-text-fill: #183B56;"
                        + "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
        );


        Label descriptionLabel =
                new Label(description);


        descriptionLabel.setWrapText(true);


        descriptionLabel.setStyle(
                "-fx-text-fill: #8291A5;"
                        + "-fx-font-size: 11px;"
        );


        activityBox.getChildren().addAll(
                titleLabel,
                descriptionLabel
        );


        activityContainer.getChildren().add(
                activityBox
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


        loadDashboardData();


        showInformation(
                "Dashboard Refreshed",
                "Your dashboard has been updated successfully."
        );
    }


    // ============================================================
    // APPLY FOR LOAN
    // ============================================================

    @FXML
    private void applyLoan() {

        showInformation(
                "Loan Application",
                "The Loan Application module will be connected here."
        );
    }


    // ============================================================
    // VIEW LOANS
    // ============================================================

    @FXML
    private void viewLoans() {

        showInformation(
                "My Loans",
                "Your loan details will be displayed here."
        );
    }


    // ============================================================
    // VIEW PAYMENTS
    // ============================================================

    @FXML
    private void viewPayments() {

        showInformation(
                "Payment History",
                "Your payment history will be displayed here."
        );
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    @FXML
    private void logout(
            javafx.event.ActionEvent event
    ) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/login.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            Stage stage =
                    (Stage)
                            ((Node) event.getSource())
                                    .getScene()
                                    .getWindow();


            Scene scene =
                    new Scene(root);


            stage.setScene(scene);


            stage.setTitle(
                    "LoanFlow - Login"
            );


            stage.centerOnScreen();


        } catch (IOException e) {

            e.printStackTrace();


            showError(
                    "Logout Error",
                    "Unable to return to the login page."
            );
        }
    }


    // ============================================================
    // DATABASE CONNECTION
    // ============================================================

    private Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(
                "jdbc:oracle:thin:@localhost:1521:xe",
                "system",
                "Jeeva_Krishnan2505"
        );
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
}