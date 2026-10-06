package com.salohi.hrms.security;

import com.salohi.hrms.domain.Employee;
import com.salohi.hrms.repo.EmployeeRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HrmsUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employees;

    public HrmsUserDetailsService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        Employee employee = employees.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown account"));
        return new HrmsUser(
                employee.getId(),
                employee.getEmail(),
                employee.getPassword(),
                employee.getFullName(),
                employee.getRole(),
                employee.isActive());
    }
}
