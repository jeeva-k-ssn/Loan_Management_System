package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import com.loanmanagement.navigation.NavigationManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
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
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.LineChart;
import javafx.stage.Stage;
import javafx.concurrent.Task;

/** Restricted administrative user and application monitoring workspace. */
public class AdminController {
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, Integer> idColumn;
    @FXML private TableColumn<UserRow, String> nameColumn, emailColumn, roleColumn;
    @FXML private Label selectedLabel;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button saveButton;
    @FXML private TextField userSearchField, applicationSearchField;
    @FXML private ComboBox<String> applicationFilterCombo;
    @FXML private Label totalCustomersLabel, newCustomersLabel, activeCustomersLabel, pendingApplicationsLabel, activeLoansLabel;
    @FXML private Label totalDisbursedLabel, totalCollectedLabel, totalOutstandingLabel;
    @FXML private Label totalApplicationsReportLabel, pendingApplicationsReportLabel, approvedApplicationsReportLabel, rejectedApplicationsReportLabel;
    @FXML private Label totalLoansReportLabel, activeLoansReportLabel, closedLoansReportLabel;
    @FXML private PieChart applicationChart, loanChart;
    @FXML private BarChart<String, Number> financialChart;
    @FXML private LineChart<String, Number> monthlyApplicationChart;
    @FXML private TableView<ApplicationRow> applicationTable;
    @FXML private TableView<CustomerRow> customerTable;
    @FXML private TableColumn<CustomerRow,Integer> customerIdColumn, customerApplicationsColumn, customerLoansColumn;
    @FXML private TableColumn<CustomerRow,String> customerNameColumn, customerEmailColumn, customerPhoneColumn, customerAddressColumn;
    @FXML private TableColumn<CustomerRow,Double> customerBorrowedColumn, customerOutstandingColumn;
    @FXML private TextField customerSearchField;
    @FXML private ComboBox<String> customerFilterCombo;
    @FXML private Button viewCustomerButton;
    @FXML private TableView<PaymentRow> paymentTable;
    @FXML private TableColumn<PaymentRow,Integer> paymentIdColumn, paymentLoanIdColumn;
    @FXML private TableColumn<PaymentRow,String> paymentCustomerColumn, paymentDateColumn, paymentMethodColumn, paymentReferenceColumn, paymentStatusColumn;
    @FXML private TableColumn<PaymentRow,Double> paymentAmountColumn;
    @FXML private TextField paymentSearchField;
    @FXML private ComboBox<String> paymentFilterCombo;
    @FXML private TableColumn<ApplicationRow, Integer> applicationIdColumn;
    @FXML private TableColumn<ApplicationRow, String> applicationCustomerColumn, applicationTypeColumn, applicationDateColumn, applicationStatusColumn;
    @FXML private TableColumn<ApplicationRow, Double> applicationAmountColumn;
    private User currentUser;
    private ObservableList<UserRow> allUsers = FXCollections.observableArrayList();
    private ObservableList<ApplicationRow> allApplications = FXCollections.observableArrayList();
    private ObservableList<CustomerRow> allCustomers = FXCollections.observableArrayList();
    private ObservableList<PaymentRow> allPayments = FXCollections.observableArrayList();

