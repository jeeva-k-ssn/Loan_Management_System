package com.loanmanagement.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loanmanagement.util.PasswordUtil;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Exercises the persisted customer-to-loan-to-payment lifecycle in one
 * transaction. The transaction is always rolled back, so no test records remain.
 */
class LoanFlowEndToEndDatabaseTest {
    @Test
    void disposableLoanLifecycleRollsBackAfterClosureAndNotifications() throws Exception {
        Assumptions.assumeTrue(System.getenv("LMS_DB_PASSWORD") != null,
                "Oracle lifecycle test skipped: LMS_DB_PASSWORD is not configured");

        Connection connection;
        try {
            connection = DatabaseConnection.getConnection();
        } catch (SQLException exception) {
            Assumptions.assumeTrue(false, "Oracle lifecycle test skipped: " + exception.getMessage());
            return;
        }

        String email = "loanflow.e2e." + UUID.randomUUID() + "@example.test";
        try {
            connection.setAutoCommit(false);
            int userId = insertUser(connection, email);
            int customerId = insertCustomer(connection, userId, email);
            int applicationId = insertApplication(connection, customerId);

            update(connection, "UPDATE LOAN_APPLICATION SET STATUS='APPROVED' WHERE APPLICATION_ID=?", applicationId);
            int loanId = insertLoan(connection, applicationId, customerId);
            BigDecimal repayable = new BigDecimal("2000.00");

            insertPayment(connection, loanId, new BigDecimal("1000.00"), "E2E-1");
            assertEquals(new BigDecimal("1000.00"), paid(connection, loanId));
            assertEquals("ACTIVE", text(connection, "SELECT STATUS FROM LOAN WHERE LOAN_ID=?", loanId));

            insertPayment(connection, loanId, new BigDecimal("1000.00"), "E2E-2");
            assertEquals(repayable, paid(connection, loanId));
            update(connection, "UPDATE LOAN SET STATUS='CLOSED', CLOSED_DATE=SYSDATE WHERE LOAN_ID=? AND STATUS='ACTIVE'", loanId);
            assertEquals("CLOSED", text(connection, "SELECT STATUS FROM LOAN WHERE LOAN_ID=?", loanId));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM LOAN WHERE LOAN_ID=? AND STATUS='CLOSED' AND CLOSED_DATE IS NULL", loanId));

            insertNotification(connection, userId, applicationId, "APPLICATION_APPROVED");
            insertNotification(connection, userId, loanId, "PAYMENT_SUCCESS");
            assertEquals(2, scalar(connection, "SELECT COUNT(*) FROM LOANFLOW_NOTIFICATION WHERE USER_ID=?", userId));
            assertEquals(0, scalar(connection, "SELECT COUNT(*) FROM PAYMENT WHERE LOAN_ID=? AND AMOUNT > ?", loanId, repayable));
            assertTrue(scalar(connection, "SELECT COUNT(*) FROM LMS_CUSTOMER WHERE CUSTOMER_ID=?", customerId) == 1);
        } finally {
            if (!connection.isClosed()) {
                connection.rollback();
                connection.close();
            }
        }
    }

    private int insertUser(Connection c, String email) throws SQLException {
        update(c, "INSERT INTO USERS (FULL_NAME,EMAIL,PASSWORD,USER_ROLE,CREATED_AT) VALUES ('LoanFlow E2E',?,?, 'CUSTOMER',SYSDATE)", email, PasswordUtil.hash("temporary-e2e-password"));
        return scalar(c, "SELECT USER_ID FROM USERS WHERE EMAIL=?", email);
    }

    private int insertCustomer(Connection c, int userId, String email) throws SQLException {
        update(c, "INSERT INTO LMS_CUSTOMER (USER_ID,FULL_NAME,EMAIL) VALUES (?, 'LoanFlow E2E', ?)", userId, email);
        return scalar(c, "SELECT CUSTOMER_ID FROM LMS_CUSTOMER WHERE USER_ID=?", userId);
    }

    private int insertApplication(Connection c, int customerId) throws SQLException {
        update(c, "INSERT INTO LOAN_APPLICATION (CUSTOMER_ID,LOAN_AMOUNT,LOAN_PURPOSE,LOAN_TYPE,TENURE_MONTHS,INTEREST_RATE,EMI_AMOUNT,STATUS) VALUES (?,10000,'E2E verification','PERSONAL',2,0,1000,'PENDING')", customerId);
        return scalar(c, "SELECT MAX(APPLICATION_ID) FROM LOAN_APPLICATION WHERE CUSTOMER_ID=?", customerId);
    }

    private int insertLoan(Connection c, int applicationId, int customerId) throws SQLException {
        update(c, "INSERT INTO LOAN (APPLICATION_ID,CUSTOMER_ID,LOAN_AMOUNT,INTEREST_RATE,TENURE_MONTHS,EMI_AMOUNT,START_DATE,STATUS) VALUES (?, ?,10000,0,2,1000,SYSDATE,'ACTIVE')", applicationId, customerId);
        return scalar(c, "SELECT LOAN_ID FROM LOAN WHERE APPLICATION_ID=?", applicationId);
    }

    private void insertPayment(Connection c, int loanId, BigDecimal amount, String reference) throws SQLException {
        update(c, "INSERT INTO PAYMENT (LOAN_ID,AMOUNT,PAYMENT_STATUS,PAYMENT_METHOD,PAYMENT_REFERENCE) VALUES (?,?,'PAID','E2E',?)", loanId, amount, reference);
    }

    private void insertNotification(Connection c, int userId, int relatedId, String type) throws SQLException {
        update(c, "INSERT INTO LOANFLOW_NOTIFICATION (USER_ID,TITLE,MESSAGE,NOTIFICATION_TYPE,RELATED_ID) VALUES (?, 'E2E notification','E2E verification',?,?)", userId, type, relatedId);
    }

    private BigDecimal paid(Connection c, int loanId) throws SQLException {
        try (PreparedStatement p = c.prepareStatement("SELECT NVL(SUM(AMOUNT),0) FROM PAYMENT WHERE LOAN_ID=? AND PAYMENT_STATUS='PAID'")) {
            p.setInt(1, loanId);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getBigDecimal(1).setScale(2); }
        }
    }

    private int scalar(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, values);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        }
    }

    private String text(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            bind(p, values);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getString(1) : null; }
        }
    }

    private void update(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) { bind(p, values); p.executeUpdate(); }
    }

    private void bind(PreparedStatement p, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof BigDecimal decimal) p.setBigDecimal(i + 1, decimal);
            else p.setObject(i + 1, values[i]);
        }
    }
}
