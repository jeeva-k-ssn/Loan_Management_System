package com.loanmanagement.controller;

import com.loanmanagement.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

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
    // DATABASE
    // ============================================================

    private static final String DB_URL =
            "jdbc:oracle:thin:@localhost:1521:xe";

    private static final String DB_USERNAME =
            "system";


    // ============================================================
    // INITIALIZE
    // ============================================================

    @FXML
    public void initialize() {

        System.out.println(
                "DashboardController initialized."
        );
    }


    // ============================================================
    // SET CURRENT USER
    // ============================================================

    public void setCurrentUser(User user) {

        this.currentUser = user;

        System.out.println(
                "Current user: "
                        + (user != null
                        ? user.getFullName()
                        : "null")
        );

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
                    "Welcome, "
                            + currentUser.getFullName()
            );
        }


        if (roleLabel != null) {

            roleLabel.setText(
                    currentUser.getRole()
            );
        }
    }


    // ============================================================
    // ROLE-BASED UI
    // ============================================================

    private void configureRoleButtons() {

        if (currentUser == null) {
            return;
        }


        String role =
                currentUser.getRole();


        System.out.println(
                "Configuring dashboard for role: "
                        + role
        );


        // ========================================================
        // CUSTOMER
        // ========================================================

        if ("CUSTOMER".equalsIgnoreCase(role)) {

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Customer Dashboard"
                );
            }


            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Manage your loan applications and payments"
                );
            }


            if (applyLoanButton != null) {

                applyLoanButton.setVisible(true);
                applyLoanButton.setManaged(true);
            }


            if (viewLoansButton != null) {

                viewLoansButton.setVisible(true);
                viewLoansButton.setManaged(true);

                viewLoansButton.setText(
                        "▣  View Loans"
                );
            }


            if (viewPaymentsButton != null) {

                viewPaymentsButton.setVisible(true);
                viewPaymentsButton.setManaged(true);

                viewPaymentsButton.setText(
                        "₹  Payment History"
                );
            }
        }


        // ========================================================
        // LOAN OFFICER
        // ========================================================

        else if (
                "LOAN_OFFICER".equalsIgnoreCase(role)
        ) {

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Loan Officer Dashboard"
                );
            }


            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Review and manage customer loan applications"
                );
            }


            /*
             * Loan Officer should not apply for a loan.
             */

            if (applyLoanButton != null) {

                applyLoanButton.setVisible(false);
                applyLoanButton.setManaged(false);
            }


            /*
             * Reuse View Loans button as
             * Pending Applications.
             */

            if (viewLoansButton != null) {

                viewLoansButton.setVisible(true);
                viewLoansButton.setManaged(true);

                viewLoansButton.setText(
                        "▣  Pending Applications"
                );
            }


            if (viewPaymentsButton != null) {

                viewPaymentsButton.setVisible(true);
                viewPaymentsButton.setManaged(true);

                viewPaymentsButton.setText(
                        "₹  Payment History"
                );
            }
        }


        // ========================================================
        // ADMIN
        // ========================================================

        else if ("ADMIN".equalsIgnoreCase(role)) {

            if (pageTitleLabel != null) {

                pageTitleLabel.setText(
                        "Admin Dashboard"
                );
            }


            if (pageDescriptionLabel != null) {

                pageDescriptionLabel.setText(
                        "Manage and monitor the LoanFlow system"
                );
            }


            /*
             * Admin should not apply for a loan.
             */

            if (applyLoanButton != null) {

                applyLoanButton.setVisible(false);
                applyLoanButton.setManaged(false);
            }


            if (viewLoansButton != null) {

                viewLoansButton.setVisible(true);
                viewLoansButton.setManaged(true);

                viewLoansButton.setText(
                        "▣  View Loans"
                );
            }


            if (viewPaymentsButton != null) {

                viewPaymentsButton.setVisible(true);
                viewPaymentsButton.setManaged(true);

                viewPaymentsButton.setText(
                        "₹  Payment History"
                );
            }
        }
    }


    // ============================================================
    // LOAD DASHBOARD DATA
    // ============================================================

    private void loadDashboardData() {

        if (currentUser == null) {
            return;
        }


        String role =
                currentUser.getRole();


        if ("ADMIN".equalsIgnoreCase(role)) {

            loadAdminDashboard();

        } else if (
                "LOAN_OFFICER".equalsIgnoreCase(role)
        ) {

            loadLoanOfficerDashboard();

        } else {

            loadCustomerDashboard();
        }
    }


    // ============================================================
    // ADMIN DASHBOARD
    // ============================================================

    private void loadAdminDashboard() {

        String customerQuery =
                "SELECT COUNT(*) FROM LMS_CUSTOMER";

        String applicationQuery =
                "SELECT COUNT(*) FROM LOAN_APPLICATION";

        String loanQuery =
                "SELECT COUNT(*) FROM LOAN";

        String paymentQuery =
                "SELECT COUNT(*) FROM PAYMENT";


        try (Connection connection =
                     getConnection()) {


            if (customersValueLabel != null) {

                customersValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        customerQuery
                                )
                        )
                );
            }


            if (applicationsValueLabel != null) {

                applicationsValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        applicationQuery
                                )
                        )
                );
            }


            if (loansValueLabel != null) {

                loansValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        loanQuery
                                )
                        )
                );
            }


            if (paymentsValueLabel != null) {

                paymentsValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        paymentQuery
                                )
                        )
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load admin dashboard statistics.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // LOAN OFFICER DASHBOARD
    // ============================================================

    private void loadLoanOfficerDashboard() {

        String customerQuery =
                "SELECT COUNT(*) FROM LMS_CUSTOMER";

        String applicationQuery =
                "SELECT COUNT(*) FROM LOAN_APPLICATION";

        String loanQuery =
                "SELECT COUNT(*) FROM LOAN";

        String paymentQuery =
                "SELECT COUNT(*) FROM PAYMENT";


        try (Connection connection =
                     getConnection()) {


            if (customersValueLabel != null) {

                customersValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        customerQuery
                                )
                        )
                );
            }


            if (applicationsValueLabel != null) {

                applicationsValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        applicationQuery
                                )
                        )
                );
            }


            if (loansValueLabel != null) {

                loansValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        loanQuery
                                )
                        )
                );
            }


            if (paymentsValueLabel != null) {

                paymentsValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        paymentQuery
                                )
                        )
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load loan officer dashboard statistics.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // CUSTOMER DASHBOARD
    // ============================================================

    private void loadCustomerDashboard() {

        if (currentUser == null) {
            return;
        }


        String applicationQuery =
                "SELECT COUNT(*) "
                        + "FROM LOAN_APPLICATION la "
                        + "JOIN LMS_CUSTOMER lc "
                        + "ON la.CUSTOMER_ID = lc.CUSTOMER_ID "
                        + "WHERE lc.USER_ID = ?";


        String loanQuery =
                "SELECT COUNT(*) "
                        + "FROM LOAN l "
                        + "JOIN LMS_CUSTOMER lc "
                        + "ON l.CUSTOMER_ID = lc.CUSTOMER_ID "
                        + "WHERE lc.USER_ID = ?";


        String paymentQuery =
                "SELECT COUNT(*) "
                        + "FROM PAYMENT p "
                        + "JOIN LOAN l "
                        + "ON p.LOAN_ID = l.LOAN_ID "
                        + "JOIN LMS_CUSTOMER lc "
                        + "ON l.CUSTOMER_ID = lc.CUSTOMER_ID "
                        + "WHERE lc.USER_ID = ?";


        try (Connection connection =
                     getConnection()) {


            /*
             * Customers count is a system-wide statistic.
             */

            if (customersValueLabel != null) {

                customersValueLabel.setText(
                        String.valueOf(
                                getCount(
                                        connection,
                                        "SELECT COUNT(*) FROM LMS_CUSTOMER"
                                )
                        )
                );
            }


            /*
             * Current customer's applications.
             */

            if (applicationsValueLabel != null) {

                applicationsValueLabel.setText(
                        String.valueOf(
                                getCountWithUserId(
                                        connection,
                                        applicationQuery,
                                        currentUser.getUserId()
                                )
                        )
                );
            }


            /*
             * Current customer's loans.
             */

            if (loansValueLabel != null) {

                loansValueLabel.setText(
                        String.valueOf(
                                getCountWithUserId(
                                        connection,
                                        loanQuery,
                                        currentUser.getUserId()
                                )
                        )
                );
            }


            /*
             * Current customer's payments.
             */

            if (paymentsValueLabel != null) {

                paymentsValueLabel.setText(
                        String.valueOf(
                                getCountWithUserId(
                                        connection,
                                        paymentQuery,
                                        currentUser.getUserId()
                                )
                        )
                );
            }


        } catch (SQLException e) {

            e.printStackTrace();

            showError(
                    "Database Error",
                    "Unable to load customer dashboard statistics.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // APPLY LOAN
    // ============================================================

    @FXML
    private void applyLoan() {

        if (currentUser == null) {

            showError(
                    "Error",
                    "No logged-in user was found."
            );

            return;
        }


        if (!"CUSTOMER".equalsIgnoreCase(
                currentUser.getRole()
        )) {

            showError(
                    "Access Denied",
                    "Only customers can apply for loans."
            );

            return;
        }


        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/apply-loan.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            LoanApplicationController controller =
                    loader.getController();


            controller.setCurrentUser(
                    currentUser
            );


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "LoanFlow - Apply for Loan"
            );


            Scene scene =
                    new Scene(
                            root,
                            1000,
                            750
                    );


            stage.setScene(scene);

            stage.setMinWidth(850);
            stage.setMinHeight(650);

            stage.setMaximized(true);

            stage.show();


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Error",
                    "Unable to open Apply Loan page.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // VIEW LOANS / PENDING APPLICATIONS
    // ============================================================

    @FXML
    private void viewLoans() {

        if (currentUser != null
                && "LOAN_OFFICER".equalsIgnoreCase(
                currentUser.getRole()
        )) {

            openPendingApplications();

            return;
        }


        showInformation(
                "My Loans",
                "Your loan details will be implemented in the Loan Management module."
        );
    }


    // ============================================================
    // OPEN PENDING APPLICATIONS
    // ============================================================

    private void openPendingApplications() {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/fxml/pending-applications.fxml"
                            )
                    );


            Parent root =
                    loader.load();


            Stage stage =
                    new Stage();


            stage.setTitle(
                    "LoanFlow - Pending Applications"
            );


            Scene scene =
                    new Scene(
                            root,
                            1400,
                            850
                    );


            stage.setScene(scene);

            stage.setMinWidth(1100);
            stage.setMinHeight(700);

            stage.setMaximized(true);

            stage.show();


        } catch (Exception e) {

            e.printStackTrace();

            showError(
                    "Error",
                    "Unable to open Pending Applications.\n\n"
                            + e.getMessage()
            );
        }
    }


    // ============================================================
    // VIEW PAYMENTS
    // ============================================================

    @FXML
    private void viewPayments() {

        showInformation(
                "Payments",
                "Payment management will be implemented in the Payment Management module."
        );
    }


    // ============================================================
    // REFRESH
    // ============================================================

    @FXML
    private void refreshDashboard() {

        if (currentUser == null) {
            return;
        }


        loadDashboardData();
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


            Parent root =
                    loader.load();


            /*
             * Your dashboard.fxml does not have an fx:id
             * for the Logout button.
             *
             * Therefore we safely obtain the current Stage
             * from welcomeLabel, which definitely exists
             * in your FXML.
             */

            Stage currentStage =
                    (Stage) welcomeLabel
                            .getScene()
                            .getWindow();


            Scene scene =
                    new Scene(
                            root,
                            1000,
                            650
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
    // DATABASE CONNECTION
    // ============================================================

    private Connection getConnection()
            throws SQLException {

        String password =
                System.getenv(
                        "LMS_DB_PASSWORD"
                );


        if (password == null
                || password.isBlank()) {

            throw new SQLException(
                    "Database password is not configured.\n"
                            + "Please set the LMS_DB_PASSWORD "
                            + "environment variable."
            );
        }


        return DriverManager.getConnection(
                DB_URL,
                DB_USERNAME,
                password
        );
    }


    // ============================================================
    // GET SIMPLE COUNT
    // ============================================================

    private int getCount(
            Connection connection,
            String query
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(query);

             ResultSet resultSet =
                     statement.executeQuery()) {


            if (resultSet.next()) {

                return resultSet.getInt(1);
            }
        }


        return 0;
    }


    // ============================================================
    // GET COUNT USING USER ID
    // ============================================================

    private int getCountWithUserId(
            Connection connection,
            String query,
            int userId
    ) throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(query)) {


            statement.setInt(
                    1,
                    userId
            );


            try (ResultSet resultSet =
                         statement.executeQuery()) {


                if (resultSet.next()) {

                    return resultSet.getInt(1);
                }
            }
        }


        return 0;
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