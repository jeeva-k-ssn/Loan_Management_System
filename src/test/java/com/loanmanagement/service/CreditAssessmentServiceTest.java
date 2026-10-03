package com.loanmanagement.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CreditAssessmentServiceTest {
    @Test
    void combinesExistingAndRequestedEmiAndRequiresReviewWithoutFabricatedCreditData() {
        var result = CreditAssessmentService.assess(null, null,
                BigDecimal.valueOf(8000), BigDecimal.valueOf(9500),
                BigDecimal.valueOf(120000), 1, 0);

        assertEquals(new BigDecimal("17500"), result.getTotalEmi());
        assertNull(result.getCreditScore());
        assertNull(result.getMonthlyIncome());
        assertEquals(CreditAssessmentService.Eligibility.REQUIRES_REVIEW, result.getEligibility());
    }

    @Test
    void marksKnownLowBurdenProfileEligible() {
        var result = CreditAssessmentService.assess(782, BigDecimal.valueOf(60000),
                BigDecimal.valueOf(8000), BigDecimal.valueOf(9500),
                BigDecimal.valueOf(120000), 1, 6);

        assertEquals(CreditAssessmentService.Eligibility.ELIGIBLE, result.getEligibility());
        assertEquals(new BigDecimal("29.17"), result.getEmiToIncomeRatio());
    }

    @Test
    void marksExcessiveRepaymentBurdenNotEligible() {
        var result = CreditAssessmentService.assess(782, BigDecimal.valueOf(30000),
                BigDecimal.valueOf(12000), BigDecimal.valueOf(8000),
                BigDecimal.valueOf(120000), 1, 6);

        assertEquals(CreditAssessmentService.Eligibility.NOT_ELIGIBLE, result.getEligibility());
    }
}
