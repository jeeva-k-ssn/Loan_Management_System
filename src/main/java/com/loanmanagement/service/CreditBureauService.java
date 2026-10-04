package com.loanmanagement.service;

import com.loanmanagement.dao.CreditReportDAO;
import com.loanmanagement.database.DatabaseConnection;
import com.loanmanagement.model.CreditReport;
import java.sql.Connection;
import java.sql.SQLException;

/** Simulates a bureau integration by retrieving a stable report stored in Oracle. */
public class CreditBureauService {
    private final CreditReportDAO reportDAO;
    public CreditBureauService() { this(new CreditReportDAO()); }
    public CreditBureauService(CreditReportDAO reportDAO) { this.reportDAO = reportDAO; }

    public CreditReport getCreditReport(int customerId) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return getCreditReport(connection, customerId);
        }
    }

    public CreditReport getCreditReport(Connection connection, int customerId) throws SQLException {
        if (customerId <= 0) throw new IllegalArgumentException("A valid customer is required.");
        return reportDAO.findByCustomerId(connection, customerId);
    }

    /** Returns the immutable application snapshot if already fetched; otherwise records one fetch. */
    public CreditReport getCreditReport(Connection connection, int customerId, int applicationId, int officerId) throws SQLException {
        CreditReport existing = reportDAO.findByApplicationId(connection, applicationId);
        if (existing != null) return existing;
        CreditReport report = getCreditReport(connection, customerId);
        if (report == null) return null;
        reportDAO.saveForApplication(connection, applicationId, officerId, report);
        return reportDAO.findByApplicationId(connection, applicationId);
    }
}
