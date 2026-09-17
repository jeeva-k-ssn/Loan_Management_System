package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.service.NotificationService;
import com.loanmanagement.model.User;
import com.loanmanagement.navigation.NavigationManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/** Displays role-scoped loans, payment history, and customer repayments. */
public class LoanManagementController {
    @FXML private Label titleLabel, descriptionLabel, selectedLoanLabel, balanceLabel;
    @FXML private Label originalAmountLabel, totalRepayableLabel, totalPaidLabel, outstandingLabel, emiLabel, paymentsMadeLabel, remainingAfterPaymentLabel;
    @FXML private TableView<LoanRow> loanTable;
    @FXML private TableView<ApplicationRow> applicationTable;
    @FXML private TableColumn<ApplicationRow, Integer> applicationIdColumn, applicationTenureColumn;
    @FXML private TableColumn<ApplicationRow, String> applicationTypeColumn, applicationPurposeColumn, applicationDateColumn, applicationStatusColumn;
    @FXML private TableColumn<ApplicationRow, Double> applicationAmountColumn, applicationInterestColumn, applicationEmiColumn;
    @FXML private TableColumn<LoanRow, Integer> loanIdColumn, loanApplicationIdColumn, tenureColumn;
    @FXML private TableColumn<LoanRow, String> customerColumn, startColumn, statusColumn;
    @FXML private TableColumn<LoanRow, Double> amountColumn, interestColumn, emiColumn, totalRepayableColumn, totalPaidColumn, outstandingColumn;
    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow, Integer> paymentIdColumn;
    @FXML private TableColumn<PaymentRow, String> paymentDateColumn, paymentMethodColumn, paymentReferenceColumn, paymentStatusColumn;
    @FXML private TableColumn<PaymentRow, Double> paymentAmountColumn;
    @FXML private TextField paymentAmountField, paymentReferenceField;
    @FXML private TextField loanSearchField, paymentSearchField;
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private ComboBox<String> loanFilterCombo, paymentFilterCombo;
    @FXML private Button recordPaymentButton;
    @FXML private Button statementButton;
    private User currentUser;
    private BigDecimal selectedOutstanding = BigDecimal.ZERO;
    private ObservableList<LoanRow> allLoans = FXCollections.observableArrayList();
    private ObservableList<PaymentRow> allPayments = FXCollections.observableArrayList();

