package com.salohi.hrms.service;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.domain.Role;
import com.salohi.hrms.repo.EmployeeRepository;
import com.salohi.hrms.web.dto.EmployeeForm;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employees, PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<Employee> list() {
        return employees.findAllByOrderByEmployeeCodeAsc();
    }

    @Transactional(readOnly = true)
    public Employee get(Long id) {
        return employees.findById(id).orElseThrow(() -> new DomainException("Employee not found."));
    }

    @Transactional
    public Employee create(EmployeeForm form) {
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new DomainException("A password is required for a new account.");
        }
        requireUnique(form, null);
        Employee employee = new Employee();
        apply(employee, form);
        employee.setPassword(passwordEncoder.encode(form.getPassword()));
        return employees.save(employee);
    }

    @Transactional
    public Employee update(Long id, EmployeeForm form) {
        Employee employee = get(id);
        requireUnique(form, id);
        apply(employee, form);
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return employees.save(employee);
    }

    @Transactional
    public void deactivate(Long id, Long actorId) {
        if (id.equals(actorId)) {
            throw new DomainException("You cannot deactivate your own account.");
        }
        Employee employee = get(id);
        employee.setActive(false);
        employees.save(employee);
    }

    private void requireUnique(EmployeeForm form, Long currentId) {
        employees.findByEmailIgnoreCase(form.getEmail()).ifPresent(existing -> {
            if (currentId == null || !existing.getId().equals(currentId)) {
                throw new DomainException("An account with this email already exists.");
            }
        });
        employees.findAllByOrderByEmployeeCodeAsc().stream()
                .filter(existing -> existing.getEmployeeCode().equalsIgnoreCase(form.getEmployeeCode()))
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new DomainException("Employee code " + form.getEmployeeCode() + " is already in use.");
                });
    }

    private void apply(Employee employee, EmployeeForm form) {
        employee.setEmployeeCode(form.getEmployeeCode().trim());
        employee.setFirstName(form.getFirstName().trim());
        employee.setLastName(form.getLastName().trim());
        employee.setEmail(form.getEmail().trim().toLowerCase());
        employee.setRole(form.getRole() == null ? Role.EMPLOYEE : form.getRole());
        employee.setDepartment(form.getDepartment().trim());
        employee.setDesignation(form.getDesignation().trim());
        employee.setPhone(blankToNull(form.getPhone()));
        employee.setJoiningDate(form.getJoiningDate());
        employee.setBasicSalary(form.getBasicSalary());
        employee.setAnnualLeaveBalance(form.getAnnualLeaveBalance());
        employee.setActive(form.isActive());
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
