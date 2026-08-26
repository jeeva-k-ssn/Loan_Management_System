package com.loanmanagement.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "system";

    private DatabaseConnection() {
        // Utility class.
    }

    /**
     * Opens a database connection using the locally configured password.
     * The password is deliberately never stored in source control.
     */
    public static Connection getConnection() throws SQLException {
        String password = System.getenv("LMS_DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new SQLException(
                    "Database password is not configured. "
                            + "Set the LMS_DB_PASSWORD environment variable "
                            + "before starting LoanFlow."
            );
        }

        return DriverManager.getConnection(URL, USERNAME, password);
    }
}
