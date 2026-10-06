package com.salohi.hrms.repo;

import com.salohi.hrms.domain.LeaveRequest;
import com.salohi.hrms.domain.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    List<LeaveRequest> findAllByOrderByAppliedAtDesc();

    long countByStatus(LeaveStatus status);

    @Query("""
            select count(l) from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status in :statuses
              and l.startDate <= :endDate
              and l.endDate >= :startDate
            """)
    long countOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("statuses") Collection<LeaveStatus> statuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select l from LeaveRequest l
            where l.employee.id = :employeeId
              and l.status = com.salohi.hrms.domain.LeaveStatus.APPROVED
              and l.leaveType = com.salohi.hrms.domain.LeaveType.UNPAID
              and l.startDate <= :monthEnd
              and l.endDate >= :monthStart
            """)
    List<LeaveRequest> findApprovedUnpaidOverlapping(
            @Param("employeeId") Long employeeId,
            @Param("monthStart") LocalDate monthStart,
            @Param("monthEnd") LocalDate monthEnd);
}
