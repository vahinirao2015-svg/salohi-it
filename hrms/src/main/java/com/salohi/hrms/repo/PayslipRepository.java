package com.salohi.hrms.repo;

import com.salohi.hrms.domain.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    List<Payslip> findByEmployeeIdOrderByPayYearDescPayMonthDesc(Long employeeId);

    List<Payslip> findAllByOrderByPayYearDescPayMonthDesc();

    Optional<Payslip> findByEmployeeIdAndPayYearAndPayMonth(Long employeeId, int payYear, int payMonth);

    Optional<Payslip> findFirstByEmployeeIdOrderByPayYearDescPayMonthDesc(Long employeeId);
}
