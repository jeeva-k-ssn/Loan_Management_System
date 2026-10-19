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

    /** Sends the same notification to every account holding one of the supplied roles. */
    public static void createForRoles(String title, String message, String type, Integer relatedId, String... roles) {
        if (roles == null || roles.length == 0) return;
        String placeholders = String.join(",", java.util.Collections.nCopies(roles.length, "?"));
        String findUsers = "SELECT USER_ID FROM USERS WHERE USER_ROLE IN (" + placeholders + ")";
        String insert = "INSERT INTO LOANFLOW_NOTIFICATION (USER_ID,TITLE,MESSAGE,NOTIFICATION_TYPE,RELATED_ID) VALUES (?,?,?,?,?)";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement users = connection.prepareStatement(findUsers);
             PreparedStatement notification = connection.prepareStatement(insert)) {
            for (int i = 0; i < roles.length; i++) users.setString(i + 1, roles[i]);
            try (java.sql.ResultSet result = users.executeQuery()) {
                while (result.next()) {
                    notification.setInt(1, result.getInt("USER_ID"));
                    notification.setString(2, title);
                    notification.setString(3, message);
                    notification.setString(4, type);
                    if (relatedId == null) notification.setNull(5, java.sql.Types.NUMERIC);
                    else notification.setInt(5, relatedId);
                    notification.addBatch();
                }
            }
            notification.executeBatch();
        } catch (SQLException ignored) {
            // Notifications must never prevent the originating business action.
        }
    }
}
