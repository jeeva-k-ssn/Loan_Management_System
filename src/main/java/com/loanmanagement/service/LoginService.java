package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;
import com.loanmanagement.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginService {


public boolean registerUser(User user) {

    String sql =
            "INSERT INTO USERS " +
            "(FULL_NAME, EMAIL, PASSWORD, USER_ROLE) " +
            "VALUES (?, ?, ?, 'CUSTOMER')";

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setString(1, user.getFullName());
        statement.setString(2, user.getEmail());
        statement.setString(3, PasswordUtil.hash(user.getPassword()));

        return statement.executeUpdate() > 0;

    } catch (SQLException e) {
        return false;
    }
}

public User loginUser(String email, String password, String role) {

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
        return null;
    }

    return null;
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
