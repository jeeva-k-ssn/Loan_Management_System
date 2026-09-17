package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Database-backed, role-scoped in-app notifications. */
public final class NotificationService {
    private NotificationService() { }

    public static void create(int userId, String title, String message, String type, Integer relatedId) {
        String sql = "INSERT INTO LOANFLOW_NOTIFICATION (USER_ID,TITLE,MESSAGE,NOTIFICATION_TYPE,RELATED_ID) VALUES (?,?,?,?,?)";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId); statement.setString(2, title); statement.setString(3, message); statement.setString(4, type);
            if (relatedId == null) statement.setNull(5, java.sql.Types.NUMERIC); else statement.setInt(5, relatedId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // Notifications must never prevent a successful loan or payment transaction.
        }
    }
}