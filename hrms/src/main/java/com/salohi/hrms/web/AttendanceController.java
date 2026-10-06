package com.salohi.hrms.web;

import com.salohi.hrms.security.HrmsUser;
import com.salohi.hrms.service.AttendanceService;
import com.salohi.hrms.service.DomainException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;

@Controller
public class AttendanceController {

    private final AttendanceService attendance;
    private final Clock clock;

    public AttendanceController(AttendanceService attendance, Clock clock) {
        this.attendance = attendance;
        this.clock = clock;
    }

    @GetMapping("/attendance")
    public String list(
            @AuthenticationPrincipal HrmsUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
            Model model) {
        LocalDate selected = day == null ? LocalDate.now(clock) : day;
        model.addAttribute("pageTitle", "Attendance");
        model.addAttribute("day", selected);
        model.addAttribute("rows", attendance.list(user.id(), user.admin(), selected));
        var todayRecord = attendance.today(user.id());
        model.addAttribute("myCheckIn", todayRecord.map(record -> record.getCheckIn()).orElse(null));
        model.addAttribute("myCheckOut", todayRecord.map(record -> record.getCheckOut()).orElse(null));
        return "attendance/list";
    }

    @PostMapping("/attendance/check-in")
    public String checkIn(@AuthenticationPrincipal HrmsUser user, RedirectAttributes redirect) {
        try {
            attendance.checkIn(user.id());
            redirect.addFlashAttribute("message", "Checked in.");
        } catch (DomainException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/attendance";
    }

    @PostMapping("/attendance/check-out")
    public String checkOut(@AuthenticationPrincipal HrmsUser user, RedirectAttributes redirect) {
        try {
            attendance.checkOut(user.id());
            redirect.addFlashAttribute("message", "Checked out.");
        } catch (DomainException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/attendance";
    }
}
