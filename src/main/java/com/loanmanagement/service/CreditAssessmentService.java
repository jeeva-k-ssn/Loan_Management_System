package com.loanmanagement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * LoanFlow's academic credit-assessment policy.  It is decision support only;
 * the loan officer still makes the final approval or rejection decision.
 */
public final class CreditAssessmentService {
    private CreditAssessmentService() { }

    public enum Eligibility { ELIGIBLE, REQUIRES_REVIEW, NOT_ELIGIBLE }

    public static final class Assessment {
        private final Integer creditScore;
        private final BigDecimal monthlyIncome;
        private final BigDecimal existingEmi;
        private final BigDecimal newEmi;
        private final BigDecimal totalEmi;
        private final BigDecimal emiToIncomeRatio;
        private final BigDecimal outstandingDebt;
        private final int activeLoanCount;
        private final int paymentCount;
        private final String creditHistory;
        private final Eligibility eligibility;
        private final String risk;
        private final String reason;

        private Assessment(Integer creditScore, BigDecimal monthlyIncome, BigDecimal existingEmi,
                           BigDecimal newEmi, BigDecimal totalEmi, BigDecimal emiToIncomeRatio,
                           BigDecimal outstandingDebt, int activeLoanCount, int paymentCount, String creditHistory,
                           Eligibility eligibility, String risk, String reason) {
            this.creditScore = creditScore;
            this.monthlyIncome = monthlyIncome;
            this.existingEmi = existingEmi;
            this.newEmi = newEmi;
            this.totalEmi = totalEmi;
            this.emiToIncomeRatio = emiToIncomeRatio;
            this.outstandingDebt = outstandingDebt;
            this.activeLoanCount = activeLoanCount;
            this.paymentCount = paymentCount;
            this.creditHistory = creditHistory;
            this.eligibility = eligibility;
            this.risk = risk;
            this.reason = reason;
        }

        public Integer getCreditScore() { return creditScore; }
        public BigDecimal getMonthlyIncome() { return monthlyIncome; }
        public BigDecimal getExistingEmi() { return existingEmi; }
        public BigDecimal getNewEmi() { return newEmi; }
        public BigDecimal getTotalEmi() { return totalEmi; }
        public BigDecimal getEmiToIncomeRatio() { return emiToIncomeRatio; }
        public BigDecimal getOutstandingDebt() { return outstandingDebt; }
        public int getActiveLoanCount() { return activeLoanCount; }
        public int getPaymentCount() { return paymentCount; }
        public String getCreditHistory() { return creditHistory; }
        public Eligibility getEligibility() { return eligibility; }
        public String getRisk() { return risk; }
        public String getReason() { return reason; }
        public String getCreditScoreDisplay() { return creditScore == null ? "NH / Not available" : creditScore.toString(); }
        public String getIncomeDisplay() { return monthlyIncome == null ? "Not available" : money(monthlyIncome); }
        public String getRatioDisplay() { return emiToIncomeRatio == null ? "Not available" : percent(emiToIncomeRatio); }
        public String getEligibilityDisplay() { return eligibility.name().replace('_', ' '); }

