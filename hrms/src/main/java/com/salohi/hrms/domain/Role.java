package com.salohi.hrms.domain;

public enum Role {
    ADMIN("Admin"),
    EMPLOYEE("Employee");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
