package com.loanmanagement.service;

import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LoginService {

    public boolean registerUser(User user) {

        String sql = "INSERT INTO USERS (FULL_NAME, EMAIL, PASSWORD) VALUES (?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());

            int rows = statement.executeUpdate();

            return rows > 0;

        } catch (SQLException e) {

            e.printStackTrace();
            return false;
        }
    }

    public boolean loginUser(String email, String password) {

    String sql = "SELECT * FROM USERS WHERE EMAIL = ? AND PASSWORD = ?";

    try (
            Connection connection = DatabaseConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)
    ) {

        statement.setString(1, email);
        statement.setString(2, password);

        return statement.executeQuery().next();

    } catch (SQLException e) {

        e.printStackTrace();
        return false;
    }
}
}