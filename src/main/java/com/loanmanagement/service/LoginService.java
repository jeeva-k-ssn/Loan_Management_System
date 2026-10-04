package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import com.loanmanagement.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {

private String lastErrorMessage;

public String getLastErrorMessage() {
    return lastErrorMessage;
}

public boolean registerUser(User user) {

    lastErrorMessage = null;

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement statement = connection.prepareStatement(userInsertSql(connection))) {

        connection.setAutoCommit(false);

        statement.setString(1, user.getFullName());
        statement.setString(2, user.getEmail());
        statement.setString(3, PasswordUtil.hash(user.getPassword()));

        if (statement.executeUpdate() != 1) {
            connection.rollback();
            return false;
        }

        String customerSql =
                "INSERT INTO LMS_CUSTOMER (USER_ID, FULL_NAME, EMAIL) "
                        + "SELECT USER_ID, FULL_NAME, EMAIL FROM USERS WHERE EMAIL = ?";

        try (PreparedStatement customerStatement =
                     connection.prepareStatement(customerSql)) {
            customerStatement.setString(1, user.getEmail());

            if (customerStatement.executeUpdate() != 1) {
                connection.rollback();
                lastErrorMessage = "Your customer profile could not be created.";
                return false;
            }
        }

        connection.commit();
        return true;

    } catch (SQLException e) {
        lastErrorMessage = toUserMessage(e);
        return false;
    }
}

public boolean resetPassword(String email, String role, String newPassword) {

    lastErrorMessage = null;

    String sql = "UPDATE USERS SET PASSWORD = ? WHERE EMAIL = ? AND USER_ROLE = ?";

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setString(1, PasswordUtil.hash(newPassword));
        statement.setString(2, email);
        statement.setString(3, role);

        if (statement.executeUpdate() == 1) {
            return true;
        }

        lastErrorMessage = "No account was found for that email and role.";
        return false;
    } catch (SQLException e) {
        lastErrorMessage = toUserMessage(e);
        return false;
    }
}

private String userInsertSql(Connection connection) throws SQLException {
    String metadataSql =
            "SELECT COUNT(*) FROM USER_TAB_COLUMNS " +
            "WHERE TABLE_NAME='USERS' AND COLUMN_NAME='CREATED_AT'";
    try (PreparedStatement metadata = connection.prepareStatement(metadataSql);
         ResultSet result = metadata.executeQuery()) {
        boolean hasCreatedAt = result.next() && result.getInt(1) > 0;
        return hasCreatedAt
                ? "INSERT INTO USERS (FULL_NAME, EMAIL, PASSWORD, USER_ROLE, CREATED_AT) VALUES (?, ?, ?, 'CUSTOMER', SYSDATE)"
                : "INSERT INTO USERS (FULL_NAME, EMAIL, PASSWORD, USER_ROLE) VALUES (?, ?, ?, 'CUSTOMER')";
    }
}

public User loginUser(String email, String password, String role) {

    lastErrorMessage = null;

    String sql =
            "SELECT USER_ID, FULL_NAME, EMAIL, PASSWORD, USER_ROLE " +
            "FROM USERS WHERE EMAIL = ? AND USER_ROLE = ?";

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setString(1, email);
        statement.setString(2, role);

        try (ResultSet result = statement.executeQuery()) {

            if (result.next()) {
                String storedPassword = result.getString("PASSWORD");

                if (!PasswordUtil.matches(password, storedPassword)) {
                    return null;
                }

                if (!PasswordUtil.isHashed(storedPassword)) {
                    upgradeLegacyPassword(connection, result.getInt("USER_ID"), password);
                }

                return new User(
                        result.getInt("USER_ID"),
                        result.getString("FULL_NAME"),
                        result.getString("EMAIL"),
                        null,
                        result.getString("USER_ROLE")
                );
            }
        }

    } catch (SQLException e) {
        lastErrorMessage = toUserMessage(e);
        return null;
    }

    return null;
}

private String toUserMessage(SQLException exception) {
    String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase();
    if (message.contains("ora-00001") || message.contains("unique constraint")) {
        return "An account with this email already exists. Please use a different email or return to login.";
    }
    if (message.contains("lms_db_password")) {
        return "LoanFlow is not configured for database access. Please contact the administrator.";
    }
    if (message.contains("ora-01017") || message.contains("invalid username/password")) {
        return "LoanFlow could not authenticate with the database. Please contact the administrator.";
    }
    return "LoanFlow could not connect to the database. Please try again later.";
}

private void upgradeLegacyPassword(Connection connection, int userId, String password)
        throws SQLException {
    String update = "UPDATE USERS SET PASSWORD = ? WHERE USER_ID = ?";

    try (PreparedStatement statement = connection.prepareStatement(update)) {
        statement.setString(1, PasswordUtil.hash(password));
        statement.setInt(2, userId);
        statement.executeUpdate();
    }
}


}
