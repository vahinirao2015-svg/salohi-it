package com.salohi.hrms.web;

import com.salohi.hrms.domain.AttendanceRecord;
import com.salohi.hrms.domain.LeaveStatus;
import com.salohi.hrms.domain.Payslip;
import com.salohi.hrms.repo.AttendanceRepository;
import com.salohi.hrms.repo.EmployeeRepository;
import com.salohi.hrms.repo.LeaveRequestRepository;
import com.salohi.hrms.repo.PayslipRepository;
import com.salohi.hrms.security.HrmsUser;
import com.salohi.hrms.service.EmployeeService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Controller
public class DashboardController {

    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final AttendanceRepository attendance;
    private final PayslipRepository payslips;
    private final EmployeeService employeeService;
    private final Clock clock;

    public DashboardController(
            EmployeeRepository employees,
            LeaveRequestRepository leaves,
            AttendanceRepository attendance,
            PayslipRepository payslips,
            EmployeeService employeeService,
            Clock clock) {
        this.employees = employees;
        this.leaves = leaves;
        this.attendance = attendance;
        this.payslips = payslips;
        this.employeeService = employeeService;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal HrmsUser user, Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("today", today);
        model.addAttribute("headcount", employees.countByActiveTrue());
        model.addAttribute("pendingLeaves", leaves.countByStatus(LeaveStatus.PENDING));
        model.addAttribute("presentToday", attendance.countByWorkDateAndCheckInIsNotNull(today));
        var self = employeeService.get(user.id());
        model.addAttribute("leaveBalance", self.getAnnualLeaveBalance());
        model.addAttribute("department", self.getDepartment());
        model.addAttribute("designation", self.getDesignation());
        Optional<AttendanceRecord> mine = attendance.findByEmployeeIdAndWorkDate(user.id(), today);
        model.addAttribute("checkedIn", mine.map(row -> row.getCheckIn() != null).orElse(false));
        model.addAttribute("checkedOut", mine.map(row -> row.getCheckOut() != null).orElse(false));
        Optional<Payslip> latest = payslips.findFirstByEmployeeIdOrderByPayYearDescPayMonthDesc(user.id());
        model.addAttribute("latestPayslip", latest.orElse(null));
        return "dashboard";
    }
}
