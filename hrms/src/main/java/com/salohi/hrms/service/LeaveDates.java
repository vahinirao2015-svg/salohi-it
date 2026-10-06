package com.salohi.hrms.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class LeaveDates {

    private LeaveDates() {
    }

    public static int inclusiveDays(LocalDate start, LocalDate end) {
        return (int) ChronoUnit.DAYS.between(start, end) + 1;
    }

    public static int overlapDays(LocalDate start, LocalDate end, LocalDate windowStart, LocalDate windowEnd) {
        LocalDate from = start.isAfter(windowStart) ? start : windowStart;
        LocalDate to = end.isBefore(windowEnd) ? end : windowEnd;
        if (to.isBefore(from)) {
            return 0;
        }
        return inclusiveDays(from, to);
    }
}
