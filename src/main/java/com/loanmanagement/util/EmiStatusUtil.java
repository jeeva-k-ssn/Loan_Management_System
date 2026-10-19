package com.loanmanagement.util;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pure installment-status rules shared by the tracker and its tests. */
public final class EmiStatusUtil {
    private EmiStatusUtil() { }

    public static String status(BigDecimal paid, BigDecimal due, LocalDate dueDate, LocalDate today) {
        if (paid.compareTo(due) >= 0) return "Paid";
        if (paid.signum() > 0) return "Partially Paid";
        return dueDate.isBefore(today) ? "Overdue" : "Pending";
    }
}
