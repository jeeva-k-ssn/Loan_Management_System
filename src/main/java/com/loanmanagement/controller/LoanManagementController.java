package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/** Displays role-scoped loans, payment history, and customer repayments. */
public class LoanManagementController {
    @FXML private Label titleLabel, descriptionLabel, selectedLoanLabel, balanceLabel;
    @FXML private TableView<LoanRow> loanTable;
    @FXML private TableColumn<LoanRow, Integer> loanIdColumn, applicationIdColumn, tenureColumn;
    @FXML private TableColumn<LoanRow, String> customerColumn, startColumn, statusColumn;
    @FXML private TableColumn<LoanRow, Double> amountColumn, interestColumn, emiColumn;
    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow, Integer> paymentIdColumn;
    @FXML private TableColumn<PaymentRow, String> paymentDateColumn, paymentMethodColumn, paymentReferenceColumn, paymentStatusColumn;
    @FXML private TableColumn<PaymentRow, Double> paymentAmountColumn;
    @FXML private TextField paymentAmountField, paymentReferenceField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private Button recordPaymentButton;
    private User currentUser;

    @FXML public void initialize() {
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("loanId")); applicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("applicationId")); customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName")); amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); interestColumn.setCellValueFactory(new PropertyValueFactory<>("interest")); tenureColumn.setCellValueFactory(new PropertyValueFactory<>("tenure")); emiColumn.setCellValueFactory(new PropertyValueFactory<>("emi")); startColumn.setCellValueFactory(new PropertyValueFactory<>("startDate")); statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("paymentId")); paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate")); paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("method")); paymentReferenceColumn.setCellValueFactory(new PropertyValueFactory<>("reference")); paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentMethodCombo.setItems(FXCollections.observableArrayList("UPI", "Bank Transfer", "Card", "Cash")); loanTable.getSelectionModel().selectedItemProperty().addListener((obs, oldLoan, loan) -> selectLoan(loan)); recordPaymentButton.setDisable(true);
    }

    public void setCurrentUser(User user) {
        currentUser = user; boolean customer = hasRole("CUSTOMER");
        titleLabel.setText(customer ? "My Loans & Repayments" : "Loan Portfolio"); descriptionLabel.setText(customer ? "View your loans, payment history, and record repayments." : "Monitor active and closed customer loans and repayment history.");
        paymentAmountField.setVisible(customer); paymentAmountField.setManaged(customer); paymentMethodCombo.setVisible(customer); paymentMethodCombo.setManaged(customer); paymentReferenceField.setVisible(customer); paymentReferenceField.setManaged(customer); recordPaymentButton.setVisible(customer); recordPaymentButton.setManaged(customer); loadLoans();
    }

    private boolean hasRole(String role) { return currentUser != null && role.equalsIgnoreCase(currentUser.getRole()); }
    private void loadLoans() {
        String sql = "SELECT l.LOAN_ID,l.APPLICATION_ID,c.FULL_NAME,l.LOAN_AMOUNT,l.INTEREST_RATE,l.TENURE_MONTHS,l.EMI_AMOUNT,TO_CHAR(l.START_DATE,'DD Mon YYYY') START_DATE,l.STATUS FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID" + (hasRole("CUSTOMER") ? " WHERE c.USER_ID=?" : "") + " ORDER BY l.LOAN_ID DESC";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (hasRole("CUSTOMER")) statement.setInt(1, currentUser.getUserId());
            try (ResultSet rs = statement.executeQuery()) { var rows = FXCollections.<LoanRow>observableArrayList(); while (rs.next()) rows.add(new LoanRow(rs.getInt("LOAN_ID"), rs.getInt("APPLICATION_ID"), rs.getString("FULL_NAME"), rs.getDouble("LOAN_AMOUNT"), rs.getDouble("INTEREST_RATE"), rs.getInt("TENURE_MONTHS"), rs.getDouble("EMI_AMOUNT"), rs.getString("START_DATE"), rs.getString("STATUS"))); loanTable.setItems(rows); }
        } catch (SQLException e) { showError("Loans unavailable", "Unable to load loan data right now."); }
    }
    private void selectLoan(LoanRow loan) {
        paymentTable.getItems().clear(); selectedLoanLabel.setText("No loan selected"); balanceLabel.setText("-"); recordPaymentButton.setDisable(true); if (loan == null) return;
        selectedLoanLabel.setText("Loan #" + loan.loanId + " · " + loan.status); loadPayments(loan.loanId, loan.totalRepayable()); recordPaymentButton.setDisable(!hasRole("CUSTOMER") || !"ACTIVE".equalsIgnoreCase(loan.status));
    }
    private void loadPayments(int loanId, BigDecimal totalRepayable) {
        String payments = "SELECT PAYMENT_ID,TO_CHAR(PAYMENT_DATE,'DD Mon YYYY') PAYMENT_DATE,AMOUNT,NVL(PAYMENT_METHOD,'-') PAYMENT_METHOD,NVL(PAYMENT_REFERENCE,'-') PAYMENT_REFERENCE,PAYMENT_STATUS FROM PAYMENT WHERE LOAN_ID=? ORDER BY PAYMENT_DATE DESC,PAYMENT_ID DESC";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement ps = connection.prepareStatement(payments)) {
            ps.setInt(1, loanId); var rows = FXCollections.<PaymentRow>observableArrayList();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(new PaymentRow(rs.getInt("PAYMENT_ID"), rs.getString("PAYMENT_DATE"), rs.getDouble("AMOUNT"), rs.getString("PAYMENT_METHOD"), rs.getString("PAYMENT_REFERENCE"), rs.getString("PAYMENT_STATUS"))); }
            paymentTable.setItems(rows); balanceLabel.setText("Outstanding balance: " + money(outstandingBalance(connection, loanId, totalRepayable)));
        } catch (SQLException e) { showError("Payments unavailable", "Unable to load payment history."); }
    }
    @FXML private void recordPayment() {
        if (!hasRole("CUSTOMER")) { showError("Access denied", "Only customers can record repayments."); return; }
        LoanRow loan = loanTable.getSelectionModel().getSelectedItem(); if (loan == null || !"ACTIVE".equalsIgnoreCase(loan.status)) { showError("Select an active loan", "Select one of your active loans first."); return; }
        BigDecimal amount; try { amount = new BigDecimal(paymentAmountField.getText().trim()); } catch (NumberFormatException e) { showError("Invalid amount", "Enter a valid positive payment amount."); return; }
        if (amount.compareTo(BigDecimal.ZERO) <= 0 || paymentMethodCombo.getValue() == null) { showError("Payment details required", "Enter a positive amount and select a payment method."); return; }
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false); BigDecimal outstanding = outstandingBalance(connection, loan.loanId, loan.totalRepayable());
            if (amount.compareTo(outstanding) > 0) { connection.rollback(); showError("Payment exceeds balance", "The payment cannot exceed the outstanding balance of " + money(outstanding) + "."); return; }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO PAYMENT (LOAN_ID,PAYMENT_DATE,AMOUNT,PAYMENT_STATUS,PAYMENT_METHOD,PAYMENT_REFERENCE) VALUES (?,SYSDATE,?,'PAID',?,?)")) { ps.setInt(1, loan.loanId); ps.setBigDecimal(2, amount); ps.setString(3, paymentMethodCombo.getValue()); ps.setString(4, paymentReferenceField.getText().trim()); ps.executeUpdate(); }
            closeIfRepaid(connection, loan.loanId, loan.totalRepayable()); connection.commit(); paymentAmountField.clear(); paymentReferenceField.clear(); paymentMethodCombo.setValue(null); loadLoans(); loanTable.getItems().stream().filter(item -> item.loanId == loan.loanId).findFirst().ifPresent(item -> loanTable.getSelectionModel().select(item)); showInfo("Payment recorded", "Your payment was recorded successfully.");
        } catch (SQLException e) { showError("Payment failed", "Unable to record the payment. No payment was saved."); }
    }
    private BigDecimal outstandingBalance(Connection connection, int loanId, BigDecimal totalRepayable) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT NVL(SUM(CASE WHEN PAYMENT_STATUS='PAID' THEN AMOUNT ELSE 0 END),0) FROM PAYMENT WHERE LOAN_ID=?")) { ps.setInt(1, loanId); try (ResultSet rs = ps.executeQuery()) { rs.next(); BigDecimal paid = rs.getBigDecimal(1); return totalRepayable.subtract(paid == null ? BigDecimal.ZERO : paid).max(BigDecimal.ZERO); } }
    }
    private void closeIfRepaid(Connection connection, int loanId, BigDecimal totalRepayable) throws SQLException {
        if (outstandingBalance(connection, loanId, totalRepayable).compareTo(BigDecimal.ZERO) == 0) try (PreparedStatement close = connection.prepareStatement("UPDATE LOAN SET STATUS='CLOSED', CLOSED_DATE=SYSDATE WHERE LOAN_ID=? AND STATUS='ACTIVE'")) { close.setInt(1, loanId); close.executeUpdate(); }
    }
    @FXML private void refresh() { loadLoans(); }
    @FXML private void back() { try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml")); Parent root = loader.load(); loader.<DashboardController>getController().setCurrentUser(currentUser); Stage stage = (Stage) loanTable.getScene().getWindow(); stage.setScene(new Scene(root, 1400, 850)); stage.setTitle("LoanFlow - Dashboard"); stage.setMaximized(true); } catch (Exception e) { showError("Navigation error", "Unable to return to the dashboard."); } }
    private String money(BigDecimal value) { return "₹" + value.setScale(2, RoundingMode.HALF_UP); }
    private void showError(String title, String message) { alert(Alert.AlertType.ERROR, title, message); } private void showInfo(String title, String message) { alert(Alert.AlertType.INFORMATION, title, message); } private void alert(Alert.AlertType type, String title, String message) { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait(); }
    public static class LoanRow { private final int loanId, applicationId, tenure; private final String customerName, startDate, status; private final double amount, interest, emi; LoanRow(int loanId,int applicationId,String customerName,double amount,double interest,int tenure,double emi,String startDate,String status){this.loanId=loanId;this.applicationId=applicationId;this.customerName=customerName;this.amount=amount;this.interest=interest;this.tenure=tenure;this.emi=emi;this.startDate=startDate;this.status=status;} public int getLoanId(){return loanId;} public int getApplicationId(){return applicationId;} public String getCustomerName(){return customerName;} public double getAmount(){return amount;} public double getInterest(){return interest;} public int getTenure(){return tenure;} public double getEmi(){return emi;} public String getStartDate(){return startDate;} public String getStatus(){return status;} public BigDecimal totalRepayable(){return BigDecimal.valueOf(emi).multiply(BigDecimal.valueOf(tenure)).setScale(2,RoundingMode.HALF_UP);} }
    public static class PaymentRow { private final int paymentId; private final String paymentDate, method, reference, status; private final double amount; PaymentRow(int paymentId,String paymentDate,double amount,String method,String reference,String status){this.paymentId=paymentId;this.paymentDate=paymentDate;this.amount=amount;this.method=method;this.reference=reference;this.status=status;} public int getPaymentId(){return paymentId;} public String getPaymentDate(){return paymentDate;} public double getAmount(){return amount;} public String getMethod(){return method;} public String getReference(){return reference;} public String getStatus(){return status;} }
}
