package com.loanmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class LoanCalculationUtilTest {
    @Test
    void calculatesZeroInterestLoan() {
        assertEquals(new BigDecimal("1000.00"),
                LoanCalculationUtil.calculateEmi(new BigDecimal("12000"), BigDecimal.ZERO, 12));
    }

    @Test
    void calculatesReducingBalanceLoan() {
        assertEquals(new BigDecimal("881.49"),
                LoanCalculationUtil.calculateEmi(new BigDecimal("10000"), new BigDecimal("10.5"), 12));
    }

    @Test
    void rejectsInvalidInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> LoanCalculationUtil.calculateEmi(BigDecimal.ZERO, BigDecimal.TEN, 12));
        assertThrows(IllegalArgumentException.class,
                () -> LoanCalculationUtil.calculateEmi(BigDecimal.TEN, BigDecimal.TEN, 0));
    }
}
