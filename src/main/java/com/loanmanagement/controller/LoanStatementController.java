package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;

/** Print-friendly, role-scoped statement for one loan account. */
public class LoanStatementController {
    @FXML private Label customerLabel, customerIdLabel, emailLabel, loanIdLabel, applicationIdLabel, amountLabel, interestLabel, tenureLabel, emiLabel, startDateLabel, closedDateLabel, statusLabel, purposeLabel, applicationDateLabel, approvalStatusLabel, repayableLabel, paidLabel, outstandingLabel;
    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow,Integer> paymentIdColumn;
    @FXML private TableColumn<PaymentRow,String> dateColumn, methodColumn, referenceColumn, paymentStatusColumn;
    @FXML private TableColumn<PaymentRow,Double> paymentAmountColumn;
    private User currentUser;
    private int loanId;

    @FXML public void initialize() {
        paymentIdColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("id"));
        dateColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("date"));
        paymentAmountColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("amount"));
        methodColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("method"));
        referenceColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("reference"));
        paymentStatusColumn.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("status"));
        paymentAmountColumn.setCellFactory(column -> new TableCell<PaymentRow, Double>() {
            @Override protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty || value == null ? null : money(BigDecimal.valueOf(value)));
            }
        });
        paymentStatusColumn.setCellFactory(column -> new TableCell<PaymentRow, String>() {
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                getStyleClass().removeIf(style -> style.startsWith("status-"));
                if (empty || value == null) { setText(null); return; }
                setText(value.toUpperCase());
                getStyleClass().add("status-badge");
                getStyleClass().add("status-" + value.toLowerCase());
            }
        });
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
    public void setLoan(User user, int selectedLoanId) { currentUser = user; loanId = selectedLoanId; loadStatementAsync(); }
    private void loadStatementAsync() {
        Task<Runnable> task = new Task<>() {
            @Override protected Runnable call() throws SQLException {
                String sql = "SELECT l.LOAN_ID,l.APPLICATION_ID,l.LOAN_AMOUNT,l.INTEREST_RATE,l.TENURE_MONTHS,l.EMI_AMOUNT,TO_CHAR(l.START_DATE,'DD Mon YYYY') START_DATE,TO_CHAR(l.CLOSED_DATE,'DD Mon YYYY') CLOSED_DATE,l.STATUS,la.LOAN_PURPOSE,TO_CHAR(la.APPLICATION_DATE,'DD Mon YYYY') APPLICATION_DATE,la.STATUS APPLICATION_STATUS,c.CUSTOMER_ID,c.FULL_NAME,u.EMAIL FROM LOAN l JOIN LOAN_APPLICATION la ON la.APPLICATION_ID=l.APPLICATION_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID JOIN USERS u ON u.USER_ID=c.USER_ID WHERE l.LOAN_ID=?" + (currentUser != null && "CUSTOMER".equalsIgnoreCase(currentUser.getRole()) ? " AND c.USER_ID=?" : "");
                try (Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
                    p.setInt(1,loanId); if(currentUser!=null&&"CUSTOMER".equalsIgnoreCase(currentUser.getRole())) p.setInt(2,currentUser.getUserId());
                    try(ResultSet r=p.executeQuery()) { if(!r.next()) throw new SQLException("not found");
                        int id=r.getInt("LOAN_ID"), app=r.getInt("APPLICATION_ID"), cid=r.getInt("CUSTOMER_ID"), months=r.getInt("TENURE_MONTHS"); String customer=r.getString("FULL_NAME"), email=r.getString("EMAIL"), start=r.getString("START_DATE"), closed=r.getString("CLOSED_DATE"), status=r.getString("STATUS"), purpose=r.getString("LOAN_PURPOSE"), appDate=r.getString("APPLICATION_DATE"), appStatus=r.getString("APPLICATION_STATUS"); BigDecimal amount=r.getBigDecimal("LOAN_AMOUNT"), emi=r.getBigDecimal("EMI_AMOUNT"), repayable=emi.multiply(BigDecimal.valueOf(months)).setScale(2,RoundingMode.HALF_UP); double interest=r.getDouble("INTEREST_RATE");
                        var payments=javafx.collections.FXCollections.<PaymentRow>observableArrayList(); String q="SELECT PAYMENT_ID,TO_CHAR(PAYMENT_DATE,'DD Mon YYYY') PAYMENT_DATE,AMOUNT,NVL(PAYMENT_METHOD,'-') PAYMENT_METHOD,NVL(PAYMENT_REFERENCE,'-') PAYMENT_REFERENCE,PAYMENT_STATUS FROM PAYMENT WHERE LOAN_ID=? ORDER BY PAYMENT_DATE DESC,PAYMENT_ID DESC"; try(PreparedStatement ps=c.prepareStatement(q)){ps.setInt(1,loanId);try(ResultSet pr=ps.executeQuery()){while(pr.next())payments.add(new PaymentRow(pr.getInt(1),pr.getString(2),pr.getDouble(3),pr.getString(4),pr.getString(5),pr.getString(6)));}}
                        BigDecimal paid=payments.stream().filter(x->"PAID".equalsIgnoreCase(x.status)).map(x->BigDecimal.valueOf(x.amount)).reduce(BigDecimal.ZERO,BigDecimal::add); BigDecimal outstanding=repayable.subtract(paid).max(BigDecimal.ZERO);
                        return ()->{loanIdLabel.setText("#"+id);applicationIdLabel.setText("#"+app);customerLabel.setText(customer);customerIdLabel.setText(String.valueOf(cid));emailLabel.setText(email);amountLabel.setText(money(amount));interestLabel.setText(String.format(Locale.US,"%.2f%%",interest));tenureLabel.setText(months+" months");emiLabel.setText(money(emi));startDateLabel.setText(start);closedDateLabel.setText(closed==null?"-":closed);statusLabel.setText(status);purposeLabel.setText(purpose==null?"-":purpose);applicationDateLabel.setText(appDate==null?"-":appDate);approvalStatusLabel.setText(appStatus==null?"-":appStatus);paymentTable.setItems(payments);repayableLabel.setText(money(repayable));paidLabel.setText(money(paid));outstandingLabel.setText(money(outstanding));};
                    }
                }
            }
        };
        task.setOnSucceeded(e->task.getValue().run()); task.setOnFailed(e->showError("Statement unavailable","Unable to load the loan statement right now.")); Thread t=new Thread(task,"loanflow-statement-load");t.setDaemon(true);t.start();
    }
    private void loadStatement() {
        String sql = "SELECT l.LOAN_ID,l.APPLICATION_ID,l.LOAN_AMOUNT,l.INTEREST_RATE,l.TENURE_MONTHS,l.EMI_AMOUNT,TO_CHAR(l.START_DATE,'DD Mon YYYY') START_DATE,TO_CHAR(l.CLOSED_DATE,'DD Mon YYYY') CLOSED_DATE,l.STATUS,la.LOAN_PURPOSE,TO_CHAR(la.APPLICATION_DATE,'DD Mon YYYY') APPLICATION_DATE,la.STATUS APPLICATION_STATUS,c.CUSTOMER_ID,c.FULL_NAME,u.EMAIL FROM LOAN l JOIN LOAN_APPLICATION la ON la.APPLICATION_ID=l.APPLICATION_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID JOIN USERS u ON u.USER_ID=c.USER_ID WHERE l.LOAN_ID=?" + (currentUser != null && "CUSTOMER".equalsIgnoreCase(currentUser.getRole()) ? " AND c.USER_ID=?" : "");
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, loanId); if (currentUser != null && "CUSTOMER".equalsIgnoreCase(currentUser.getRole())) p.setInt(2, currentUser.getUserId());
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) { showError("Statement unavailable", "The selected loan could not be found."); return; }
                loanIdLabel.setText("#" + r.getInt("LOAN_ID")); applicationIdLabel.setText("#" + r.getInt("APPLICATION_ID")); customerLabel.setText(r.getString("FULL_NAME")); customerIdLabel.setText(String.valueOf(r.getInt("CUSTOMER_ID"))); emailLabel.setText(r.getString("EMAIL")); amountLabel.setText(money(r.getBigDecimal("LOAN_AMOUNT"))); interestLabel.setText(String.format(Locale.US,"%.2f%%",r.getDouble("INTEREST_RATE"))); tenureLabel.setText(r.getInt("TENURE_MONTHS") + " months"); emiLabel.setText(money(r.getBigDecimal("EMI_AMOUNT"))); startDateLabel.setText(r.getString("START_DATE")); closedDateLabel.setText(r.getString("CLOSED_DATE") == null ? "-" : r.getString("CLOSED_DATE")); String loanStatus=r.getString("STATUS"); statusLabel.setText(loanStatus); statusLabel.getStyleClass().removeIf(style -> style.startsWith("status-")); statusLabel.getStyleClass().add("status-badge"); statusLabel.getStyleClass().add("status-" + loanStatus.toLowerCase()); purposeLabel.setText(r.getString("LOAN_PURPOSE") == null ? "-" : r.getString("LOAN_PURPOSE")); applicationDateLabel.setText(r.getString("APPLICATION_DATE") == null ? "-" : r.getString("APPLICATION_DATE")); approvalStatusLabel.setText(r.getString("APPLICATION_STATUS") == null ? "-" : r.getString("APPLICATION_STATUS"));
                BigDecimal repayable = r.getBigDecimal("EMI_AMOUNT").multiply(BigDecimal.valueOf(r.getInt("TENURE_MONTHS"))).setScale(2,RoundingMode.HALF_UP); loadPayments(c, repayable);
            }
        } catch (SQLException e) { showError("Statement unavailable", "Unable to load the loan statement right now."); }
    }
    private void loadPayments(Connection c, BigDecimal repayable) throws SQLException {
        var rows = javafx.collections.FXCollections.<PaymentRow>observableArrayList();
        try (PreparedStatement p=c.prepareStatement("SELECT PAYMENT_ID,TO_CHAR(PAYMENT_DATE,'DD Mon YYYY') PAYMENT_DATE,AMOUNT,NVL(PAYMENT_METHOD,'-') PAYMENT_METHOD,NVL(PAYMENT_REFERENCE,'-') PAYMENT_REFERENCE,PAYMENT_STATUS FROM PAYMENT WHERE LOAN_ID=? ORDER BY PAYMENT_DATE DESC,PAYMENT_ID DESC")) { p.setInt(1,loanId); try(ResultSet r=p.executeQuery()){ while(r.next()) rows.add(new PaymentRow(r.getInt(1),r.getString(2),r.getDouble(3),r.getString(4),r.getString(5),r.getString(6))); } }
        paymentTable.setItems(rows); BigDecimal paid=rows.stream().filter(x->"PAID".equalsIgnoreCase(x.status)).map(x->BigDecimal.valueOf(x.amount)).reduce(BigDecimal.ZERO,BigDecimal::add); paidLabel.setText(money(paid)); repayableLabel.setText(money(repayable)); outstandingLabel.setText(money(repayable.subtract(paid).max(BigDecimal.ZERO)));
    }
    @FXML private void refresh() { loadStatementAsync(); }
    @FXML private void back() { try { FXMLLoader l=new FXMLLoader(getClass().getResource("/fxml/loans.fxml")); Parent root=l.load(); l.<LoanManagementController>getController().setCurrentUser(currentUser); Stage s=(Stage)paymentTable.getScene().getWindow(); s.setScene(new Scene(root,1400,850)); s.setMaximized(true); } catch(Exception e){ showError("Navigation error","Unable to return to the loan portfolio."); } }
    private String money(BigDecimal value){return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(value.setScale(2,RoundingMode.HALF_UP));}
    private void showError(String title,String message){Alert a=new Alert(Alert.AlertType.ERROR);a.setTitle(title);a.setHeaderText(null);a.setContentText(message);a.showAndWait();}
    public static class PaymentRow { private final int id; private final String date,method,reference,status; private final double amount; PaymentRow(int id,String date,double amount,String method,String reference,String status){this.id=id;this.date=date;this.amount=amount;this.method=method;this.reference=reference;this.status=status;} public int getId(){return id;} public String getDate(){return date;} public double getAmount(){return amount;} public String getMethod(){return method;} public String getReference(){return reference;} public String getStatus(){return status;} }
}
