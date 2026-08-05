package com.loanmanagement.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Oracle Database URL
    private static final String URL =
            "jdbc:oracle:thin:@localhost:1521:XE";

    // Oracle Username
    private static final String USERNAME = "system";

    // Oracle Password
    private static final String PASSWORD = "Jeeva_Krishnan2505";

    public static Connection getConnection() {

        try {

            Connection connection =
                    DriverManager.getConnection(URL, USERNAME, PASSWORD);

            System.out.println("Database Connected Successfully!");

            return connection;

        } catch (SQLException e) {

            System.out.println("Database Connection Failed!");

            e.printStackTrace();

            return null;
        }
    }

}