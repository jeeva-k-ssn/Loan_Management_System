package com.loanmanagement.dao;

import com.loanmanagement.model.CreditReport;
import java.sql.*;
import java.time.LocalDateTime;

/** Data access boundary for simulated bureau data. It never calculates a score. */
public class CreditReportDAO {
    public CreditReport findByCustomerId(Connection connection, int customerId) throws SQLException {
        String sql = "SELECT CUSTOMER_ID,CIBIL_SCORE,ACTIVE_ACCOUNTS,PAYMENT_HISTORY,CREDIT_UTILIZATION," +
                "RECENT_ENQUIRIES,DEFAULTS,TOTAL_OUTSTANDING,RETRIEVED_AT,SOURCE FROM LOANFLOW_MOCK_CREDIT_REPORT WHERE CUSTOMER_ID=?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                Timestamp timestamp = r.getTimestamp("RETRIEVED_AT");
                return new CreditReport(r.getInt("CUSTOMER_ID"), r.getInt("CIBIL_SCORE"),
                        r.getInt("ACTIVE_ACCOUNTS"), r.getBigDecimal("PAYMENT_HISTORY"),
                        r.getBigDecimal("CREDIT_UTILIZATION"), r.getInt("RECENT_ENQUIRIES"),
                        r.getInt("DEFAULTS"), r.getBigDecimal("TOTAL_OUTSTANDING"),
                        timestamp == null ? null : timestamp.toLocalDateTime(), r.getString("SOURCE"));
            }
        }
    }

    public CreditReport findByApplicationId(Connection connection, int applicationId) throws SQLException {
        String sql = "SELECT CUSTOMER_ID,CIBIL_SCORE,ACTIVE_ACCOUNTS,PAYMENT_HISTORY,CREDIT_UTILIZATION," +
                "RECENT_ENQUIRIES,DEFAULTS,TOTAL_OUTSTANDING,RETRIEVED_AT,SOURCE FROM LOANFLOW_CREDIT_RETRIEVAL WHERE APPLICATION_ID=?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, applicationId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return null;
                return read(r);
            }
        }
    }

    public void saveForApplication(Connection connection, int applicationId, int officerId, CreditReport report) throws SQLException {
        String sql = "INSERT INTO LOANFLOW_CREDIT_RETRIEVAL " +
                "(APPLICATION_ID,CUSTOMER_ID,CIBIL_SCORE,ACTIVE_ACCOUNTS,PAYMENT_HISTORY,CREDIT_UTILIZATION,RECENT_ENQUIRIES,DEFAULTS,TOTAL_OUTSTANDING,SOURCE,RETRIEVED_AT,RETRIEVED_BY) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, applicationId); p.setInt(2, report.customerId()); p.setInt(3, report.cibilScore());
            p.setInt(4, report.activeAccounts()); p.setBigDecimal(5, report.paymentHistory());
            p.setBigDecimal(6, report.creditUtilization()); p.setInt(7, report.recentEnquiries());
            p.setInt(8, report.defaults()); p.setBigDecimal(9, report.totalOutstanding());
            p.setString(10, report.source()); p.setTimestamp(11, Timestamp.valueOf(report.retrievedAt()));
            if (officerId > 0) p.setInt(12, officerId); else p.setNull(12, Types.NUMERIC);
            p.executeUpdate();
        } catch (SQLException duplicate) {
            if (duplicate.getErrorCode() != 1) throw duplicate;
        }
    }

    private CreditReport read(ResultSet r) throws SQLException {
        Timestamp timestamp = r.getTimestamp("RETRIEVED_AT");
        return new CreditReport(r.getInt("CUSTOMER_ID"), r.getInt("CIBIL_SCORE"), r.getInt("ACTIVE_ACCOUNTS"),
                r.getBigDecimal("PAYMENT_HISTORY"), r.getBigDecimal("CREDIT_UTILIZATION"), r.getInt("RECENT_ENQUIRIES"),
                r.getInt("DEFAULTS"), r.getBigDecimal("TOTAL_OUTSTANDING"),
                timestamp == null ? null : timestamp.toLocalDateTime(), r.getString("SOURCE"));
    }
}
