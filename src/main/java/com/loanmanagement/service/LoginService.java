package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;

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

    Connection connection = null;

    try {

        connection = DatabaseConnection.getConnection();

        if (connection == null) {
            return false;
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());

            return statement.executeUpdate() > 0;
        }

    } catch (SQLException e) {

        e.printStackTrace();
        return false;

    } finally {

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }
}

public User loginUser(String email, String password, String role) {

    String sql =
            "SELECT USER_ID, FULL_NAME, EMAIL, PASSWORD, USER_ROLE " +
            "FROM USERS " +
            "WHERE EMAIL = ? " +
            "AND PASSWORD = ? " +
            "AND USER_ROLE = ?";

    Connection connection = null;

    try {

        connection = DatabaseConnection.getConnection();

        if (connection == null) {
            return null;
        }

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);
            statement.setString(2, password);
            statement.setString(3, role);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    return new User(
                            result.getInt("USER_ID"),
                            result.getString("FULL_NAME"),
                            result.getString("EMAIL"),
                            result.getString("PASSWORD"),
                            result.getString("USER_ROLE")
                    );
                }
            }
        }

    } catch (SQLException e) {

        e.printStackTrace();

    } finally {

        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    return null;
}


}
