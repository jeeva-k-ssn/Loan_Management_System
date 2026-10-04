package com.loanmanagement.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Immutable report returned by LoanFlow's simulated external credit bureau. */
public record CreditReport(int customerId, int cibilScore, int activeAccounts,
                           BigDecimal paymentHistory, BigDecimal creditUtilization,
                           int recentEnquiries, int defaults, BigDecimal totalOutstanding,
                           LocalDateTime retrievedAt, String source) {
    public String riskClassification() {
        return cibilScore >= 750 ? "LOW" : cibilScore >= 650 ? "MEDIUM" : "HIGH";
    }
}
