package com.salohi.hrms.config;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.domain.Role;
import com.salohi.hrms.repo.EmployeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(EmployeeRepository employees, PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (employees.count() > 0) {
            return;
        }
        employees.save(person(
                "SAL-001", "Priya", "Menon", "admin@salohi.it", Role.ADMIN,
                "Human Resources", "HR Manager", "9000000001",
                LocalDate.of(2019, 4, 1), new BigDecimal("90000.00"), 18));
        employees.save(person(
                "SAL-002", "Arjun", "Rao", "arjun@salohi.it", Role.EMPLOYEE,
                "Engineering", "Software Engineer", "9000000002",
                LocalDate.of(2022, 7, 11), new BigDecimal("75000.00"), 16));
        employees.save(person(
                "SAL-003", "Meera", "Iyer", "meera@salohi.it", Role.EMPLOYEE,
                "Finance", "Accountant", "9000000003",
                LocalDate.of(2021, 1, 18), new BigDecimal("62000.00"), 14));
    }

    private Employee person(
            String code,
            String first,
            String last,
            String email,
            Role role,
            String department,
            String designation,
            String phone,
            LocalDate joined,
            BigDecimal salary,
            int balance) {
        Employee employee = new Employee();
        employee.setEmployeeCode(code);
        employee.setFirstName(first);
        employee.setLastName(last);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode("Salohi@123"));
        employee.setRole(role);
        employee.setDepartment(department);
        employee.setDesignation(designation);
        employee.setPhone(phone);
        employee.setJoiningDate(joined);
        employee.setBasicSalary(salary);
        employee.setAnnualLeaveBalance(balance);
        employee.setActive(true);
        return employee;
    }
}
