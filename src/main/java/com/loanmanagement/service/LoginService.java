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

    String sql =
            "INSERT INTO USERS " +
            "(FULL_NAME, EMAIL, PASSWORD, USER_ROLE) " +
            "VALUES (?, ?, ?, 'CUSTOMER')";

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

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
    if (exception.getMessage() != null
            && exception.getMessage().contains("LMS_DB_PASSWORD")) {
        return exception.getMessage();
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
