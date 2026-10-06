package com.salohi.hrms.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@Component("fmt")
public class Formatters {

    private static final Locale INDIA = Locale.forLanguageTag("en-IN");

    public String month(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public String inr(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getCurrencyInstance(INDIA);
        return format.format(amount);
    }
}
