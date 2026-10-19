package com.loanmanagement.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmiStatusUtilTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final BigDecimal EMI = new BigDecimal("1250.00");

    @Test void identifiesPaidPendingOverdueAndPartialLoansIndependently() {
        assertEquals("Paid", EmiStatusUtil.status(new BigDecimal("1250.00"), EMI, TODAY.minusDays(2), TODAY));
        assertEquals("Partially Paid", EmiStatusUtil.status(new BigDecimal("500.00"), EMI, TODAY.minusDays(2), TODAY));
        assertEquals("Overdue", EmiStatusUtil.status(BigDecimal.ZERO, EMI, TODAY.minusDays(1), TODAY));
        assertEquals("Pending", EmiStatusUtil.status(BigDecimal.ZERO, EMI, TODAY.plusDays(1), TODAY));
    }
}
