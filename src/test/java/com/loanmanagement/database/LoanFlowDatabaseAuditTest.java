package com.loanmanagement.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/** Read-only Oracle audit for relationships and persisted reporting support. */
class LoanFlowDatabaseAuditTest {
    @Test
    void schemaAndFinancialRelationshipsAreConsistentWhenOracleIsConfigured() throws Exception {
        Assumptions.assumeTrue(System.getenv("LMS_DB_PASSWORD") != null,
                "Oracle audit skipped: LMS_DB_PASSWORD is not configured");

        Connection connection;
        try {
            connection = DatabaseConnection.getConnection();
        } catch (SQLException exception) {
            Assumptions.assumeTrue(false, "Oracle audit skipped: " + exception.getMessage());
            return;
        }

        try (connection) {
            assertEquals(1, scalar(connection,
                    "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='LOANFLOW_NOTIFICATION'"),
                    "Notification migration must be applied");
            assertEquals(1, scalar(connection,
                    "SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='USERS' AND COLUMN_NAME='CREATED_AT'"),
                    "Customer timestamp migration must be applied");
            assertEquals(0, scalar(connection,
                    "SELECT COUNT(*) FROM PAYMENT p WHERE NOT EXISTS (SELECT 1 FROM LOAN l WHERE l.LOAN_ID=p.LOAN_ID)"),
                    "Payments must reference existing loans");
            assertEquals(0, scalar(connection,
                    "SELECT COUNT(*) FROM LOAN l WHERE NOT EXISTS (SELECT 1 FROM LMS_CUSTOMER c WHERE c.CUSTOMER_ID=l.CUSTOMER_ID)"),
                    "Loans must reference existing customers");
            assertEquals(0, scalar(connection,
                    "SELECT COUNT(*) FROM LOAN l WHERE l.STATUS='CLOSED' AND l.EMI_AMOUNT*l.TENURE_MONTHS > NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p WHERE p.LOAN_ID=l.LOAN_ID AND p.PAYMENT_STATUS='PAID'),0) + 0.005"),
                    "Closed loans must not retain an outstanding balance");
        }
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
