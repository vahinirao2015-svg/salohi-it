package com.salohi.hrms.web;

import com.salohi.hrms.domain.LeaveType;
import com.salohi.hrms.security.HrmsUser;
import com.salohi.hrms.service.DomainException;
import com.salohi.hrms.service.EmployeeService;
import com.salohi.hrms.service.LeaveService;
import com.salohi.hrms.web.dto.LeaveForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LeaveController {

    private final LeaveService leaves;
    private final EmployeeService employees;

    public LeaveController(LeaveService leaves, EmployeeService employees) {
        this.leaves = leaves;
        this.employees = employees;
    }

    @GetMapping("/leaves")
    public String list(@AuthenticationPrincipal HrmsUser user, Model model) {
        model.addAttribute("pageTitle", "Leave");
        model.addAttribute("requests", leaves.listFor(user.id(), user.admin()));
        model.addAttribute("balance", employees.get(user.id()).getAnnualLeaveBalance());
        return "leaves/list";
    }

    @GetMapping("/leaves/new")
    public String form(Model model) {
        model.addAttribute("pageTitle", "Request leave");
        model.addAttribute("leaveForm", new LeaveForm());
        model.addAttribute("leaveTypes", LeaveType.values());
        return "leaves/form";
    }

    @PostMapping("/leaves")
    public String apply(
            @AuthenticationPrincipal HrmsUser user,
            @Valid @ModelAttribute("leaveForm") LeaveForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Request leave");
            model.addAttribute("leaveTypes", LeaveType.values());
            return "leaves/form";
        }
        try {
            leaves.apply(user.id(), form);
        } catch (DomainException ex) {
            model.addAttribute("pageTitle", "Request leave");
            model.addAttribute("leaveTypes", LeaveType.values());
            model.addAttribute("error", ex.getMessage());
            return "leaves/form";
        }
        redirect.addFlashAttribute("message", "Leave request submitted.");
        return "redirect:/leaves";
    }

    @PostMapping("/admin/leaves/{id}/approve")
    public String approve(
            @PathVariable Long id,
            @AuthenticationPrincipal HrmsUser user,
            RedirectAttributes redirect) {
        try {
            leaves.approve(id, user.id());
            redirect.addFlashAttribute("message", "Leave approved.");
        } catch (DomainException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leaves";
    }

    @PostMapping("/admin/leaves/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @AuthenticationPrincipal HrmsUser user,
            RedirectAttributes redirect) {
        try {
            leaves.reject(id, user.id());
            redirect.addFlashAttribute("message", "Leave rejected.");
        } catch (DomainException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/leaves";
    }
}