    @FXML public void initialize() {
        applicationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loanTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        loanIdColumn.setCellValueFactory(new PropertyValueFactory<>("loanId")); loanApplicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("applicationId")); customerColumn.setCellValueFactory(new PropertyValueFactory<>("customerName")); amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); interestColumn.setCellValueFactory(new PropertyValueFactory<>("interest")); tenureColumn.setCellValueFactory(new PropertyValueFactory<>("tenure")); emiColumn.setCellValueFactory(new PropertyValueFactory<>("emi")); totalRepayableColumn.setCellValueFactory(new PropertyValueFactory<>("totalRepayable")); totalPaidColumn.setCellValueFactory(new PropertyValueFactory<>("totalPaid")); outstandingColumn.setCellValueFactory(new PropertyValueFactory<>("outstanding")); startColumn.setCellValueFactory(new PropertyValueFactory<>("startDate")); statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("paymentId")); paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("paymentDate")); paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("method")); paymentReferenceColumn.setCellValueFactory(new PropertyValueFactory<>("reference")); paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentMethodCombo.setItems(FXCollections.observableArrayList("UPI", "Bank Transfer", "Card", "Cash"));
        loanTable.getSelectionModel().selectedItemProperty().addListener((obs, oldLoan, loan) -> selectLoan(loan));
        paymentAmountField.textProperty().addListener((obs, oldValue, newValue) -> refreshPaymentPreview());
        loanSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyLoanFilter());
        paymentSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyPaymentFilter());
        loanFilterCombo.setItems(FXCollections.observableArrayList("All Loans", "Active Loans", "Closed Loans"));
        loanFilterCombo.getSelectionModel().selectFirst();
        loanFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyLoanFilter());
        paymentFilterCombo.setItems(FXCollections.observableArrayList("All Payments", "PAID", "PENDING", "FAILED"));
        paymentFilterCombo.getSelectionModel().selectFirst();
        paymentFilterCombo.valueProperty().addListener((obs, oldValue, newValue) -> applyPaymentFilter());
        recordPaymentButton.setDisable(true); statementButton.setDisable(true);
        applicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("applicationId")); applicationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("loanType")); applicationAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); applicationPurposeColumn.setCellValueFactory(new PropertyValueFactory<>("purpose")); applicationTenureColumn.setCellValueFactory(new PropertyValueFactory<>("tenure")); applicationInterestColumn.setCellValueFactory(new PropertyValueFactory<>("interest")); applicationEmiColumn.setCellValueFactory(new PropertyValueFactory<>("emi")); applicationDateColumn.setCellValueFactory(new PropertyValueFactory<>("applicationDate")); applicationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        amountColumn.setCellFactory(column -> currencyCell());
        emiColumn.setCellFactory(column -> currencyCell());
        totalRepayableColumn.setCellFactory(column -> currencyCell()); totalPaidColumn.setCellFactory(column -> currencyCell()); outstandingColumn.setCellFactory(column -> currencyCell());
        paymentAmountColumn.setCellFactory(column -> currencyCell());
        applicationAmountColumn.setCellFactory(column -> currencyCell());
        applicationEmiColumn.setCellFactory(column -> currencyCell());
        statusColumn.setCellFactory(column -> statusCell());
        paymentStatusColumn.setCellFactory(column -> statusCell());
        applicationStatusColumn.setCellFactory(column -> statusCell());
    }

    private <T> TableCell<T, Double> currencyCell() {
        return new TableCell<T, Double>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value));
            }
        };
    }
    private <T> TableCell<T, String> statusCell() {
        return new TableCell<T, String>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty); getStyleClass().removeIf(style -> style.startsWith("status-"));
                if (empty || value == null) { setText(null); return; }
                String normalized = value.toLowerCase().replace('_', '-'); setText(value.toUpperCase()); getStyleClass().add("status-badge"); getStyleClass().add("status-" + normalized);
            }
        };
    }

    public void setCurrentUser(User user) {
        currentUser = user; boolean customer = hasRole("CUSTOMER");
        titleLabel.setText(customer ? "My Loans & Repayments" : "Loan Portfolio"); descriptionLabel.setText(customer ? "View your loans, payment history, and record repayments." : "Monitor active and closed customer loans and repayment history.");
        applicationTable.setVisible(customer); applicationTable.setManaged(customer);
        paymentAmountField.setVisible(customer); paymentAmountField.setManaged(customer); paymentMethodCombo.setVisible(customer); paymentMethodCombo.setManaged(customer); paymentReferenceField.setVisible(customer); paymentReferenceField.setManaged(customer); recordPaymentButton.setVisible(customer); recordPaymentButton.setManaged(customer); if (customer) loadApplications(); loadLoans();
    }

    private boolean hasRole(String role) { return currentUser != null && role.equalsIgnoreCase(currentUser.getRole()); }
    private void loadLoans() {
        String sql = "SELECT l.LOAN_ID,l.APPLICATION_ID,c.FULL_NAME,l.LOAN_AMOUNT,l.INTEREST_RATE,l.TENURE_MONTHS,l.EMI_AMOUNT,l.EMI_AMOUNT*l.TENURE_MONTHS TOTAL_REPAYABLE,NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p WHERE p.LOAN_ID=l.LOAN_ID AND p.PAYMENT_STATUS='PAID'),0) TOTAL_PAID,GREATEST(l.EMI_AMOUNT*l.TENURE_MONTHS-NVL((SELECT SUM(p2.AMOUNT) FROM PAYMENT p2 WHERE p2.LOAN_ID=l.LOAN_ID AND p2.PAYMENT_STATUS='PAID'),0),0) OUTSTANDING,TO_CHAR(l.START_DATE,'DD Mon YYYY') START_DATE,l.STATUS FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID" + (hasRole("CUSTOMER") ? " WHERE c.USER_ID=?" : "") + " ORDER BY l.LOAN_ID DESC";
        Task<ObservableList<LoanRow>> task = new Task<>() {
            @Override protected ObservableList<LoanRow> call() throws SQLException {
                try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
                    if (hasRole("CUSTOMER")) statement.setInt(1, currentUser.getUserId());
                    try (ResultSet rs = statement.executeQuery()) { var rows=FXCollections.<LoanRow>observableArrayList(); while(rs.next()) rows.add(new LoanRow(rs.getInt("LOAN_ID"),rs.getInt("APPLICATION_ID"),rs.getString("FULL_NAME"),rs.getDouble("LOAN_AMOUNT"),rs.getDouble("INTEREST_RATE"),rs.getInt("TENURE_MONTHS"),rs.getDouble("EMI_AMOUNT"),rs.getDouble("TOTAL_REPAYABLE"),rs.getDouble("TOTAL_PAID"),rs.getDouble("OUTSTANDING"),rs.getString("START_DATE"),rs.getString("STATUS"))); return rows; }
                }
            }
        };
        task.setOnSucceeded(event -> { allLoans=task.getValue(); applyLoanFilter(); if(!allLoans.isEmpty()) loanTable.getSelectionModel().selectFirst(); else { paymentTable.getItems().clear(); selectedLoanLabel.setText("No loan selected"); balanceLabel.setText("-"); clearRepaymentSummary(); } });
        task.setOnFailed(event -> showError("Loans unavailable", "Unable to load loan data right now."));
        Thread thread=new Thread(task,"loanflow-loans-load"); thread.setDaemon(true); thread.start();
    }
    private void loadApplications() {
        String sql = "SELECT la.APPLICATION_ID,la.LOAN_TYPE,la.LOAN_AMOUNT,la.LOAN_PURPOSE,la.TENURE_MONTHS,la.INTEREST_RATE,la.EMI_AMOUNT,TO_CHAR(la.APPLICATION_DATE,'DD Mon YYYY') APPLICATION_DATE,la.STATUS FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=la.CUSTOMER_ID WHERE c.USER_ID=? ORDER BY la.APPLICATION_DATE DESC,la.APPLICATION_ID DESC";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, currentUser.getUserId());
            try (ResultSet rs = statement.executeQuery()) { var rows = FXCollections.<ApplicationRow>observableArrayList(); while (rs.next()) rows.add(new ApplicationRow(rs.getInt("APPLICATION_ID"), rs.getString("LOAN_TYPE"), rs.getDouble("LOAN_AMOUNT"), rs.getString("LOAN_PURPOSE"), rs.getInt("TENURE_MONTHS"), rs.getDouble("INTEREST_RATE"), rs.getDouble("EMI_AMOUNT"), rs.getString("APPLICATION_DATE"), rs.getString("STATUS"))); applicationTable.setItems(rows); }
        } catch (SQLException e) { showError("Applications unavailable", "Unable to load your application history."); }
    }
    private void selectLoan(LoanRow loan) {
        paymentTable.getItems().clear(); selectedLoanLabel.setText("No loan selected"); balanceLabel.setText("-"); clearRepaymentSummary(); recordPaymentButton.setDisable(true); statementButton.setDisable(loan == null); if (loan == null) return;
        selectedLoanLabel.setText("Loan #" + loan.loanId + " · " + loan.status); loadPayments(loan.loanId, loan.totalRepayable()); recordPaymentButton.setDisable(!hasRole("CUSTOMER") || !"ACTIVE".equalsIgnoreCase(loan.status));
    }
    private void loadPayments(int loanId, BigDecimal totalRepayable) {
        String payments = "SELECT PAYMENT_ID,TO_CHAR(PAYMENT_DATE,'DD Mon YYYY') PAYMENT_DATE,AMOUNT,NVL(PAYMENT_METHOD,'-') PAYMENT_METHOD,NVL(PAYMENT_REFERENCE,'-') PAYMENT_REFERENCE,PAYMENT_STATUS FROM PAYMENT WHERE LOAN_ID=? ORDER BY PAYMENT_DATE DESC,PAYMENT_ID DESC";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement ps = connection.prepareStatement(payments)) {
            ps.setInt(1, loanId); var rows = FXCollections.<PaymentRow>observableArrayList();
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(new PaymentRow(rs.getInt("PAYMENT_ID"), rs.getString("PAYMENT_DATE"), rs.getDouble("AMOUNT"), rs.getString("PAYMENT_METHOD"), rs.getString("PAYMENT_REFERENCE"), rs.getString("PAYMENT_STATUS"))); }
            allPayments = rows;
            applyPaymentFilter();
            BigDecimal paid = rows.stream().filter(row -> "PAID".equalsIgnoreCase(row.status)).map(row -> BigDecimal.valueOf(row.amount)).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = totalRepayable.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            originalAmountLabel.setText(money(findSelectedLoanAmount()));
            totalRepayableLabel.setText(money(totalRepayable));
            totalPaidLabel.setText(money(paid));
            outstandingLabel.setText(money(outstanding));
            emiLabel.setText(money(findSelectedLoanEmi()));
            paymentsMadeLabel.setText(String.valueOf(rows.stream().filter(row -> "PAID".equalsIgnoreCase(row.status)).count()));
            selectedOutstanding = outstanding;
            balanceLabel.setText("Outstanding balance: " + money(outstanding));
            remainingAfterPaymentLabel.setText("Enter an amount to preview your remaining balance.");
        } catch (SQLException e) { showError("Payments unavailable", "Unable to load payment history."); }
    }
    private void applyLoanFilter() {
        if (loanTable == null) return;
        String search = loanSearchField == null || loanSearchField.getText() == null ? "" : loanSearchField.getText().trim().toLowerCase();
        String filter = loanFilterCombo == null || loanFilterCombo.getValue() == null ? "All Loans" : loanFilterCombo.getValue();
        FilteredList<LoanRow> filtered = new FilteredList<>(allLoans, row -> {
            boolean statusMatches = "All Loans".equals(filter) || row.status.equalsIgnoreCase(filter.replace(" Loans", ""));
            boolean searchMatches = search.isEmpty() || String.valueOf(row.loanId).contains(search) || String.valueOf(row.applicationId).contains(search) || row.customerName.toLowerCase().contains(search);
            return statusMatches && searchMatches;
        });
        loanTable.setItems(filtered);
    }
    private void applyPaymentFilter() {
        if (paymentTable == null) return;
        String search = paymentSearchField == null || paymentSearchField.getText() == null ? "" : paymentSearchField.getText().trim().toLowerCase();
        String filter = paymentFilterCombo == null || paymentFilterCombo.getValue() == null ? "All Payments" : paymentFilterCombo.getValue();
        FilteredList<PaymentRow> filtered = new FilteredList<>(allPayments, row -> {
            boolean statusMatches = "All Payments".equals(filter) || row.status.equalsIgnoreCase(filter);
            boolean searchMatches = search.isEmpty() || String.valueOf(row.paymentId).contains(search) || row.reference.toLowerCase().contains(search) || row.method.toLowerCase().contains(search);
            return statusMatches && searchMatches;
        });
        paymentTable.setItems(filtered);
    }
    @FXML private void recordPayment() {
        if (!hasRole("CUSTOMER")) { showError("Access denied", "Only customers can record repayments."); return; }
        LoanRow loan = loanTable.getSelectionModel().getSelectedItem(); if (loan == null || !"ACTIVE".equalsIgnoreCase(loan.status)) { showError("Select an active loan", "Select one of your active loans first."); return; }
        if (paymentAmountField.getText() == null || paymentAmountField.getText().trim().isEmpty()) { showError("Payment amount required", "Payment amount must be greater than ₹0."); return; }
        BigDecimal amount; try { amount = new BigDecimal(paymentAmountField.getText().trim()); } catch (NumberFormatException e) { showError("Invalid amount", "Enter a valid payment amount."); return; }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) { showError("Invalid amount", "Payment amount must be greater than ₹0."); return; }
        if (paymentMethodCombo.getValue() == null) { showError("Payment method required", "Select a payment method before continuing."); return; }
        Task<String> paymentTask = new Task<>() { @Override protected String call() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false); BigDecimal outstanding = outstandingBalance(connection, loan.loanId, loan.totalRepayable());
            if (amount.compareTo(outstanding) > 0) { connection.rollback(); throw new SQLException("Payment exceeds the outstanding balance of " + money(outstanding) + "."); }
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO PAYMENT (LOAN_ID,PAYMENT_DATE,AMOUNT,PAYMENT_STATUS,PAYMENT_METHOD,PAYMENT_REFERENCE) VALUES (?,SYSDATE,?,'PAID',?,?)")) { ps.setInt(1, loan.loanId); ps.setBigDecimal(2, amount); ps.setString(3, paymentMethodCombo.getValue()); ps.setString(4, paymentReferenceField.getText().trim()); ps.executeUpdate(); }
            BigDecimal remainingAfterPayment = outstanding.subtract(amount).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            boolean closesLoan = remainingAfterPayment.compareTo(BigDecimal.ZERO) == 0;
            closeIfRepaid(connection, loan.loanId, loan.totalRepayable()); connection.commit();
            try (PreparedStatement notificationStatement = connection.prepareStatement("SELECT c.USER_ID,MAX(p.PAYMENT_ID) PAYMENT_ID FROM LMS_CUSTOMER c JOIN LOAN l ON l.CUSTOMER_ID=c.CUSTOMER_ID JOIN PAYMENT p ON p.LOAN_ID=l.LOAN_ID WHERE l.LOAN_ID=? GROUP BY c.USER_ID")) {
                notificationStatement.setInt(1, loan.loanId);
                try (ResultSet result = notificationStatement.executeQuery()) {
                    if (result.next()) {
                        int paymentId = result.getInt("PAYMENT_ID");
                        NotificationService.create(result.getInt("USER_ID"), "Payment successful", "Payment #" + paymentId + " of " + money(amount) + " was applied to Loan #" + loan.loanId + ". Remaining balance: " + money(remainingAfterPayment) + ".", "PAYMENT_SUCCESS", paymentId);
                        if (closesLoan) NotificationService.create(result.getInt("USER_ID"), "Loan closed", "Congratulations! Loan #" + loan.loanId + " has been fully repaid and closed.", "LOAN_CLOSED", loan.loanId);
                    }
                }
            }
            return "Payment recorded";
        }}};
        recordPaymentButton.setDisable(true);
        paymentTask.setOnSucceeded(e->{recordPaymentButton.setDisable(false);paymentAmountField.clear();paymentReferenceField.clear();paymentMethodCombo.setValue(null);loadLoans();showInfo("Payment recorded","Your payment was recorded successfully for Loan #"+loan.loanId+".");});
        paymentTask.setOnFailed(e->{recordPaymentButton.setDisable(false);showError("Payment failed","Unable to record the payment. No payment was saved.");});
        Thread thread=new Thread(paymentTask,"loanflow-record-payment");thread.setDaemon(true);thread.start();
    }
    private BigDecimal outstandingBalance(Connection connection, int loanId, BigDecimal totalRepayable) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT NVL(SUM(CASE WHEN PAYMENT_STATUS='PAID' THEN AMOUNT ELSE 0 END),0) FROM PAYMENT WHERE LOAN_ID=?")) { ps.setInt(1, loanId); try (ResultSet rs = ps.executeQuery()) { rs.next(); BigDecimal paid = rs.getBigDecimal(1); return totalRepayable.subtract(paid == null ? BigDecimal.ZERO : paid).max(BigDecimal.ZERO); } }
    }
    private void closeIfRepaid(Connection connection, int loanId, BigDecimal totalRepayable) throws SQLException {
        if (outstandingBalance(connection, loanId, totalRepayable).compareTo(BigDecimal.ZERO) == 0) try (PreparedStatement close = connection.prepareStatement("UPDATE LOAN SET STATUS='CLOSED', CLOSED_DATE=SYSDATE WHERE LOAN_ID=? AND STATUS='ACTIVE'")) { close.setInt(1, loanId); close.executeUpdate(); }
    }
    @FXML private void refresh() { if (hasRole("CUSTOMER")) loadApplications(); loadLoans(); }
    @FXML private void openStatement() {
        LoanRow loan = loanTable.getSelectionModel().getSelectedItem();
        if (loan == null) { showError("Select a loan", "Select a loan before opening its statement."); return; }
        try { NavigationManager.navigate((Stage) loanTable.getScene().getWindow(), "/fxml/loan-statement.fxml", "LoanFlow - Loan Statement", c -> ((LoanStatementController)c).setLoan(currentUser, loan.loanId)); }
        catch (Exception e) { showError("Statement unavailable", "Unable to open the loan statement."); }
    }
    @FXML private void back() { try { NavigationManager.navigate((Stage) loanTable.getScene().getWindow(), "/fxml/dashboard.fxml", "LoanFlow - Dashboard", c -> ((DashboardController)c).setCurrentUser(currentUser)); } catch (Exception e) { showError("Navigation error", "Unable to return to the dashboard."); } }
    private void refreshPaymentPreview() {
        LoanRow loan = loanTable == null ? null : loanTable.getSelectionModel().getSelectedItem();
        if (loan == null || remainingAfterPaymentLabel == null) return;
        try {
            BigDecimal amount = new BigDecimal(paymentAmountField.getText().trim());
            BigDecimal outstanding = selectedOutstanding;
            if (amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(outstanding) <= 0) {
                remainingAfterPaymentLabel.setText("Remaining after payment: " + money(outstanding.subtract(amount)));
            } else if (amount.compareTo(outstanding) > 0) {
                remainingAfterPaymentLabel.setText("Payment cannot exceed " + money(outstanding));
            } else {
                remainingAfterPaymentLabel.setText("Enter an amount greater than ₹0 to preview.");
            }
        } catch (NumberFormatException e) { remainingAfterPaymentLabel.setText("Enter a valid amount to preview."); }
    }
    private BigDecimal findSelectedLoanAmount() { LoanRow loan = loanTable.getSelectionModel().getSelectedItem(); return loan == null ? BigDecimal.ZERO : BigDecimal.valueOf(loan.amount); }
    private BigDecimal findSelectedLoanEmi() { LoanRow loan = loanTable.getSelectionModel().getSelectedItem(); return loan == null ? BigDecimal.ZERO : BigDecimal.valueOf(loan.emi); }
    private void clearRepaymentSummary() { selectedOutstanding = BigDecimal.ZERO; if (originalAmountLabel != null) { originalAmountLabel.setText("-"); totalRepayableLabel.setText("-"); totalPaidLabel.setText("-"); outstandingLabel.setText("-"); emiLabel.setText("-"); paymentsMadeLabel.setText("-"); remainingAfterPaymentLabel.setText("Select a loan to see repayment details."); } }
    private String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value.setScale(2, RoundingMode.HALF_UP)); }
    private void showError(String title, String message) { alert(Alert.AlertType.ERROR, title, message); } private void showInfo(String title, String message) { alert(Alert.AlertType.INFORMATION, title, message); } private void alert(Alert.AlertType type, String title, String message) { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait(); }
    public static class LoanRow { private final int loanId, applicationId, tenure; private final String customerName, startDate, status; private final double amount, interest, emi, totalRepayable, totalPaid, outstanding; LoanRow(int loanId,int applicationId,String customerName,double amount,double interest,int tenure,double emi,double totalRepayable,double totalPaid,double outstanding,String startDate,String status){this.loanId=loanId;this.applicationId=applicationId;this.customerName=customerName;this.amount=amount;this.interest=interest;this.tenure=tenure;this.emi=emi;this.totalRepayable=totalRepayable;this.totalPaid=totalPaid;this.outstanding=outstanding;this.startDate=startDate;this.status=status;} public int getLoanId(){return loanId;} public int getApplicationId(){return applicationId;} public String getCustomerName(){return customerName;} public double getAmount(){return amount;} public double getInterest(){return interest;} public int getTenure(){return tenure;} public double getEmi(){return emi;} public double getTotalRepayable(){return totalRepayable;} public double getTotalPaid(){return totalPaid;} public double getOutstanding(){return outstanding;} public String getStartDate(){return startDate;} public String getStatus(){return status;} public BigDecimal totalRepayable(){return BigDecimal.valueOf(totalRepayable).setScale(2,RoundingMode.HALF_UP);} }
    public static class ApplicationRow { private final int applicationId, tenure; private final String loanType, purpose, applicationDate, status; private final double amount, interest, emi; ApplicationRow(int applicationId,String loanType,double amount,String purpose,int tenure,double interest,double emi,String applicationDate,String status){this.applicationId=applicationId;this.loanType=loanType;this.amount=amount;this.purpose=purpose;this.tenure=tenure;this.interest=interest;this.emi=emi;this.applicationDate=applicationDate;this.status=status;} public int getApplicationId(){return applicationId;} public String getLoanType(){return loanType;} public double getAmount(){return amount;} public String getPurpose(){return purpose;} public int getTenure(){return tenure;} public double getInterest(){return interest;} public double getEmi(){return emi;} public String getApplicationDate(){return applicationDate;} public String getStatus(){return status;} }
    public static class PaymentRow { private final int paymentId; private final String paymentDate, method, reference, status; private final double amount; PaymentRow(int paymentId,String paymentDate,double amount,String method,String reference,String status){this.paymentId=paymentId;this.paymentDate=paymentDate;this.amount=amount;this.method=method;this.reference=reference;this.status=status;} public int getPaymentId(){return paymentId;} public String getPaymentDate(){return paymentDate;} public double getAmount(){return amount;} public String getMethod(){return method;} public String getReference(){return reference;} public String getStatus(){return status;} }
}
