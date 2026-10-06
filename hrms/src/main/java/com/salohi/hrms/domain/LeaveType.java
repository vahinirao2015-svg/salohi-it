package com.salohi.hrms.domain;

public enum LeaveType {
    CASUAL("Casual"),
    SICK("Sick"),
    EARNED("Earned"),
    UNPAID("Unpaid");

    private final String label;

    LeaveType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean usesAnnualBalance() {
        return this == CASUAL || this == EARNED;
    }
}
