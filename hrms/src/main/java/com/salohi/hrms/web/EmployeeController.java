package com.salohi.hrms.web;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.domain.Role;
import com.salohi.hrms.security.HrmsUser;
import com.salohi.hrms.service.DomainException;
import com.salohi.hrms.service.EmployeeService;
import com.salohi.hrms.web.dto.EmployeeForm;
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
public class EmployeeController {

    private final EmployeeService employees;

    public EmployeeController(EmployeeService employees) {
        this.employees = employees;
    }

    @GetMapping("/admin/employees")
    public String list(Model model) {
        model.addAttribute("pageTitle", "People");
        model.addAttribute("people", employees.list());
        return "employees/list";
    }

    @GetMapping("/admin/employees/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Add employee");
        model.addAttribute("employeeForm", new EmployeeForm());
        model.addAttribute("roles", Role.values());
        model.addAttribute("editing", false);
        model.addAttribute("postUrl", "/admin/employees");
        return "employees/form";
    }

    @PostMapping("/admin/employees")
    public String create(
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (form.getPassword() == null || form.getPassword().length() < 8) {
            binding.rejectValue("password", "password", "Use at least 8 characters.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Add employee");
            model.addAttribute("roles", Role.values());
            model.addAttribute("editing", false);
            model.addAttribute("postUrl", "/admin/employees");
            return "employees/form";
        }
        try {
            employees.create(form);
        } catch (DomainException ex) {
            model.addAttribute("pageTitle", "Add employee");
            model.addAttribute("roles", Role.values());
            model.addAttribute("editing", false);
            model.addAttribute("postUrl", "/admin/employees");
            model.addAttribute("error", ex.getMessage());
            return "employees/form";
        }
        redirect.addFlashAttribute("message", "Employee account created.");
        return "redirect:/admin/employees";
    }

    @GetMapping("/admin/employees/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employee employee = employees.get(id);
        EmployeeForm form = new EmployeeForm();
        form.setEmployeeCode(employee.getEmployeeCode());
        form.setFirstName(employee.getFirstName());
        form.setLastName(employee.getLastName());
        form.setEmail(employee.getEmail());
        form.setRole(employee.getRole());
        form.setDepartment(employee.getDepartment());
        form.setDesignation(employee.getDesignation());
        form.setPhone(employee.getPhone());
        form.setJoiningDate(employee.getJoiningDate());
        form.setBasicSalary(employee.getBasicSalary());
        form.setAnnualLeaveBalance(employee.getAnnualLeaveBalance());
        form.setActive(employee.isActive());
        model.addAttribute("pageTitle", "Edit employee");
        model.addAttribute("employeeForm", form);
        model.addAttribute("roles", Role.values());
        model.addAttribute("editing", true);
        model.addAttribute("employeeId", id);
        model.addAttribute("postUrl", "/admin/employees/" + id);
        return "employees/form";
    }

    @PostMapping("/admin/employees/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("employeeForm") EmployeeForm form,
            BindingResult binding,
            Model model,
            RedirectAttributes redirect) {
        if (form.getPassword() != null && !form.getPassword().isBlank() && form.getPassword().length() < 8) {
            binding.rejectValue("password", "password", "Use at least 8 characters, or leave the password blank.");
        }
        if (binding.hasErrors()) {
            model.addAttribute("pageTitle", "Edit employee");
            model.addAttribute("roles", Role.values());
            model.addAttribute("editing", true);
            model.addAttribute("employeeId", id);
            model.addAttribute("postUrl", "/admin/employees/" + id);
            return "employees/form";
        }
        try {
            employees.update(id, form);
        } catch (DomainException ex) {
            model.addAttribute("pageTitle", "Edit employee");
            model.addAttribute("roles", Role.values());
            model.addAttribute("editing", true);
            model.addAttribute("employeeId", id);
            model.addAttribute("postUrl", "/admin/employees/" + id);
            model.addAttribute("error", ex.getMessage());
            return "employees/form";
        }
        redirect.addFlashAttribute("message", "Employee updated.");
        return "redirect:/admin/employees";
    }

    @PostMapping("/admin/employees/{id}/deactivate")
    public String deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal HrmsUser actor,
            RedirectAttributes redirect) {
        try {
            employees.deactivate(id, actor.id());
            redirect.addFlashAttribute("message", "Employee deactivated.");
        } catch (DomainException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/employees";
    }
}
