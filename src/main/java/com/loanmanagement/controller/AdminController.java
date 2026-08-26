package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/** Restricted administrative user and application monitoring workspace. */
public class AdminController {
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, Integer> idColumn;
    @FXML private TableColumn<UserRow, String> nameColumn, emailColumn, roleColumn;
    @FXML private Label selectedLabel;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button saveButton;
    @FXML private TableView<ApplicationRow> applicationTable;
    @FXML private TableColumn<ApplicationRow, Integer> applicationIdColumn;
    @FXML private TableColumn<ApplicationRow, String> applicationCustomerColumn, applicationTypeColumn, applicationDateColumn, applicationStatusColumn;
    @FXML private TableColumn<ApplicationRow, Double> applicationAmountColumn;
    private User currentUser;

    @FXML public void initialize() {
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
        roleCombo.setItems(FXCollections.observableArrayList("CUSTOMER", "LOAN_OFFICER", "ADMIN"));
        userTable.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> select(b));
        saveButton.setDisable(true);
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
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            var rows = FXCollections.<UserRow>observableArrayList();
            while (r.next()) rows.add(new UserRow(r.getInt(1), r.getString(2), r.getString(3), r.getString(4)));
            userTable.setItems(rows);
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Users unavailable", "Unable to load users."); }
    }

    private void loadApplications() {
        String sql = "SELECT la.APPLICATION_ID,c.FULL_NAME,NVL(la.LOAN_TYPE,'-') LOAN_TYPE,la.LOAN_AMOUNT,TO_CHAR(la.APPLICATION_DATE,'DD Mon YYYY') APPLICATION_DATE,la.STATUS "
                + "FROM LOAN_APPLICATION la JOIN LMS_CUSTOMER c ON c.CUSTOMER_ID=la.CUSTOMER_ID ORDER BY la.APPLICATION_DATE DESC,la.APPLICATION_ID DESC";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql); ResultSet r = p.executeQuery()) {
            var rows = FXCollections.<ApplicationRow>observableArrayList();
            while (r.next()) rows.add(new ApplicationRow(r.getInt("APPLICATION_ID"), r.getString("FULL_NAME"), r.getString("LOAN_TYPE"), r.getDouble("LOAN_AMOUNT"), r.getString("APPLICATION_DATE"), r.getString("STATUS")));
            applicationTable.setItems(rows);
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Applications unavailable", "Unable to load applications."); }
    }

    private void select(UserRow row) {
        if (row == null) { selectedLabel.setText("No user selected"); saveButton.setDisable(true); return; }
        selectedLabel.setText("Selected: " + row.name); roleCombo.setValue(row.role); saveButton.setDisable(row.id == currentUser.getUserId());
    }

    @FXML private void saveRole() {
        if (!isAdmin()) return;
        UserRow row = userTable.getSelectionModel().getSelectedItem();
        if (row == null || roleCombo.getValue() == null) return;
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE USERS SET USER_ROLE=? WHERE USER_ID=?")) {
            p.setString(1, roleCombo.getValue()); p.setInt(2, row.id); p.executeUpdate(); loadUsers();
            alert(Alert.AlertType.INFORMATION, "Role updated", "The user role was updated.");
        } catch (SQLException e) { alert(Alert.AlertType.ERROR, "Update failed", "Unable to update the user role."); }
    }

    @FXML private void refresh() { if (isAdmin()) { loadUsers(); loadApplications(); } }
    @FXML private void back() {
        try {
            FXMLLoader l = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml")); Parent root = l.load();
            l.<DashboardController>getController().setCurrentUser(currentUser); Stage s = (Stage) userTable.getScene().getWindow();
            s.setScene(new Scene(root, 1400, 850)); s.setMaximized(true);
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
}
