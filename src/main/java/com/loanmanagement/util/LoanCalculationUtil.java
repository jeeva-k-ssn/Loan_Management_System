package com.loanmanagement.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Financial calculations shared by the loan application and repayment flows. */
public final class LoanCalculationUtil {
    private LoanCalculationUtil() { }

    /** Calculates a monthly reducing-balance EMI, rounded to two decimal places. */
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int months) {
        if (principal == null || annualRate == null || principal.signum() <= 0 || annualRate.signum() < 0 || months <= 0) {
            throw new IllegalArgumentException("Principal, rate, and tenure must be valid positive values.");
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(1200), 16, RoundingMode.HALF_UP);
        if (monthlyRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }
        double rate = monthlyRate.doubleValue();
        double factor = Math.pow(1.0 + rate, months);
        return principal.multiply(BigDecimal.valueOf(rate))
                .multiply(BigDecimal.valueOf(factor))
                .divide(BigDecimal.valueOf(factor - 1.0), 2, RoundingMode.HALF_UP);
    }
}
