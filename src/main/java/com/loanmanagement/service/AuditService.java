package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Small audit boundary for material officer/admin actions. */
public final class AuditService {
    private AuditService() { }

    public static void log(int userId, String action, String entityType, int entityId, String details) {
        String sql = "INSERT INTO LOANFLOW_AUDIT_LOG (USER_ID,ACTION,ENTITY_TYPE,ENTITY_ID,DETAILS) VALUES (?,?,?,?,?)";
        try (Connection connection = DatabaseConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId); statement.setString(2, action); statement.setString(3, entityType);
            statement.setInt(4, entityId); statement.setString(5, details); statement.executeUpdate();
        } catch (SQLException ignored) {
            // Auditing must never prevent an approved/rejected loan action from completing.
        }
    }
}
