package com.salohi.hrms.repo;

import com.salohi.hrms.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);

    List<Employee> findAllByOrderByEmployeeCodeAsc();

    long countByActiveTrue();
}
