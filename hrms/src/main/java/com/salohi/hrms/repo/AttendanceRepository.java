package com.salohi.hrms.repo;

import com.salohi.hrms.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);

    List<AttendanceRecord> findByWorkDateOrderByCheckInAsc(LocalDate workDate);

    List<AttendanceRecord> findByEmployeeIdOrderByWorkDateDesc(Long employeeId);

    long countByWorkDateAndCheckInIsNotNull(LocalDate workDate);
}
