package com.loanmanagement.controller;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import com.loanmanagement.navigation.NavigationManager;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.*;
import java.util.List;

/** Notification inbox. JDBC work is performed away from the FX application thread. */
public class NotificationController {
    @FXML private ListView<NotificationRow> notificationList;
    @FXML private Label countLabel;
    private User currentUser;

    public void setCurrentUser(User user) { currentUser = user; loadNotifications(); }

    @FXML public void initialize() {
        notificationList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(NotificationRow row, boolean empty) {
                super.updateItem(row, empty);
                setText(empty || row == null ? null : row.title + "\n" + row.message + "\n" + row.createdAt);
                getStyleClass().removeAll("notification-read", "notification-unread");
                if (!empty && row != null) getStyleClass().add(row.read ? "notification-read" : "notification-unread");
            }
        });
    }

    private void loadNotifications() {
        if (currentUser == null) return;
        countLabel.setText("Loading notifications…");
        Task<List<NotificationRow>> task = new Task<>() {
            @Override protected List<NotificationRow> call() throws SQLException {
                String sql = "SELECT NOTIFICATION_ID,TITLE,MESSAGE,TO_CHAR(CREATED_AT,'DD Mon YYYY HH24:MI') CREATED_AT,IS_READ FROM LOANFLOW_NOTIFICATION WHERE USER_ID=? ORDER BY CREATED_AT DESC,NOTIFICATION_ID DESC";
                try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
                    p.setInt(1, currentUser.getUserId());
                    try (ResultSet r = p.executeQuery()) {
                        var rows = new java.util.ArrayList<NotificationRow>();
                        while (r.next()) rows.add(new NotificationRow(r.getInt(1), r.getString(2), r.getString(3), r.getString(4), "Y".equals(r.getString(5))));
                        return rows;
                    }
                }
            }
        };
        task.setOnSucceeded(e -> {
            var rows = FXCollections.observableArrayList(task.getValue());
            notificationList.setItems(rows);
            countLabel.setText(rows.stream().filter(row -> !row.read).count() + " unread");
        });
        task.setOnFailed(e -> { notificationList.setItems(FXCollections.observableArrayList()); countLabel.setText("Notifications unavailable"); });
        start(task, "loanflow-notifications");
    }

    @FXML private void markAllRead() {
        if (currentUser == null) return;
        countLabel.setText("Updating notifications…");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws SQLException {
                try (Connection c = DatabaseConnection.getConnection(); PreparedStatement p = c.prepareStatement("UPDATE LOANFLOW_NOTIFICATION SET IS_READ='Y' WHERE USER_ID=?")) {
                    p.setInt(1, currentUser.getUserId()); p.executeUpdate(); return null;
                }
            }
        };
        task.setOnSucceeded(e -> loadNotifications());
        task.setOnFailed(e -> countLabel.setText("Unable to update notifications"));
        start(task, "loanflow-notifications-update");
    }

    @FXML private void refresh() { loadNotifications(); }
    @FXML private void back() {
        try {
            NavigationManager.navigate((javafx.stage.Stage) notificationList.getScene().getWindow(), "/fxml/dashboard.fxml", "LoanFlow - Dashboard",
                    controller -> ((DashboardController) controller).setCurrentUser(currentUser));
        } catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Unable to return to the dashboard.").showAndWait(); }
    }

    private void start(Task<?> task, String name) { Thread thread = new Thread(task, name); thread.setDaemon(true); thread.start(); }
    public static class NotificationRow {
        final int id; final String title, message, createdAt; final boolean read;
        NotificationRow(int id, String title, String message, String createdAt, boolean read) { this.id=id; this.title=title; this.message=message; this.createdAt=createdAt; this.read=read; }
    }
}