    @FXML public void initialize() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        applicationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setPlaceholder(new Label("No users match the current search."));
        applicationTable.setPlaceholder(new Label("No applications match the current search or filter."));
        customerTable.setPlaceholder(new Label("No customers match the current search or filter."));
        paymentTable.setPlaceholder(new Label("No payments match the current search or filter."));
        configureDynamicHeight(userTable, 82, 360);
        configureDynamicHeight(applicationTable, 82, 360);
        configureDynamicHeight(customerTable, 82, 360);
        configureDynamicHeight(paymentTable, 82, 360);
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        applicationIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        applicationCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customer"));
        applicationTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        applicationAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        applicationDateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        applicationStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        applicationAmountColumn.setCellFactory(column -> new TableCell<ApplicationRow, Double>() {
            @Override protected void updateItem(Double value, boolean empty) { super.updateItem(value, empty); setText(empty || value == null ? null : NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value)); }
        });
        applicationStatusColumn.setCellFactory(column -> new TableCell<ApplicationRow, String>() {
            @Override protected void updateItem(String value, boolean empty) { super.updateItem(value, empty); getStyleClass().removeIf(style -> style.startsWith("status-")); if (empty || value == null) { setText(null); return; } setText(value.toUpperCase()); getStyleClass().add("status-badge"); getStyleClass().add("status-" + value.toLowerCase()); }
        });
        customerIdColumn.setCellValueFactory(new PropertyValueFactory<>("id")); customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name")); customerEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email")); customerPhoneColumn.setCellValueFactory(new PropertyValueFactory<>("phone")); customerAddressColumn.setCellValueFactory(new PropertyValueFactory<>("address")); customerApplicationsColumn.setCellValueFactory(new PropertyValueFactory<>("applications")); customerLoansColumn.setCellValueFactory(new PropertyValueFactory<>("loans")); customerBorrowedColumn.setCellValueFactory(new PropertyValueFactory<>("borrowed")); customerOutstandingColumn.setCellValueFactory(new PropertyValueFactory<>("outstanding"));
        customerBorrowedColumn.setCellFactory(column -> new TableCell<CustomerRow,Double>() { @Override protected void updateItem(Double value, boolean empty){super.updateItem(value,empty);setText(empty||value==null?null:money(value));} }); customerOutstandingColumn.setCellFactory(column -> new TableCell<CustomerRow,Double>() { @Override protected void updateItem(Double value, boolean empty){super.updateItem(value,empty);setText(empty||value==null?null:money(value));} });
        paymentIdColumn.setCellValueFactory(new PropertyValueFactory<>("id")); paymentLoanIdColumn.setCellValueFactory(new PropertyValueFactory<>("loanId")); paymentCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("customer")); paymentDateColumn.setCellValueFactory(new PropertyValueFactory<>("date")); paymentAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount")); paymentMethodColumn.setCellValueFactory(new PropertyValueFactory<>("method")); paymentReferenceColumn.setCellValueFactory(new PropertyValueFactory<>("reference")); paymentStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        paymentAmountColumn.setCellFactory(column -> new TableCell<PaymentRow,Double>() { @Override protected void updateItem(Double value, boolean empty){super.updateItem(value,empty);setText(empty||value==null?null:money(value));} }); paymentStatusColumn.setCellFactory(column -> new TableCell<PaymentRow,String>() { @Override protected void updateItem(String value, boolean empty){super.updateItem(value,empty);getStyleClass().removeIf(style->style.startsWith("status-"));if(empty||value==null){setText(null);return;}setText(value.toUpperCase());getStyleClass().add("status-badge");getStyleClass().add("status-"+value.toLowerCase());} });
        roleCombo.setItems(FXCollections.observableArrayList("CUSTOMER", "LOAN_OFFICER", "ADMIN"));
        userTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> select(b));
        saveButton.setDisable(true);
        userSearchField.textProperty().addListener((o, a, b) -> applyUserFilter());
        applicationSearchField.textProperty().addListener((o, a, b) -> applyApplicationFilter());
        applicationFilterCombo.setItems(FXCollections.observableArrayList("ALL", "PENDING", "APPROVED", "REJECTED"));
        applicationFilterCombo.getSelectionModel().selectFirst();
        applicationFilterCombo.valueProperty().addListener((o, a, b) -> applyApplicationFilter());
        customerSearchField.textProperty().addListener((o, a, b) -> applyCustomerFilter());
        addClearButton(customerSearchField, this::clearCustomerSearch);
        customerTable.getSelectionModel().selectedItemProperty().addListener((o,a,b)->viewCustomerButton.setDisable(b==null));
        viewCustomerButton.setDisable(true);
        customerFilterCombo.setItems(FXCollections.observableArrayList("ALL", "ACTIVE LOAN CUSTOMERS", "NO LOANS", "CLOSED LOANS")); customerFilterCombo.getSelectionModel().selectFirst(); customerFilterCombo.valueProperty().addListener((o,a,b)->applyCustomerFilter());
        paymentSearchField.textProperty().addListener((o, a, b) -> applyPaymentFilter());
        paymentFilterCombo.setItems(FXCollections.observableArrayList("ALL", "PAID", "PENDING", "FAILED")); paymentFilterCombo.getSelectionModel().selectFirst(); paymentFilterCombo.valueProperty().addListener((o,a,b)->applyPaymentFilter());
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        if (!isAdmin()) {
            userTable.setDisable(true); applicationTable.setDisable(true);
            alert(Alert.AlertType.ERROR, "Access denied", "Only administrators can access this workspace.");
            return;
        }
        refresh();
    }

    private boolean isAdmin() { return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole()); }

    private void loadUsers() {
        String sql = "SELECT USER_ID,FULL_NAME,EMAIL,USER_ROLE FROM USERS ORDER BY USER_ID";
        Task<ObservableList<UserRow>> task=new Task<>(){protected ObservableList<UserRow> call() throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){var rows=FXCollections.<UserRow>observableArrayList();while(r.next())rows.add(new UserRow(r.getInt(1),r.getString(2),r.getString(3),r.getString(4)));return rows;}}};
        task.setOnSucceeded(e->{allUsers=task.getValue();applyUserFilter();});task.setOnFailed(e->alert(Alert.AlertType.ERROR,"Users unavailable","Unable to load users."));start(task,"loanflow-admin-users");
    }

    private void loadApplications() {
        String sql = "SELECT la.APPLICATION_ID,c.FULL_NAME,NVL(la.LOAN_TYPE,'-') LOAN_TYPE,la.LOAN_AMOUNT,TO_CHAR(la.APPLICATION_DATE,'DD Mon YYYY') APPLICATION_DATE,la.STATUS "
                + "FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=la.CUSTOMER_ID ORDER BY la.APPLICATION_DATE DESC,la.APPLICATION_ID DESC";
        Task<ObservableList<ApplicationRow>> task=new Task<>(){protected ObservableList<ApplicationRow> call() throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){var rows=FXCollections.<ApplicationRow>observableArrayList();while(r.next())rows.add(new ApplicationRow(r.getInt("APPLICATION_ID"),r.getString("FULL_NAME"),r.getString("LOAN_TYPE"),r.getDouble("LOAN_AMOUNT"),r.getString("APPLICATION_DATE"),r.getString("STATUS")));return rows;}}};
        task.setOnSucceeded(e->{allApplications=task.getValue();applyApplicationFilter();});task.setOnFailed(e->alert(Alert.AlertType.ERROR,"Applications unavailable","Unable to load applications."));start(task,"loanflow-admin-applications");
    }

    private void start(Task<?> task,String name){Thread t=new Thread(task,name);t.setDaemon(true);t.start();}

    private void applyUserFilter() {
        String query = userSearchField == null ? "" : userSearchField.getText().trim().toLowerCase();
        userTable.setItems(new FilteredList<>(allUsers, row -> query.isEmpty()
                || String.valueOf(row.id).contains(query)
                || row.name.toLowerCase().contains(query)
                || row.email.toLowerCase().contains(query)
                || row.role.toLowerCase().contains(query)));
        resizeTable(userTable, 82, 360);
    }

    private void applyApplicationFilter() {
        String query = applicationSearchField == null ? "" : applicationSearchField.getText().trim().toLowerCase();
        String status = applicationFilterCombo == null || applicationFilterCombo.getValue() == null ? "ALL" : applicationFilterCombo.getValue();
        applicationTable.setItems(new FilteredList<>(allApplications, row -> {
            boolean statusMatches = "ALL".equals(status) || row.status.equalsIgnoreCase(status);
            boolean queryMatches = query.isEmpty() || String.valueOf(row.id).contains(query)
                    || row.customer.toLowerCase().contains(query) || row.type.toLowerCase().contains(query);
            return statusMatches && queryMatches;
        }));
        resizeTable(applicationTable, 82, 360);
    }

    private void select(UserRow row) {
        if (row == null) { selectedLabel.setText("No user selected"); saveButton.setDisable(true); return; }
        selectedLabel.setText("Selected: " + row.name); roleCombo.setValue(row.role); saveButton.setDisable(row.id == currentUser.getUserId());
    }

    @FXML private void saveRole() {
        if (!isAdmin()) return;
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null || roleCombo.getValue() == null) return;
        if (row.id == currentUser.getUserId()) { alert(Alert.AlertType.WARNING, "Action not allowed", "You cannot change your own administrator role."); return; }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Change " + row.name + "'s role to " + roleCombo.getValue() + "?", javafx.scene.control.ButtonType.OK, javafx.scene.control.ButtonType.CANCEL);
        confirmation.setTitle("Confirm role change"); confirmation.setHeaderText("Sensitive access change");
        if (confirmation.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) return;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE USERS SET USER_ROLE=? WHERE USER_ID=?")) {
            p.setString(1, roleCombo.getValue()); p.setInt(2, row.id); p.executeUpdate(); loadUsers();
            alert(Alert.AlertType.INFORMATION, "Role updated", "The user role was updated.");
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Update failed", "Unable to update the user role."); }
    }

    @FXML private void refresh() { if (isAdmin()) { loadUsers(); loadApplications(); loadCustomers(); loadPayments(); loadMetrics(); } }
    @FXML private void openCustomerProfile() { CustomerRow row=customerTable.getSelectionModel().getSelectedItem();if(row==null)return;try{NavigationManager.navigate((Stage)customerTable.getScene().getWindow(),"/fxml/profile.fxml","LoanFlow - Customer Profile",c->((ProfileController)c).setCustomer(currentUser,row.id));}catch(Exception e){alert(Alert.AlertType.ERROR,"Profile unavailable","Unable to open the customer profile.");} }

    private void loadMetrics() {
        Task<AdminMetrics> task=new Task<>(){protected AdminMetrics call() throws SQLException{try(Connection c=DatabaseConnection.getConnection()){return new AdminMetrics(queryCount(c,"SELECT COUNT(*) FROM LMS_CUSTOMER"),queryNewCustomerCount(c),queryCount(c,"SELECT COUNT(DISTINCT CUSTOMER_ID) FROM LOAN WHERE STATUS='ACTIVE'"),queryCount(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE UPPER(STATUS)='PENDING'"),queryCount(c,"SELECT COUNT(*) FROM LOAN WHERE STATUS='ACTIVE'"),queryCount(c,"SELECT COUNT(*) FROM LOAN_APPLICATION"),queryCount(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE UPPER(STATUS)='APPROVED'"),queryCount(c,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE UPPER(STATUS)='REJECTED'"),queryCount(c,"SELECT COUNT(*) FROM LOAN"),queryCount(c,"SELECT COUNT(*) FROM LOAN WHERE STATUS='CLOSED'"),queryAmount(c,"SELECT NVL(SUM(LOAN_AMOUNT),0) FROM LOAN"),queryAmount(c,"SELECT NVL(SUM(AMOUNT),0) FROM PAYMENT WHERE PAYMENT_STATUS='PAID'"),queryAmount(c,"SELECT NVL(SUM(EMI_AMOUNT * TENURE_MONTHS),0) FROM LOAN"));}}};
        task.setOnSucceeded(e->{AdminMetrics m=task.getValue();totalCustomersLabel.setText(String.valueOf(m.customers));newCustomersLabel.setText(String.valueOf(m.newCustomers));activeCustomersLabel.setText(String.valueOf(m.activeCustomers));pendingApplicationsLabel.setText(String.valueOf(m.pending));activeLoansLabel.setText(String.valueOf(m.activeLoans));totalApplicationsReportLabel.setText(String.valueOf(m.applications));pendingApplicationsReportLabel.setText(String.valueOf(m.pending));approvedApplicationsReportLabel.setText(String.valueOf(m.approved));rejectedApplicationsReportLabel.setText(String.valueOf(m.rejected));totalLoansReportLabel.setText(String.valueOf(m.loans));activeLoansReportLabel.setText(String.valueOf(m.activeLoans));closedLoansReportLabel.setText(String.valueOf(m.closedLoans));totalDisbursedLabel.setText(money(m.disbursed));totalCollectedLabel.setText(money(m.collected));totalOutstandingLabel.setText(money(Math.max(0,m.repayable-m.collected)));loadChartsAsync();});
        task.setOnFailed(e->{totalCustomersLabel.setText("-");newCustomersLabel.setText("-");activeCustomersLabel.setText("-");pendingApplicationsLabel.setText("-");activeLoansLabel.setText("-");});start(task,"loanflow-admin-metrics");
    }
    private static final class AdminMetrics{final int customers,newCustomers,activeCustomers,pending,activeLoans,applications,approved,rejected,loans,closedLoans;final double disbursed,collected,repayable;AdminMetrics(int c,int n,int ac,int p,int al,int a,int ap,int r,int l,int cl,double d,double co,double re){customers=c;newCustomers=n;activeCustomers=ac;pending=p;activeLoans=al;applications=a;approved=ap;rejected=r;loans=l;closedLoans=cl;disbursed=d;collected=co;repayable=re;}}
    private void loadChartsAsync(){Task<ChartSnapshot> task=new Task<>(){protected ChartSnapshot call() throws SQLException{try(Connection c=DatabaseConnection.getConnection()){return new ChartSnapshot(readPairs(c,"SELECT STATUS,COUNT(*) FROM LOAN_APPLICATION GROUP BY STATUS"),readPairs(c,"SELECT STATUS,COUNT(*) FROM LOAN GROUP BY STATUS"),queryAmount(c,"SELECT NVL(SUM(LOAN_AMOUNT),0) FROM LOAN"),queryAmount(c,"SELECT NVL(SUM(AMOUNT),0) FROM PAYMENT WHERE PAYMENT_STATUS='PAID'"),queryAmount(c,"SELECT NVL(SUM(EMI_AMOUNT * TENURE_MONTHS),0) FROM LOAN"),readPairs(c,"SELECT TO_CHAR(APPLICATION_DATE,'YYYY-MM'),COUNT(*) FROM LOAN_APPLICATION GROUP BY TO_CHAR(APPLICATION_DATE,'YYYY-MM') ORDER BY TO_CHAR(APPLICATION_DATE,'YYYY-MM')"));}}};task.setOnSucceeded(e->{ChartSnapshot s=task.getValue();applicationChart.getData().clear();s.applications.forEach(x->applicationChart.getData().add(new PieChart.Data(x[0],Integer.parseInt(x[1]))));loanChart.getData().clear();s.loans.forEach(x->loanChart.getData().add(new PieChart.Data(x[0],Integer.parseInt(x[1]))));financialChart.getData().clear();XYChart.Series<String,Number> f=new XYChart.Series<>();f.setName("Amount");f.getData().add(new XYChart.Data<>("Disbursed",s.disbursed));f.getData().add(new XYChart.Data<>("Collected",s.collected));f.getData().add(new XYChart.Data<>("Outstanding",s.repayable-s.collected));financialChart.getData().add(f);monthlyApplicationChart.getData().clear();XYChart.Series<String,Number> m=new XYChart.Series<>();m.setName("Applications");s.monthly.forEach(x->m.getData().add(new XYChart.Data<>(x[0],Integer.parseInt(x[1]))));monthlyApplicationChart.getData().add(m);});start(task,"loanflow-admin-charts");}
    private java.util.List<String[]> readPairs(Connection c,String sql)throws SQLException{var out=new java.util.ArrayList<String[]>();try(PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){while(r.next())out.add(new String[]{r.getString(1),String.valueOf(r.getInt(2))});}return out;}
    private static final class ChartSnapshot{final java.util.List<String[]> applications,loans,monthly;final double disbursed,collected,repayable;ChartSnapshot(java.util.List<String[]>a,java.util.List<String[]>l,double d,double c,double r,java.util.List<String[]>m){applications=a;loans=l;disbursed=d;collected=c;repayable=r;monthly=m;}}
    private void loadCustomers() {
        String sql="SELECT c.CUSTOMER_ID,c.FULL_NAME,u.EMAIL,c.PHONE,c.ADDRESS,(SELECT COUNT(*) FROM LOAN_APPLICATION la WHERE la.CUSTOMER_ID=c.CUSTOMER_ID),(SELECT COUNT(*) FROM LOAN l WHERE l.CUSTOMER_ID=c.CUSTOMER_ID),(SELECT COUNT(*) FROM LOAN l WHERE l.CUSTOMER_ID=c.CUSTOMER_ID AND l.STATUS='ACTIVE'),(SELECT COUNT(*) FROM LOAN l WHERE l.CUSTOMER_ID=c.CUSTOMER_ID AND l.STATUS='CLOSED'),(SELECT NVL(SUM(l.LOAN_AMOUNT),0) FROM LOAN l WHERE l.CUSTOMER_ID=c.CUSTOMER_ID),(SELECT GREATEST(NVL(SUM(l3.EMI_AMOUNT*l3.TENURE_MONTHS),0)-NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p JOIN LOAN l4 ON l4.LOAN_ID=p.LOAN_ID WHERE l4.CUSTOMER_ID=c.CUSTOMER_ID AND p.PAYMENT_STATUS='PAID'),0),0) FROM LOAN l3 WHERE l3.CUSTOMER_ID=c.CUSTOMER_ID) FROM LMS_CUSTOMER c JOIN USERS u ON u.USER_ID=c.USER_ID ORDER BY c.CUSTOMER_ID";
        Task<ObservableList<CustomerRow>> task=new Task<>(){protected ObservableList<CustomerRow> call() throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){var rows=FXCollections.<CustomerRow>observableArrayList();while(r.next())rows.add(new CustomerRow(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getInt(6),r.getInt(7),r.getInt(8),r.getInt(9),r.getDouble(10),r.getDouble(11)));return rows;}}};task.setOnSucceeded(e->{allCustomers=task.getValue();applyCustomerFilter();});task.setOnFailed(e->customerTable.setItems(FXCollections.observableArrayList()));start(task,"loanflow-admin-customers");
    }
    private void loadCharts(Connection c) throws SQLException {
        applicationChart.getData().clear(); applicationChart.getData().addAll(pieData(c, "SELECT STATUS,COUNT(*) FROM LOAN_APPLICATION GROUP BY STATUS"));
        loanChart.getData().clear(); loanChart.getData().addAll(pieData(c, "SELECT STATUS,COUNT(*) FROM LOAN GROUP BY STATUS"));
        financialChart.getData().clear(); XYChart.Series<String,Number> series=new XYChart.Series<>(); series.setName("Amount"); series.getData().add(new XYChart.Data<>("Disbursed",queryAmount(c,"SELECT NVL(SUM(LOAN_AMOUNT),0) FROM LOAN"))); series.getData().add(new XYChart.Data<>("Collected",queryAmount(c,"SELECT NVL(SUM(AMOUNT),0) FROM PAYMENT WHERE PAYMENT_STATUS='PAID'"))); series.getData().add(new XYChart.Data<>("Outstanding",queryAmount(c,"SELECT NVL(SUM(EMI_AMOUNT * TENURE_MONTHS),0) FROM LOAN")-queryAmount(c,"SELECT NVL(SUM(AMOUNT),0) FROM PAYMENT WHERE PAYMENT_STATUS='PAID'"))); financialChart.getData().add(series);
        monthlyApplicationChart.getData().clear(); XYChart.Series<String,Number> monthly=new XYChart.Series<>(); monthly.setName("Applications"); try(PreparedStatement p=c.prepareStatement("SELECT TO_CHAR(APPLICATION_DATE,'YYYY-MM'),COUNT(*) FROM LOAN_APPLICATION GROUP BY TO_CHAR(APPLICATION_DATE,'YYYY-MM') ORDER BY TO_CHAR(APPLICATION_DATE,'YYYY-MM')"); ResultSet r=p.executeQuery()){while(r.next())monthly.getData().add(new XYChart.Data<>(r.getString(1),r.getInt(2)));} monthlyApplicationChart.getData().add(monthly);
    }
    private java.util.List<PieChart.Data> pieData(Connection c, String sql) throws SQLException { java.util.List<PieChart.Data> data=new java.util.ArrayList<>(); try(PreparedStatement p=c.prepareStatement(sql); ResultSet r=p.executeQuery()){while(r.next()) data.add(new PieChart.Data(r.getString(1),r.getInt(2)));} return data; }
    private int queryCount(Connection c, String sql) throws SQLException { try (PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; } }
    private int queryNewCustomerCount(Connection c) throws SQLException {
        if (queryCount(c, "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='USERS' AND COLUMN_NAME='CREATED_AT'") == 0) return 0;
        return queryCount(c, "SELECT COUNT(*) FROM USERS WHERE USER_ROLE='CUSTOMER' AND CREATED_AT >= SYSDATE - 30");
    }
    private double queryAmount(Connection c, String sql) throws SQLException { try (PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) { return r.next() ? r.getDouble(1) : 0; } }
    private String money(double value) { return NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(value); }
    private void applyCustomerFilter(){String q=customerSearchField==null?"":customerSearchField.getText().trim().toLowerCase();String filter=customerFilterCombo==null||customerFilterCombo.getValue()==null?"ALL":customerFilterCombo.getValue();customerTable.setItems(new FilteredList<>(allCustomers,row->(q.isEmpty()||String.valueOf(row.id).contains(q)||row.name.toLowerCase().contains(q)||row.email.toLowerCase().contains(q)||row.phone.toLowerCase().contains(q))&&("ALL".equals(filter)||("ACTIVE LOAN CUSTOMERS".equals(filter)&&row.activeLoans>0)||("NO LOANS".equals(filter)&&row.loans==0)||("CLOSED LOANS".equals(filter)&&row.closedLoans>0))));resizeTable(customerTable,82,360);}
    private void clearCustomerSearch(){if(customerSearchField!=null)customerSearchField.clear();}
    private void addClearButton(TextField field, Runnable action){if(field!=null&&field.getParent() instanceof HBox box){Button clear=new Button("Clear");clear.getStyleClass().add("secondary-action-button");clear.setOnAction(event->action.run());int index=box.getChildren().indexOf(field)+1;box.getChildren().add(Math.max(0,index),clear);}}
    private void loadPayments(){String sql="SELECT p.PAYMENT_ID,p.LOAN_ID,c.FULL_NAME,TO_CHAR(p.PAYMENT_DATE,'DD Mon YYYY') PAYMENT_DATE,p.AMOUNT,NVL(p.PAYMENT_METHOD,'-'),NVL(p.PAYMENT_REFERENCE,'-'),p.PAYMENT_STATUS FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID=p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID ORDER BY p.PAYMENT_DATE DESC,p.PAYMENT_ID DESC";Task<ObservableList<PaymentRow>> task=new Task<>(){protected ObservableList<PaymentRow> call() throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){var rows=FXCollections.<PaymentRow>observableArrayList();while(r.next())rows.add(new PaymentRow(r.getInt(1),r.getInt(2),r.getString(3),r.getString(4),r.getDouble(5),r.getString(6),r.getString(7),r.getString(8)));return rows;}}};task.setOnSucceeded(e->{allPayments=task.getValue();applyPaymentFilter();});task.setOnFailed(e->paymentTable.setItems(FXCollections.observableArrayList()));start(task,"loanflow-admin-payments");}
    private void applyPaymentFilter(){String q=paymentSearchField==null?"":paymentSearchField.getText().trim().toLowerCase();String status=paymentFilterCombo==null||paymentFilterCombo.getValue()==null?"ALL":paymentFilterCombo.getValue();paymentTable.setItems(new FilteredList<>(allPayments,row->("ALL".equals(status)||row.status.equalsIgnoreCase(status))&&(q.isEmpty()||String.valueOf(row.id).contains(q)||String.valueOf(row.loanId).contains(q)||row.customer.toLowerCase().contains(q)||row.reference.toLowerCase().contains(q))));resizeTable(paymentTable,82,360);}
    private void configureDynamicHeight(TableView<?> table,double minimum,double maximum){table.setFixedCellSize(34);table.setMinHeight(minimum);table.setMaxHeight(maximum);resizeTable(table,minimum,maximum);}
    private void resizeTable(TableView<?> table,double minimum,double maximum){int rows=table.getItems()==null?0:table.getItems().size();table.setPrefHeight(Math.min(maximum,Math.max(minimum,32+rows*table.getFixedCellSize())));}
    @FXML private void back() {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml")); Parent root = l.load();
            l.<DashboardController>getController().setCurrentUser(currentUser); Stage s = (Stage) userTable.getScene().getWindow();
            s.getScene().setRoot(root); s.setMaximized(true);
        } catch (Exception e) { alert(Alert.AlertType.ERROR, "Navigation error", "Unable to return to the dashboard."); }
    }
    private void alert(Alert.AlertType type, String title, String message) { Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(message); a.showAndWait(); }

    public static class UserRow {
        private final int id; private final String name, email, role;
        UserRow(int id, String name, String email, String role) { this.id=id; this.name=name; this.email=email; this.role=role; }
        public int getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getRole(){return role;}
    }
    public static class ApplicationRow {
        private final int id; private final String customer, type, date, status; private final double amount;
        ApplicationRow(int id, String customer, String type, double amount, String date, String status) { this.id=id; this.customer=customer; this.type=type; this.amount=amount; this.date=date; this.status=status; }
        public int getId(){return id;} public String getCustomer(){return customer;} public String getType(){return type;} public double getAmount(){return amount;} public String getDate(){return date;} public String getStatus(){return status;}
    }
    public static class CustomerRow { private final int id,applications,loans,activeLoans,closedLoans; private final String name,email,phone,address; private final double borrowed,outstanding; CustomerRow(int id,String name,String email,String phone,String address,int applications,int loans,int activeLoans,int closedLoans,double borrowed,double outstanding){this.id=id;this.name=name;this.email=email;this.phone=phone==null?"-":phone;this.address=address==null?"-":address;this.applications=applications;this.loans=loans;this.activeLoans=activeLoans;this.closedLoans=closedLoans;this.borrowed=borrowed;this.outstanding=outstanding;} public int getId(){return id;} public String getName(){return name;} public String getEmail(){return email;} public String getPhone(){return phone;} public String getAddress(){return address;} public int getApplications(){return applications;} public int getLoans(){return loans;} public double getBorrowed(){return borrowed;} public double getOutstanding(){return outstanding;} }
    public static class PaymentRow { private final int id,loanId; private final String customer,date,method,reference,status; private final double amount; PaymentRow(int id,int loanId,String customer,String date,double amount,String method,String reference,String status){this.id=id;this.loanId=loanId;this.customer=customer;this.date=date;this.amount=amount;this.method=method;this.reference=reference;this.status=status;} public int getId(){return id;} public int getLoanId(){return loanId;} public String getCustomer(){return customer;} public String getDate(){return date;} public double getAmount(){return amount;} public String getMethod(){return method;} public String getReference(){return reference;} public String getStatus(){return status;} }
}
