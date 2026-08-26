package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/** Displays only the dashboard data and actions available to the signed-in role. */
public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Label pageTitleLabel;
    @FXML private Label pageDescriptionLabel;
    @FXML private Label customersCardLabel;
    @FXML private Label applicationsCardLabel;
    @FXML private Label loansCardLabel;
    @FXML private Label paymentsCardLabel;
    @FXML private Label customersCardDescription;
    @FXML private Label applicationsCardDescription;
    @FXML private Label loansCardDescription;
    @FXML private Label paymentsCardDescription;
    @FXML private Label customersValueLabel;
    @FXML private Label applicationsValueLabel;
    @FXML private Label loansValueLabel;
    @FXML private Label paymentsValueLabel;
    @FXML private VBox activityContainer;
    @FXML private Button applyLoanButton;
    @FXML private Button viewLoansButton;
    @FXML private Button viewPaymentsButton;
    @FXML private Button pendingApplicationsButton;

    private User currentUser;

    @FXML
    public void initialize() {
        configureDefaultButtons();
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        if (user == null) return;
        welcomeLabel.setText("Welcome, " + user.getFullName());
        roleLabel.setText(user.getDisplayRole());
        configureRoleView();
        loadDashboardData();
    }

    private void configureRoleView() {
        configureDefaultButtons();
        if (hasRole("CUSTOMER")) {
            pageTitleLabel.setText("Your loan overview");
            pageDescriptionLabel.setText("Track applications, active loans, and repayments in one place.");
            setCardCopy("APPLICATIONS", "Submitted by you", "ACTIVE LOANS", "Currently being repaid",
                    "PAYMENTS", "Successful repayments", "TOTAL PAID", "Across your loans");
            showButton(applyLoanButton); showButton(viewLoansButton); showButton(viewPaymentsButton);
        } else if (hasRole("LOAN_OFFICER")) {
            pageTitleLabel.setText("Loan processing workspace");
            pageDescriptionLabel.setText("Review applications and monitor the current lending portfolio.");
            setCardCopy("PENDING", "Awaiting review", "APPROVED", "Approved applications",
                    "ACTIVE LOANS", "Open loan accounts", "PAYMENTS", "Payments received");
            showButton(pendingApplicationsButton);
        } else if (hasRole("ADMIN")) {
            pageTitleLabel.setText("Administrative overview");
            pageDescriptionLabel.setText("Monitor customers, lending activity, and payment operations.");
            setCardCopy("CUSTOMERS", "Registered customers", "APPLICATIONS", "All applications",
                    "ACTIVE LOANS", "Currently active", "PAYMENTS", "Recorded payments");
        }
    }

    private void setCardCopy(String a, String ad, String b, String bd, String c, String cd, String d, String dd) {
        customersCardLabel.setText(a); customersCardDescription.setText(ad);
        applicationsCardLabel.setText(b); applicationsCardDescription.setText(bd);
        loansCardLabel.setText(c); loansCardDescription.setText(cd);
        paymentsCardLabel.setText(d); paymentsCardDescription.setText(dd);
    }

    private void configureDefaultButtons() {
        hideButton(applyLoanButton); hideButton(viewLoansButton);
        hideButton(viewPaymentsButton); hideButton(pendingApplicationsButton);
    }

    private void hideButton(Button button) {
        if (button != null) { button.setVisible(false); button.setManaged(false); }
    }

    private void showButton(Button button) { button.setVisible(true); button.setManaged(true); }

    private boolean hasRole(String role) {
        return currentUser != null && role.equalsIgnoreCase(currentUser.getRole());
    }

    private void loadDashboardData() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            if (hasRole("CUSTOMER")) loadCustomerMetrics(connection);
            else if (hasRole("LOAN_OFFICER")) setMetricValues(
                    queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION WHERE STATUS = 'PENDING'"),
                    queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION WHERE STATUS = 'APPROVED'"),
                    queryCount(connection, "SELECT COUNT(*) FROM LOAN WHERE STATUS = 'ACTIVE'"),
                    queryCount(connection, "SELECT COUNT(*) FROM PAYMENT WHERE PAYMENT_STATUS = 'PAID'"));
            else if (hasRole("ADMIN")) setMetricValues(
                    queryCount(connection, "SELECT COUNT(*) FROM LMS_CUSTOMER"),
                    queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION"),
                    queryCount(connection, "SELECT COUNT(*) FROM LOAN WHERE STATUS = 'ACTIVE'"),
                    queryCount(connection, "SELECT COUNT(*) FROM PAYMENT"));
            loadRecentActivity(connection);
        } catch (SQLException exception) {
            setMetricValues(0, 0, 0, 0);
            showEmptyActivity("Dashboard data is temporarily unavailable.");
        }
    }

    private void loadCustomerMetrics(Connection connection) throws SQLException {
        int applications = queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = la.CUSTOMER_ID WHERE c.USER_ID = ?");
        int loans = queryCount(connection, "SELECT COUNT(*) FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND l.STATUS = 'ACTIVE'");
        int payments = queryCount(connection, "SELECT COUNT(*) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID = p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND p.PAYMENT_STATUS = 'PAID'");
        double paid = queryAmount(connection, "SELECT NVL(SUM(p.AMOUNT), 0) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID = p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND p.PAYMENT_STATUS = 'PAID'");
        customersValueLabel.setText(String.valueOf(applications));
        applicationsValueLabel.setText(String.valueOf(loans));
        loansValueLabel.setText(String.valueOf(payments));
        paymentsValueLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(paid));
    }

    private int queryCount(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sql.contains("c.USER_ID = ?")) statement.setInt(1, currentUser.getUserId());
            try (ResultSet results = statement.executeQuery()) { return results.next() ? results.getInt(1) : 0; }
        }
    }

    private double queryAmount(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, currentUser.getUserId());
            try (ResultSet results = statement.executeQuery()) { return results.next() ? results.getDouble(1) : 0; }
        }
    }

    private void setMetricValues(int first, int second, int third, int fourth) {
        customersValueLabel.setText(String.valueOf(first)); applicationsValueLabel.setText(String.valueOf(second));
        loansValueLabel.setText(String.valueOf(third)); paymentsValueLabel.setText(String.valueOf(fourth));
    }

    private void loadRecentActivity(Connection connection) throws SQLException {
        activityContainer.getChildren().clear();
        String sql = hasRole("CUSTOMER")
                ? "SELECT message FROM (SELECT 'Application #' || la.APPLICATION_ID || ' is ' || la.STATUS || ' - ' || TO_CHAR(la.APPLICATION_DATE, 'DD Mon YYYY') AS message FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = la.CUSTOMER_ID WHERE c.USER_ID = ? ORDER BY la.APPLICATION_DATE DESC) WHERE ROWNUM <= 5"
                : "SELECT message FROM (SELECT 'Application #' || APPLICATION_ID || ' is ' || STATUS || ' - ' || TO_CHAR(APPLICATION_DATE, 'DD Mon YYYY') AS message FROM LOAN_APPLICATION ORDER BY APPLICATION_DATE DESC) WHERE ROWNUM <= 5";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (hasRole("CUSTOMER")) statement.setInt(1, currentUser.getUserId());
            try (ResultSet results = statement.executeQuery()) {
                boolean found = false;
                while (results.next()) {
                    found = true;
                    Label item = new Label(results.getString("message"));
                    item.setWrapText(true); item.setMaxWidth(Double.MAX_VALUE);
                    item.getStyleClass().add("activity-item"); activityContainer.getChildren().add(item);
                }
                if (!found) showEmptyActivity("No recent activity yet.");
            }
        }
    }

    private void showEmptyActivity(String message) {
        activityContainer.getChildren().clear();
        Label empty = new Label(message); empty.getStyleClass().add("empty-label");
        activityContainer.getChildren().add(empty);
    }

    @FXML private void refreshDashboard() { if (currentUser != null) loadDashboardData(); }
    @FXML private void applyLoan() { if (requireRole("CUSTOMER")) openLoanApplication(); }
    @FXML private void viewLoans() { if (requireRole("CUSTOMER")) showInformation("Coming next", "Your loan list will be available in the Loan Management module."); }
    @FXML private void viewPayments() { if (requireRole("CUSTOMER")) showInformation("Coming next", "Payment history will be available in the Payment Management module."); }

    @FXML
    private void openPendingApplications() {
        if (!requireRole("LOAN_OFFICER")) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/pending-applications.fxml"));
            Parent root = loader.load();
            loader.<PendingApplicationsController>getController().setCurrentUser(currentUser);
            replaceScene(root, "LoanFlow - Pending Applications");
        } catch (Exception exception) { showError("Navigation Error", "Unable to open pending applications."); }
    }

    private boolean requireRole(String role) {
        if (hasRole(role)) return true;
        showError("Access denied", "Your account is not authorised to access this feature.");
        return false;
    }

    private void openLoanApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/apply-loan.fxml"));
            Parent root = loader.load();
            loader.<LoanApplicationController>getController().setCurrentUser(currentUser);
            replaceScene(root, "LoanFlow - Apply for Loan");
        } catch (Exception exception) { showError("Navigation Error", "Unable to open the loan application page."); }
    }

    @FXML
    private void logout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = currentStage(); stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("LoanFlow - Login"); stage.setMaximized(false); stage.centerOnScreen();
        } catch (Exception exception) { showError("Logout Error", "Unable to return to the login page."); }
    }

    private void replaceScene(Parent root, String title) {
        Stage stage = currentStage(); stage.setScene(new Scene(root, 1400, 850));
        stage.setTitle(title); stage.setMaximized(true);
    }

    private Stage currentStage() { return (Stage) welcomeLabel.getScene().getWindow(); }
    private void showError(String title, String message) { showAlert(Alert.AlertType.ERROR, title, message); }
    private void showInformation(String title, String message) { showAlert(Alert.AlertType.INFORMATION, title, message); }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }
}
