package com.salohi.hrms.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollRulesTest {

    @Test
    void inclusiveLeaveDaysCountBothEnds() {
        assertEquals(4, LeaveDates.inclusiveDays(LocalDate.of(2026, 3, 30), LocalDate.of(2026, 4, 2)));
    }

    @Test
    void unpaidOverlapIsClippedToThePayMonth() {
        int april = LeaveDates.overlapDays(
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 2),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30));
        assertEquals(2, april);
    }

    @Test
    void payslipUsesSalohiAllowancesAndDeductions() {
        PayslipCalculator.Breakdown slip = PayslipCalculator.calculate(new BigDecimal("75000.00"), 0, 30);
        assertEquals(new BigDecimal("30000.00"), slip.houseRentAllowance());
        assertEquals(new BigDecimal("7500.00"), slip.specialAllowance());
        assertEquals(new BigDecimal("9000.00"), slip.providentFund());
        assertEquals(new BigDecimal("200.00"), slip.professionalTax());
        assertEquals(new BigDecimal("103300.00"), slip.netPay());
    }

    @Test
    void unpaidDaysReduceNetPay() {
        PayslipCalculator.Breakdown slip = PayslipCalculator.calculate(new BigDecimal("30000.00"), 2, 30);
        assertEquals(new BigDecimal("2000.00"), slip.lossOfPay());
        assertEquals(new BigDecimal("39200.00"), slip.netPay());
    }
}
