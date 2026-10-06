package com.salohi.hrms.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class PayslipForm {

    @NotNull
    private Long employeeId;

    @NotNull
    @Min(2020)
    @Max(2100)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
