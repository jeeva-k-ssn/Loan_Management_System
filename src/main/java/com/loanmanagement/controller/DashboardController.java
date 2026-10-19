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
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import com.loanmanagement.navigation.NavigationManager;
import com.loanmanagement.util.AlertUtil;
import com.loanmanagement.util.EmiStatusUtil;

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
    @FXML private GridPane summaryGrid;
    @FXML private Button applyLoanButton;
    @FXML private Button viewLoansButton;
    @FXML private Button viewPaymentsButton;
    @FXML private Button pendingApplicationsButton;
    @FXML private Button loanPortfolioButton;
    @FXML private Button userManagementButton;
    @FXML private Button sidebarApplyButton;
    @FXML private Button sidebarLoansButton;
    @FXML private Button sidebarPaymentsButton;
    @FXML private Button sidebarPendingButton;
    @FXML private Button sidebarPortfolioButton;
    @FXML private Button sidebarUsersButton;
    @FXML private Button sidebarNotificationsButton;
    @FXML private Button sidebarProfileButton;
    @FXML private TableView<EmiRow> emiTrackerTable;
    @FXML private Button emiScheduleButton;
    @FXML private TableColumn<EmiRow,Integer> emiLoanIdColumn;
    @FXML private TableColumn<EmiRow,String> emiTypeColumn, emiDueDateColumn, emiStatusColumn;
    @FXML private TableColumn<EmiRow,BigDecimal> emiAmountColumn, emiPaidColumn, emiRemainingColumn;

    private User currentUser;

    @FXML
    public void initialize() {
        configureDefaultButtons();
        summaryGrid.widthProperty().addListener((obs, oldWidth, newWidth) -> configureResponsiveSummaryCards(newWidth.doubleValue()));
        configureResponsiveSummaryCards(0);
        if (emiTrackerTable != null) {
            emiTrackerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            emiLoanIdColumn.setCellValueFactory(new PropertyValueFactory<>("loanId"));
            emiTypeColumn.setCellValueFactory(new PropertyValueFactory<>("loanType"));
            emiAmountColumn.setCellValueFactory(new PropertyValueFactory<>("emi"));
            emiDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
            emiPaidColumn.setCellValueFactory(new PropertyValueFactory<>("paid"));
            emiRemainingColumn.setCellValueFactory(new PropertyValueFactory<>("remaining"));
            emiStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            emiTrackerTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            emiStatusColumn.setCellFactory(c -> new javafx.scene.control.TableCell<>() { protected void updateItem(String v, boolean empty){super.updateItem(v,empty);setText(empty?null:v);getStyleClass().removeIf(s->s.startsWith("status-"));if(!empty&&v!=null){getStyleClass().add("status-badge");getStyleClass().add("status-"+v.toLowerCase().replace(' ','-'));}} });
        }
    }

    private void configureResponsiveSummaryCards(double width) {
        if (summaryGrid == null) return;
        int columns = width >= 900 ? 4 : width >= 560 ? 2 : 1;
        summaryGrid.getColumnConstraints().clear();
        for (int i = 0; i < columns; i++) {
            ColumnConstraints constraint = new ColumnConstraints();
            constraint.setPercentWidth(100.0 / columns);
            constraint.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            summaryGrid.getColumnConstraints().add(constraint);
        }
        for (int i = 0; i < summaryGrid.getChildren().size(); i++) {
            javafx.scene.Node card = summaryGrid.getChildren().get(i);
            GridPane.setColumnIndex(card, i % columns);
            GridPane.setRowIndex(card, i / columns);
        }
    }

    public void setCurrentUser(User user) {
        currentUser = user;
        if (user == null) return;
        welcomeLabel.setText("Welcome, " + user.getFullName());
        roleLabel.setText(user.getDisplayRole());
        configureRoleView();
        loadDashboardData();
        if (hasRole("CUSTOMER")) loadCurrentEmis();
    }

    private void configureRoleView() {
        configureDefaultButtons();
        hideButton(sidebarApplyButton); hideButton(sidebarLoansButton); hideButton(sidebarPaymentsButton);
        hideButton(sidebarPendingButton); hideButton(sidebarPortfolioButton); hideButton(sidebarUsersButton);
        hideButton(sidebarProfileButton); showButton(sidebarNotificationsButton);
        if (hasRole("CUSTOMER")) {
            pageTitleLabel.setText("Your loan overview");
            pageDescriptionLabel.setText("Track applications, active loans, and repayments in one place.");
            setCardCopy("APPLICATIONS", "Submitted by you", "ACTIVE LOANS", "Currently being repaid",
                    "TOTAL PAID", "Successful repayments", "OUTSTANDING", "Remaining across active loans");
            showButton(applyLoanButton); showButton(viewLoansButton); showButton(viewPaymentsButton);
            showButton(sidebarApplyButton); showButton(sidebarLoansButton); showButton(sidebarPaymentsButton);
            showButton(sidebarProfileButton);
        } else if (hasRole("LOAN_OFFICER")) {
            pageTitleLabel.setText("Loan processing workspace");
            pageDescriptionLabel.setText("Review applications and monitor the current lending portfolio.");
            setCardCopy("PENDING", "Awaiting review", "APPROVED", "Approved applications",
                    "ACTIVE LOANS", "Open loan accounts", "PAYMENTS", "Payments received");
            showButton(pendingApplicationsButton);
            showButton(loanPortfolioButton);
            showButton(sidebarPendingButton); showButton(sidebarPortfolioButton);
        } else if (hasRole("ADMIN")) {
            pageTitleLabel.setText("Administrative overview");
            pageDescriptionLabel.setText("Monitor customers, lending activity, and payment operations.");
            setCardCopy("CUSTOMERS", "Registered customers", "APPLICATIONS", "All applications",
                    "ACTIVE LOANS", "Currently active", "PAYMENTS", "Recorded payments");
            showButton(loanPortfolioButton);
            showButton(userManagementButton);
            showButton(sidebarPortfolioButton); showButton(sidebarUsersButton);
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
        hideButton(loanPortfolioButton);
        hideButton(userManagementButton);
    }

    private void hideButton(Button button) {
        if (button != null) { button.setVisible(false); button.setManaged(false); }
    }

    private void showButton(Button button) { button.setVisible(true); button.setManaged(true); }

    private boolean hasRole(String role) {
        return currentUser != null && role.equalsIgnoreCase(currentUser.getRole());
    }

    private void loadDashboardData() {
        customersValueLabel.setText("…"); applicationsValueLabel.setText("…");
        loansValueLabel.setText("…"); paymentsValueLabel.setText("…");
        Task<DashboardSnapshot> task = new Task<>() {
            @Override protected DashboardSnapshot call() throws SQLException { return fetchDashboardSnapshot(); }
        };
        task.setOnSucceeded(event -> renderDashboard(task.getValue()));
        task.setOnFailed(event -> { setMetricValues(0, 0, 0, 0); showEmptyActivity("Dashboard data is temporarily unavailable."); });
        Thread thread = new Thread(task, "loanflow-dashboard-load"); thread.setDaemon(true); thread.start();
    }

    private DashboardSnapshot fetchDashboardSnapshot() throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            int first, second, third, fourth;
            if (hasRole("CUSTOMER")) {
                first = queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = la.CUSTOMER_ID WHERE c.USER_ID = ?");
                second = queryCount(connection, "SELECT COUNT(*) FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND l.STATUS = 'ACTIVE'");
                double paid = queryAmount(connection, "SELECT NVL(SUM(p.AMOUNT), 0) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID = p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND p.PAYMENT_STATUS = 'PAID'");
                double repayable = queryAmount(connection, "SELECT NVL(SUM(l.EMI_AMOUNT * l.TENURE_MONTHS), 0) FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ?");
                third = (int) Math.round(paid * 100); fourth = (int) Math.round(Math.max(0, repayable - paid) * 100);
            } else if (hasRole("LOAN_OFFICER")) {
                first=queryCount(connection,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE STATUS = 'PENDING'"); second=queryCount(connection,"SELECT COUNT(*) FROM LOAN_APPLICATION WHERE STATUS = 'APPROVED'"); third=queryCount(connection,"SELECT COUNT(*) FROM LOAN WHERE STATUS = 'ACTIVE'"); fourth=queryCount(connection,"SELECT COUNT(*) FROM PAYMENT WHERE PAYMENT_STATUS = 'PAID'");
            } else {
                first=queryCount(connection,"SELECT COUNT(*) FROM LMS_CUSTOMER"); second=queryCount(connection,"SELECT COUNT(*) FROM LOAN_APPLICATION"); third=queryCount(connection,"SELECT COUNT(*) FROM LOAN WHERE STATUS = 'ACTIVE'"); fourth=queryCount(connection,"SELECT COUNT(*) FROM PAYMENT");
            }
            var activity = new java.util.ArrayList<String>();
            String sql = hasRole("CUSTOMER") ? "SELECT message FROM (SELECT 'Application #' || la.APPLICATION_ID || ' is ' || la.STATUS || ' - ' || TO_CHAR(la.APPLICATION_DATE, 'DD Mon YYYY') AS message FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = la.CUSTOMER_ID WHERE c.USER_ID = ? ORDER BY la.APPLICATION_DATE DESC) WHERE ROWNUM <= 5" : "SELECT message FROM (SELECT 'Application #' || APPLICATION_ID || ' is ' || STATUS || ' - ' || TO_CHAR(APPLICATION_DATE, 'DD Mon YYYY') AS message FROM LOAN_APPLICATION ORDER BY APPLICATION_DATE DESC) WHERE ROWNUM <= 5";
            try (PreparedStatement statement=connection.prepareStatement(sql)) { if (hasRole("CUSTOMER")) statement.setInt(1,currentUser.getUserId()); try(ResultSet rs=statement.executeQuery()){while(rs.next()) activity.add(rs.getString(1));} }
            int unreadNotifications = 0;
            try (PreparedStatement notificationQuery = connection.prepareStatement(
                    "SELECT COUNT(*) FROM LOANFLOW_NOTIFICATION WHERE USER_ID=? AND IS_READ='N'")) {
                notificationQuery.setInt(1, currentUser.getUserId());
                try (ResultSet result = notificationQuery.executeQuery()) {
                    if (result.next()) unreadNotifications = result.getInt(1);
                }
            } catch (SQLException ignored) {
                // Keep the dashboard usable if the optional notification migration is absent.
            }
            return new DashboardSnapshot(first, second, third, fourth, activity, unreadNotifications);
        }
    }

    private void renderDashboard(DashboardSnapshot snapshot) {
        if (hasRole("CUSTOMER")) {
            customersValueLabel.setText(String.valueOf(snapshot.first)); applicationsValueLabel.setText(String.valueOf(snapshot.second));
            loansValueLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(snapshot.third / 100.0));
            paymentsValueLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(snapshot.fourth / 100.0));
        } else setMetricValues(snapshot.first, snapshot.second, snapshot.third, snapshot.fourth);
        if (sidebarNotificationsButton != null) {
            sidebarNotificationsButton.setText(snapshot.unreadNotifications > 0
                    ? "Notifications (" + snapshot.unreadNotifications + ")"
                    : "Notifications");
        }
        activityContainer.getChildren().clear();
        if (snapshot.activity.isEmpty()) showEmptyActivity("No recent activity yet.");
        else for (String message : snapshot.activity) { Label item=new Label(message); item.setWrapText(true); item.setMaxWidth(Double.MAX_VALUE); item.getStyleClass().add("activity-item"); activityContainer.getChildren().add(item); }
    }

    private static final class DashboardSnapshot {
        final int first, second, third, fourth, unreadNotifications; final java.util.List<String> activity;
        DashboardSnapshot(int first,int second,int third,int fourth,java.util.List<String> activity,int unreadNotifications){this.first=first;this.second=second;this.third=third;this.fourth=fourth;this.activity=activity;this.unreadNotifications=unreadNotifications;}
    }

    private void loadCustomerMetrics(Connection connection) throws SQLException {
        int applications = queryCount(connection, "SELECT COUNT(*) FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = la.CUSTOMER_ID WHERE c.USER_ID = ?");
        int loans = queryCount(connection, "SELECT COUNT(*) FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND l.STATUS = 'ACTIVE'");
        int payments = queryCount(connection, "SELECT COUNT(*) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID = p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND p.PAYMENT_STATUS = 'PAID'");
        double paid = queryAmount(connection, "SELECT NVL(SUM(p.AMOUNT), 0) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID = p.LOAN_ID JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ? AND p.PAYMENT_STATUS = 'PAID'");
        double repayable = queryAmount(connection, "SELECT NVL(SUM(l.EMI_AMOUNT * l.TENURE_MONTHS), 0) FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID = l.CUSTOMER_ID WHERE c.USER_ID = ?");
        customersValueLabel.setText(String.valueOf(applications));
        applicationsValueLabel.setText(String.valueOf(loans));
        loansValueLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(paid));
        paymentsValueLabel.setText(NumberFormat.getCurrencyInstance(new Locale("en", "IN")).format(Math.max(0, repayable - paid)));
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
    private void loadNotificationCount(Connection connection) {
        if (sidebarNotificationsButton == null || currentUser == null) return;
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM LOANFLOW_NOTIFICATION WHERE USER_ID=? AND IS_READ='N'")) {
            statement.setInt(1, currentUser.getUserId());
            try (ResultSet results = statement.executeQuery()) { int count = results.next() ? results.getInt(1) : 0; sidebarNotificationsButton.setText(count > 0 ? "Notifications (" + count + ")" : "Notifications"); }
        } catch (SQLException ignored) { sidebarNotificationsButton.setText("Notifications"); }
    }

    @FXML private void refreshDashboard() { if (currentUser != null) loadDashboardData(); }
    private void loadCurrentEmis() {
        Task<java.util.List<EmiRow>> task = new Task<>() {
            @Override protected java.util.List<EmiRow> call() throws SQLException {
                java.util.List<EmiRow> rows = new java.util.ArrayList<>();
                String sql = "SELECT l.LOAN_ID,NVL(la.LOAN_TYPE,'-') LOAN_TYPE,l.EMI_AMOUNT,s.DUE_DATE,s.DUE_AMOUNT," +
                        "NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p WHERE p.INSTALLMENT_ID=s.INSTALLMENT_ID AND p.PAYMENT_STATUS='PAID'),0) PAID " +
                        "FROM LOAN l JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=l.CUSTOMER_ID " +
                        "JOIN LOAN_APPLICATION la ON la.APPLICATION_ID=l.APPLICATION_ID " +
                        "JOIN LOAN_EMI_SCHEDULE s ON s.LOAN_ID=l.LOAN_ID " +
                        "WHERE c.USER_ID=? AND l.STATUS='ACTIVE' AND TRUNC(s.DUE_DATE,'MM')=TRUNC(SYSDATE,'MM') " +
                        "ORDER BY l.LOAN_ID";
                try (Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
                    p.setInt(1,currentUser.getUserId());
                    try (ResultSet r=p.executeQuery()) { while(r.next()) { BigDecimal due=r.getBigDecimal("DUE_AMOUNT").setScale(2,RoundingMode.HALF_UP); BigDecimal paid=r.getBigDecimal("PAID").setScale(2,RoundingMode.HALF_UP); rows.add(new EmiRow(r.getInt("LOAN_ID"),r.getString("LOAN_TYPE"),r.getBigDecimal("EMI_AMOUNT"),r.getDate("DUE_DATE"),due,paid,status(paid,due,r.getDate("DUE_DATE")))); } }
                }
                return rows;
            }
        };
        task.setOnSucceeded(e -> {
            emiTrackerTable.getItems().setAll(task.getValue());
            if (!emiTrackerTable.getItems().isEmpty()) {
                emiTrackerTable.getSelectionModel().selectFirst();
                emiScheduleButton.setDisable(false);
            } else {
                emiScheduleButton.setDisable(true);
            }
        });
        task.setOnFailed(e -> {
            emiTrackerTable.getItems().clear();
            emiScheduleButton.setDisable(true);
            showError("EMI tracker unavailable", "Apply database/module12_emi_tracker_migration.sql, then refresh the dashboard.");
        });
        Thread t=new Thread(task,"loanflow-emi-tracker-load"); t.setDaemon(true); t.start();
    }
    private String status(BigDecimal paid, BigDecimal due, java.sql.Date dueDate) { return EmiStatusUtil.status(paid,due,dueDate.toLocalDate(),java.time.LocalDate.now()); }
    @FXML private void openEmiSchedule() {
        EmiRow selected=emiTrackerTable.getSelectionModel().getSelectedItem();
        if (selected == null && !emiTrackerTable.getItems().isEmpty()) {
            selected = emiTrackerTable.getItems().get(0);
            emiTrackerTable.getSelectionModel().select(selected);
        }
        if(selected==null){showInformation("Select a loan","Select a loan to view its installment schedule.");return;}
        TableView<ScheduleRow> table=new TableView<>(); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); String[] headers={"Installment","Due date","Due","Paid","Remaining","Status"};
        for(int i=0;i<headers.length;i++){ TableColumn<ScheduleRow,String> col=new TableColumn<>(headers[i]); final int n=i; col.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().value(n))); table.getColumns().add(col); }
        try(Connection c=DatabaseConnection.getConnection(); PreparedStatement p=c.prepareStatement("SELECT s.INSTALLMENT_NO,s.DUE_DATE,s.DUE_AMOUNT,NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p WHERE p.INSTALLMENT_ID=s.INSTALLMENT_ID AND p.PAYMENT_STATUS='PAID'),0) PAID FROM LOAN_EMI_SCHEDULE s WHERE s.LOAN_ID=? ORDER BY s.INSTALLMENT_NO")){p.setInt(1,selected.loanId);try(ResultSet r=p.executeQuery()){while(r.next()){BigDecimal due=r.getBigDecimal("DUE_AMOUNT").setScale(2),paid=r.getBigDecimal("PAID").setScale(2);table.getItems().add(new ScheduleRow(r.getInt(1),r.getDate(2),due,paid,status(paid,due,r.getDate(2))));}}}
        catch(SQLException ex){showError("Schedule unavailable","Apply database/module12_emi_tracker_migration.sql before viewing installment schedules.");return;}
        javafx.scene.control.Dialog<Void> dialog=new javafx.scene.control.Dialog<>(); dialog.setTitle("Loan #"+selected.loanId+" · EMI schedule"); dialog.getDialogPane().setContent(table); dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE); dialog.showAndWait();
    }
    @FXML private void applyLoan() { if (requireRole("CUSTOMER")) openLoanApplication(); }
    @FXML private void viewLoans() { if (requireRole("CUSTOMER")) openLoanPortfolio(LoanManagementController.ViewMode.LOANS); }
    @FXML private void viewPayments() { if (requireRole("CUSTOMER")) openLoanPortfolio(LoanManagementController.ViewMode.PAYMENTS); }
    @FXML private void openNotifications() {
        if (currentUser == null) return;
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/notifications.fxml")); Parent root = loader.load(); loader.<NotificationController>getController().setCurrentUser(currentUser); replaceScene(root, "LoanFlow - Notifications"); }
        catch (Exception exception) { showError("Navigation Error", "Unable to open notifications."); }
    }
    @FXML private void openProfile() {
        if (!requireRole("CUSTOMER")) return;
        try { FXMLLoader loader=new FXMLLoader(getClass().getResource("/fxml/profile.fxml")); Parent root=loader.load(); loader.<ProfileController>getController().setCurrentUser(currentUser); replaceScene(root,"LoanFlow - Profile"); }
        catch(Exception e){showError("Navigation Error","Unable to open your profile.");}
    }
    @FXML private void openLoanPortfolioPage() { if (currentUser != null) openLoanPortfolio(LoanManagementController.ViewMode.LOANS); }
    @FXML private void openUserManagement() {
        if (!requireRole("ADMIN")) return;
        try { FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin.fxml")); Parent root = loader.load(); loader.<AdminController>getController().setCurrentUser(currentUser); replaceScene(root, "LoanFlow - User Management"); }
        catch (Exception exception) { showError("Navigation Error", "Unable to open user management."); }
    }

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

    private void openLoanPortfolio(LoanManagementController.ViewMode viewMode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/loans.fxml"));
            Parent root = loader.load();
            LoanManagementController controller = loader.getController();
            controller.setViewMode(viewMode);
            controller.setCurrentUser(currentUser);
            replaceScene(root, "LoanFlow - Loans & Payments");
        } catch (Exception exception) { showError("Navigation Error", "Unable to open the loan portfolio."); }
    }

    @FXML
    private void logout() {
        try {
            NavigationManager.navigate(currentStage(), "/fxml/login.fxml", "LoanFlow - Login", null);
        } catch (Exception exception) { showError("Logout Error", "Unable to return to the login page."); }
    }

    private void replaceScene(Parent root, String title) {
        Stage stage = currentStage();
        stage.getScene().setRoot(root);
        stage.setTitle(title);
        stage.setMaximized(true);
    }

    private Stage currentStage() { return (Stage) welcomeLabel.getScene().getWindow(); }
    private void showError(String title, String message) { showAlert(Alert.AlertType.ERROR, title, message); }
    private void showInformation(String title, String message) { showAlert(Alert.AlertType.INFORMATION, title, message); }
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setContentText(message); AlertUtil.show(alert);
    }
    public static final class EmiRow { final int loanId; final String loanType,status; final BigDecimal emi,due,paid; final java.sql.Date dueDate; EmiRow(int i,String t,BigDecimal e,java.sql.Date d,BigDecimal due,BigDecimal paid,String s){loanId=i;loanType=t;emi=e;dueDate=d;this.due=due;this.paid=paid;status=s;} public int getLoanId(){return loanId;} public String getLoanType(){return loanType;} public BigDecimal getEmi(){return emi;} public java.sql.Date getDueDate(){return dueDate;} public BigDecimal getDue(){return due;} public BigDecimal getPaid(){return paid;} public BigDecimal getRemaining(){return due.subtract(paid).max(BigDecimal.ZERO);} public String getStatus(){return status;} }
    private record ScheduleRow(int no, java.sql.Date date, BigDecimal due, BigDecimal paid, String status) { String value(int n){return switch(n){case 0->String.valueOf(no);case 1->date.toString();case 2->format(due);case 3->format(paid);case 4->format(due.subtract(paid).max(BigDecimal.ZERO));default->status;};} private static String format(BigDecimal v){return NumberFormat.getCurrencyInstance(new Locale("en","IN")).format(v);}}
}
