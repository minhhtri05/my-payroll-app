package com.example.mypayrollapp.repository;

import com.example.mypayrollapp.dto.ConflictResponse;
import com.example.mypayrollapp.entity.PlanAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PlanAssignmentRepository extends JpaRepository<PlanAssignment, Long> {

    List<PlanAssignment> findByPlanId(Long planId);

    // 1. Kiểm tra 1 nhân sự xem có bị trùng lịch ở Plan khác trong ngày không (Check khi thêm ca)
    @Query("SELECT pa FROM PlanAssignment pa " +
            "JOIN FETCH pa.plan p " +
            "WHERE pa.employee.id = :employeeId " +
            "AND pa.workDate = :workDate " +
            "AND pa.plan.id <> :planId")
    List<PlanAssignment> findConflictingShifts(
            @Param("employeeId") Long employeeId,
            @Param("workDate") LocalDate workDate,
            @Param("planId") Long planId
    );

    // 2. Quét toàn bộ xung đột của một Plan với tất cả các Plan khác (Check trước khi xuất Excel)
    @Query("SELECT new com.example.mypayrollapp.dto.ConflictResponse(" +
            "pa1.employee.id, pa1.employee.fullName, pa1.employee.idCardNumber, " +
            "pa1.workDate, p1.name, p2.name, pa1.location, pa2.location) " +
            "FROM PlanAssignment pa1 " +
            "JOIN pa1.plan p1 " +
            "JOIN PlanAssignment pa2 ON pa1.employee.id = pa2.employee.id " +
            "    AND pa1.workDate = pa2.workDate " +
            "    AND pa1.plan.id <> pa2.plan.id " +
            "JOIN pa2.plan p2 " +
            "WHERE pa1.plan.id = :planId")
    List<ConflictResponse> findAllConflictsByPlanId(@Param("planId") Long planId);
}