package com.salohi.hrms.service;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.domain.LeaveRequest;
import com.salohi.hrms.domain.Payslip;
import com.salohi.hrms.repo.LeaveRequestRepository;
import com.salohi.hrms.repo.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
public class PayslipService {

    private final PayslipRepository payslips;
    private final EmployeeService employees;
    private final LeaveRequestRepository leaves;
    private final Clock clock;

    public PayslipService(
            PayslipRepository payslips,
            EmployeeService employees,
            LeaveRequestRepository leaves,
            Clock clock) {
        this.payslips = payslips;
        this.employees = employees;
        this.leaves = leaves;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Payslip> list(Long employeeId, boolean admin) {
        if (admin) {
            return payslips.findAllByOrderByPayYearDescPayMonthDesc();
        }
        return payslips.findByEmployeeIdOrderByPayYearDescPayMonthDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public Payslip get(Long id, Long viewerId, boolean admin) {
        Payslip payslip = payslips.findById(id).orElseThrow(() -> new DomainException("Payslip not found."));
        if (!admin && !payslip.getEmployee().getId().equals(viewerId)) {
            throw new DomainException("You can only open your own payslips.");
        }
        return payslip;
    }

    @Transactional
    public Payslip generate(Long employeeId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new DomainException("Choose a month from 1 to 12.");
        }
        Employee employee = employees.get(employeeId);
        if (!employee.isActive()) {
            throw new DomainException("Payslips are generated for active employees.");
        }
        if (payslips.findByEmployeeIdAndPayYearAndPayMonth(employeeId, year, month).isPresent()) {
            throw new DomainException("A payslip for that month already exists.");
        }
        YearMonth period = YearMonth.of(year, month);
        int unpaidDays = unpaidDays(employeeId, period);
        PayslipCalculator.Breakdown breakdown = PayslipCalculator.calculate(
                employee.getBasicSalary(),
                unpaidDays,
                period.lengthOfMonth());
        Payslip payslip = new Payslip();
        payslip.setEmployee(employee);
        payslip.setPayYear(year);
        payslip.setPayMonth(month);
        payslip.setBasicSalary(breakdown.basic());
        payslip.setHouseRentAllowance(breakdown.houseRentAllowance());
        payslip.setSpecialAllowance(breakdown.specialAllowance());
        payslip.setProvidentFund(breakdown.providentFund());
        payslip.setProfessionalTax(breakdown.professionalTax());
        payslip.setLossOfPay(breakdown.lossOfPay());
        payslip.setUnpaidDays(breakdown.unpaidDays());
        payslip.setGrossPay(breakdown.grossPay());
        payslip.setNetPay(breakdown.netPay());
        payslip.setGeneratedAt(LocalDateTime.now(clock));
        return payslips.save(payslip);
    }

    private int unpaidDays(Long employeeId, YearMonth period) {
        LocalDate start = period.atDay(1);
        LocalDate end = period.atEndOfMonth();
        int days = 0;
        for (LeaveRequest leave : leaves.findApprovedUnpaidOverlapping(employeeId, start, end)) {
            days += LeaveDates.overlapDays(leave.getStartDate(), leave.getEndDate(), start, end);
        }
        return days;
    }
}