        private static String money(BigDecimal value) { return "INR " + value.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
        private static String percent(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%"; }
    }

    /** Assesses a customer using current database relationships and the already calculated new EMI. */
    public static Assessment loadForCustomer(Connection connection, int customerId, BigDecimal newEmi) throws SQLException {
        BigDecimal existingEmi = BigDecimal.ZERO;
        BigDecimal outstandingDebt = BigDecimal.ZERO;
        int activeLoans = 0;
        int payments = 0;
        // Credit scores are supplied only by CreditBureauService. This decision-support
        // service deliberately does not read the legacy manually verified/profile score.
        Integer creditScore = null;
        BigDecimal monthlyIncome = null;
        String creditHistory = "Insufficient credit history";
        String profileSql = "SELECT MONTHLY_INCOME, NVL(CREDIT_HISTORY_STATUS,'INSUFFICIENT'), NVL(VERIFICATION_STATUS,'NOT_VERIFIED') "
                + "FROM LOANFLOW_CREDIT_PROFILE WHERE CUSTOMER_ID=?";
        try (PreparedStatement p = connection.prepareStatement(profileSql)) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    monthlyIncome = r.getBigDecimal(1);
                    String verificationStatus = r.getString(3);
                    creditHistory = "VERIFIED".equalsIgnoreCase(verificationStatus)
                            ? r.getString(2)
                            : "Credit score not verified";
                }
            }
        } catch (SQLException ignored) {
            // The additive migration may not yet be installed; preserve legacy operation.
        }
        String sql = "SELECT NVL(SUM(l.EMI_AMOUNT),0), "
                + "NVL(SUM(GREATEST(l.EMI_AMOUNT*l.TENURE_MONTHS-NVL((SELECT SUM(p.AMOUNT) FROM PAYMENT p WHERE p.LOAN_ID=l.LOAN_ID AND p.PAYMENT_STATUS='PAID'),0),0)),0), "
                + "COUNT(l.LOAN_ID) "
                + "FROM LOAN l WHERE l.CUSTOMER_ID=? AND UPPER(l.STATUS)='ACTIVE'";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    existingEmi = decimal(r.getBigDecimal(1));
                    outstandingDebt = decimal(r.getBigDecimal(2));
                    activeLoans = r.getInt(3);
                }
            }
        }
        String paymentSql = "SELECT COUNT(*) FROM PAYMENT p JOIN LOAN l ON l.LOAN_ID=p.LOAN_ID "
                + "WHERE l.CUSTOMER_ID=? AND p.PAYMENT_STATUS='PAID'";
        try (PreparedStatement p = connection.prepareStatement(paymentSql)) {
            p.setInt(1, customerId);
            try (ResultSet r = p.executeQuery()) { if (r.next()) payments = r.getInt(1); }
        }
        return assess(creditScore, monthlyIncome, existingEmi, newEmi, outstandingDebt, activeLoans, payments, creditHistory);
    }

    /** Loads the assessment for the exact application selected by the officer. */
    public static Assessment loadForApplication(Connection connection, int applicationId) throws SQLException {
        String sql = "SELECT CUSTOMER_ID, EMI_AMOUNT FROM LOAN_APPLICATION WHERE APPLICATION_ID=?";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, applicationId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) throw new SQLException("Application not found: " + applicationId);
                return loadForCustomer(connection, r.getInt(1), decimal(r.getBigDecimal(2)));
            }
        }
    }

    /** Centralized configurable policy. Missing income/score/history requires human review. */
    public static Assessment assess(Integer creditScore, BigDecimal monthlyIncome, BigDecimal existingEmi,
                                    BigDecimal newEmi, BigDecimal outstandingDebt, int activeLoanCount,
                                    int paymentCount) {
        return assess(creditScore, monthlyIncome, existingEmi, newEmi, outstandingDebt, activeLoanCount,
                paymentCount, paymentCount == 0 ? "Insufficient credit history" : "Repayment history available");
    }

    public static Assessment assess(Integer creditScore, BigDecimal monthlyIncome, BigDecimal existingEmi,
                                    BigDecimal newEmi, BigDecimal outstandingDebt, int activeLoanCount,
                                    int paymentCount, String creditHistory) {
        BigDecimal oldEmi = decimal(existingEmi);
        BigDecimal requestedEmi = decimal(newEmi);
        BigDecimal total = oldEmi.add(requestedEmi);
        BigDecimal ratio = monthlyIncome == null || monthlyIncome.signum() <= 0
                ? null : total.multiply(BigDecimal.valueOf(100)).divide(monthlyIncome, 2, RoundingMode.HALF_UP);

        Eligibility result;
        String risk;
        String reason;
        if (requestedEmi.signum() <= 0 || (ratio != null && ratio.compareTo(BigDecimal.valueOf(50)) > 0)
                || (creditScore != null && creditScore < 600)) {
            result = Eligibility.NOT_ELIGIBLE;
            risk = "HIGH";
            reason = "Configured LoanFlow limits indicate excessive repayment burden or weak credit data.";
        } else if (creditScore == null || monthlyIncome == null || paymentCount == 0) {
            result = Eligibility.REQUIRES_REVIEW;
            risk = activeLoanCount > 0 ? "MEDIUM" : "REVIEW";
            reason = "Income, score, or sufficient repayment history is not available; officer verification is required.";
        } else if (ratio.compareTo(BigDecimal.valueOf(35)) <= 0 && creditScore >= 700) {
            result = Eligibility.ELIGIBLE;
            risk = "LOW";
            reason = "Configured LoanFlow score and repayment-burden checks are within policy limits.";
        } else {
            result = Eligibility.REQUIRES_REVIEW;
            risk = "MEDIUM";
            reason = "The application is within broad limits but needs officer review of the complete credit profile.";
        }
        return new Assessment(creditScore, monthlyIncome, oldEmi, requestedEmi, total, ratio,
                decimal(outstandingDebt), activeLoanCount, paymentCount, creditHistory, result, risk, reason);
    }

    /** Stores the exact decision-support snapshot used for an application. */
    public static void saveAssessment(Connection connection, int applicationId, Assessment a) throws SQLException {
        String sql = "MERGE INTO LOANFLOW_CREDIT_ASSESSMENT target USING (SELECT ? APPLICATION_ID, ? CREDIT_SCORE, ? MONTHLY_INCOME, ? EXISTING_EMI, ? NEW_EMI, ? TOTAL_EMI, ? EMI_INCOME_RATIO, ? OUTSTANDING_DEBT, ? ELIGIBILITY, ? RISK_LEVEL, ? CREDIT_HISTORY, ? REASON FROM DUAL) source "
                + "ON (target.APPLICATION_ID=source.APPLICATION_ID) WHEN MATCHED THEN UPDATE SET "
                + "CREDIT_SCORE=source.CREDIT_SCORE, MONTHLY_INCOME=source.MONTHLY_INCOME, EXISTING_EMI=source.EXISTING_EMI, NEW_EMI=source.NEW_EMI, TOTAL_EMI=source.TOTAL_EMI, EMI_INCOME_RATIO=source.EMI_INCOME_RATIO, OUTSTANDING_DEBT=source.OUTSTANDING_DEBT, ELIGIBILITY=source.ELIGIBILITY, RISK_LEVEL=source.RISK_LEVEL, CREDIT_HISTORY=source.CREDIT_HISTORY, REASON=source.REASON, ASSESSED_AT=SYSDATE "
                + "WHEN NOT MATCHED THEN INSERT (APPLICATION_ID,CREDIT_SCORE,MONTHLY_INCOME,EXISTING_EMI,NEW_EMI,TOTAL_EMI,EMI_INCOME_RATIO,OUTSTANDING_DEBT,ELIGIBILITY,RISK_LEVEL,CREDIT_HISTORY,REASON) VALUES (source.APPLICATION_ID,source.CREDIT_SCORE,source.MONTHLY_INCOME,source.EXISTING_EMI,source.NEW_EMI,source.TOTAL_EMI,source.EMI_INCOME_RATIO,source.OUTSTANDING_DEBT,source.ELIGIBILITY,source.RISK_LEVEL,source.CREDIT_HISTORY,source.REASON)";
        try (PreparedStatement p = connection.prepareStatement(sql)) {
            p.setInt(1, applicationId); setNullableInt(p, 2, a.creditScore); setNullableDecimal(p, 3, a.monthlyIncome);
            p.setBigDecimal(4, a.existingEmi); p.setBigDecimal(5, a.newEmi); p.setBigDecimal(6, a.totalEmi);
            setNullableDecimal(p, 7, a.emiToIncomeRatio); p.setBigDecimal(8, a.outstandingDebt);
            p.setString(9, a.eligibility.name()); p.setString(10, a.risk); p.setString(11, a.creditHistory); p.setString(12, a.reason);
            p.executeUpdate();
        }
    }

    private static void setNullableDecimal(PreparedStatement p, int index, BigDecimal value) throws SQLException { if (value == null) p.setNull(index, java.sql.Types.NUMERIC); else p.setBigDecimal(index, value); }
    private static void setNullableInt(PreparedStatement p, int index, Integer value) throws SQLException { if (value == null) p.setNull(index, java.sql.Types.NUMERIC); else p.setInt(index, value); }

    private static BigDecimal decimal(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
}
