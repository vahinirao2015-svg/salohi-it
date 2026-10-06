package com.salohi.hrms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PayslipCalculator {

    private static final BigDecimal HRA_RATE = new BigDecimal("0.40");
    private static final BigDecimal SPECIAL_RATE = new BigDecimal("0.10");
    private static final BigDecimal PF_RATE = new BigDecimal("0.12");
    private static final BigDecimal PROFESSIONAL_TAX = new BigDecimal("200.00");

    private PayslipCalculator() {
    }

    public static Breakdown calculate(BigDecimal basic, int unpaidDays, int daysInMonth) {
        if (daysInMonth < 1) {
            throw new IllegalArgumentException("A month needs at least one day.");
        }
        BigDecimal perDay = basic.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);
        BigDecimal lossOfPay = perDay.multiply(BigDecimal.valueOf(unpaidDays)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal hra = money(basic.multiply(HRA_RATE));
        BigDecimal special = money(basic.multiply(SPECIAL_RATE));
        BigDecimal gross = basic.add(hra).add(special);
        BigDecimal pf = money(basic.multiply(PF_RATE));
        BigDecimal net = gross.subtract(pf).subtract(PROFESSIONAL_TAX).subtract(lossOfPay);
        if (net.signum() < 0) {
            net = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return new Breakdown(basic, hra, special, pf, PROFESSIONAL_TAX, lossOfPay, unpaidDays, gross, net);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public record Breakdown(
            BigDecimal basic,
            BigDecimal houseRentAllowance,
            BigDecimal specialAllowance,
            BigDecimal providentFund,
            BigDecimal professionalTax,
            BigDecimal lossOfPay,
            int unpaidDays,
            BigDecimal grossPay,
            BigDecimal netPay
    ) {
    }
}
