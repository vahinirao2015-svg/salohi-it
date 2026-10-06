package com.salohi.hrms.service;

import com.salohi.hrms.domain.AttendanceRecord;
import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.repo.AttendanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final EmployeeService employees;
    private final Clock clock;

    public AttendanceService(AttendanceRepository attendance, EmployeeService employees, Clock clock) {
        this.attendance = attendance;
        this.employees = employees;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecord> list(Long employeeId, boolean admin, LocalDate day) {
        if (admin) {
            return attendance.findByWorkDateOrderByCheckInAsc(day);
        }
        return attendance.findByEmployeeIdOrderByWorkDateDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public Optional<AttendanceRecord> today(Long employeeId) {
        return attendance.findByEmployeeIdAndWorkDate(employeeId, LocalDate.now(clock));
    }

    @Transactional
    public void checkIn(Long employeeId) {
        LocalDate today = LocalDate.now(clock);
        AttendanceRecord record = attendance.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseGet(() -> newRecord(employeeId, today));
        if (record.getCheckIn() != null) {
            throw new DomainException("You have already checked in today.");
        }
        record.setCheckIn(LocalDateTime.now(clock));
        attendance.save(record);
    }

    @Transactional
    public void checkOut(Long employeeId) {
        LocalDate today = LocalDate.now(clock);
        AttendanceRecord record = attendance.findByEmployeeIdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new DomainException("Check in before you check out."));
        if (record.getCheckIn() == null) {
            throw new DomainException("Check in before you check out.");
        }
        if (record.getCheckOut() != null) {
            throw new DomainException("You have already checked out today.");
        }
        record.setCheckOut(LocalDateTime.now(clock));
        attendance.save(record);
    }

    private AttendanceRecord newRecord(Long employeeId, LocalDate day) {
        Employee employee = employees.get(employeeId);
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setWorkDate(day);
        return record;
    }
}
