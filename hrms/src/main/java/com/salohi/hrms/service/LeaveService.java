package com.salohi.hrms.service;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.domain.LeaveRequest;
import com.salohi.hrms.domain.LeaveStatus;
import com.salohi.hrms.repo.LeaveRequestRepository;
import com.salohi.hrms.web.dto.LeaveForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaves;
    private final EmployeeService employees;
    private final Clock clock;

    public LeaveService(LeaveRequestRepository leaves, EmployeeService employees, Clock clock) {
        this.leaves = leaves;
        this.employees = employees;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> listFor(Long employeeId, boolean admin) {
        if (admin) {
            return leaves.findAllByOrderByAppliedAtDesc();
        }
        return leaves.findByEmployeeIdOrderByAppliedAtDesc(employeeId);
    }

    @Transactional
    public LeaveRequest apply(Long employeeId, LeaveForm form) {
        if (form.getEndDate().isBefore(form.getStartDate())) {
            throw new DomainException("The end date must be on or after the start date.");
        }
        int days = LeaveDates.inclusiveDays(form.getStartDate(), form.getEndDate());
        if (days > 31) {
            throw new DomainException("A single request can cover at most 31 days.");
        }
        long overlap = leaves.countOverlapping(
                employeeId,
                EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED),
                form.getStartDate(),
                form.getEndDate());
        if (overlap > 0) {
            throw new DomainException("Those dates overlap a pending or approved request.");
        }
        Employee employee = employees.get(employeeId);
        if (form.getLeaveType().usesAnnualBalance() && days > employee.getAnnualLeaveBalance()) {
            throw new DomainException("Only " + employee.getAnnualLeaveBalance() + " annual days are left.");
        }
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setLeaveType(form.getLeaveType());
        request.setStartDate(form.getStartDate());
        request.setEndDate(form.getEndDate());
        request.setReason(form.getReason().trim());
        request.setStatus(LeaveStatus.PENDING);
        request.setAppliedAt(LocalDateTime.now(clock));
        return leaves.save(request);
    }

    @Transactional
    public void approve(Long leaveId, Long reviewerId) {
        LeaveRequest request = requirePending(leaveId);
        int days = LeaveDates.inclusiveDays(request.getStartDate(), request.getEndDate());
        Employee employee = request.getEmployee();
        if (request.getLeaveType().usesAnnualBalance()) {
            if (days > employee.getAnnualLeaveBalance()) {
                throw new DomainException("Not enough annual leave balance to approve this request.");
            }
            employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() - days);
        }
        request.setStatus(LeaveStatus.APPROVED);
        request.setReviewer(employees.get(reviewerId));
        request.setReviewedAt(LocalDateTime.now(clock));
    }

    @Transactional
    public void reject(Long leaveId, Long reviewerId) {
        LeaveRequest request = requirePending(leaveId);
        request.setStatus(LeaveStatus.REJECTED);
        request.setReviewer(employees.get(reviewerId));
        request.setReviewedAt(LocalDateTime.now(clock));
    }

    private LeaveRequest requirePending(Long leaveId) {
        LeaveRequest request = leaves.findById(leaveId).orElseThrow(() -> new DomainException("Leave request not found."));
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new DomainException("Only a pending request can be reviewed.");
        }
        return request;
    }
}
