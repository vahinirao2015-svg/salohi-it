package com.salohi.hrms.web;

import com.salohi.hrms.security.HrmsUser;
import com.salohi.hrms.service.DomainException;
import com.salohi.hrms.service.EmployeeService;
import com.salohi.hrms.service.PayslipService;
import com.salohi.hrms.web.dto.PayslipForm;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.Clock;
import java.time.YearMonth;

@Controller
public class PayslipController {

    private final PayslipService payslips;
    private final EmployeeService employees;
    private final Clock clock;

    public PayslipController(PayslipService payslips, EmployeeService employees, Clock clock) {
        this.payslips = payslips;
        this.employees = employees;
        this.clock = clock;
    }

    @GetMapping("/payslips")
    public String list(@AuthenticationPrincipal HrmsUser user, Model model) {
        model.addAttribute("pageTitle", "Payslips");
        model.addAttribute("slips", payslips.list(user.id(), user.admin()));
        return "payslips/list";
    }

    @GetMapping("/admin/payslips/new")
    public String form(Model model) {
        PayslipForm form = new PayslipForm();
        YearMonth current = YearMonth.now(clock);
        form.setYear(current.getYear());
        form.setMonth(current.getMonthValue());
        model.addAttribute("pageTitle", "Generate payslip");
        model.addAttribute("payslipForm", form);
        model.addAttribute("people", employees.list());
        return "payslips/generate";
    }

    @PostMapping("/admin/payslips")
    public String generate(
            @Valid @ModelAttribute("payslipForm") PayslipForm form,
            BindingResult binding,
            Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Generate payslip");
            model.addAttribute("people", employees.list());
            return "payslips/generate";
        }
        try {
            var slip = payslips.generate(form.getEmployeeId(), form.getYear(), form.getMonth());
            return "redirect:/payslips/" + slip.getId();
        } catch (DomainException ex) {
            model.addAttribute("pageTitle", "Generate payslip");
            model.addAttribute("people", employees.list());
            model.addAttribute("error", ex.getMessage());
            return "payslips/generate";
        }
    }

    @GetMapping("/payslips/{id}")
    public String view(@PathVariable Long id, @AuthenticationPrincipal HrmsUser user, Model model) {
        var slip = payslips.get(id, user.id(), user.admin());
        model.addAttribute("pageTitle", "Payslip");
        model.addAttribute("slip", slip);
        return "payslips/view";
    }
}
