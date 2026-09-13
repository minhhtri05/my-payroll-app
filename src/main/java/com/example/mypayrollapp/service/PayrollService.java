package com.example.mypayrollapp.service;

import com.example.mypayrollapp.dto.AssignmentRequest;
import com.example.mypayrollapp.dto.ConflictResponse;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.entity.Plan;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.repository.EmployeeRepository;
import com.example.mypayrollapp.repository.PlanAssignmentRepository;
import com.example.mypayrollapp.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PlanAssignmentRepository assignmentRepo;
    private final EmployeeRepository employeeRepo;
    private final PlanRepository planRepo;

    // 1. Quét toàn bộ xung đột lịch của một Plan
    @Transactional(readOnly = true)
    public List<ConflictResponse> checkPlanConflicts(Long planId) {
        return assignmentRepo.findAllConflictsByPlanId(planId);
    }

    // 2. Thêm ca làm mới vào Plan (kèm kiểm tra trùng ngày)
    @Transactional
    public PlanAssignment assignEmployeeToPlan(Long planId, AssignmentRequest req, boolean force) {
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan không tồn tại với ID: " + planId));
        Employee employee = employeeRepo.findById(req.getEmployeeId())
                .orElseThrow(() -> new IllegalArgumentException("Nhân viên không tồn tại với ID: " + req.getEmployeeId()));

        // Kiểm tra xem nhân viên đã có ca ở Plan khác trong cùng ngày chưa
        List<PlanAssignment> conflicts = assignmentRepo.findConflictingShifts(
                employee.getId(), req.getWorkDate(), planId
        );

        if (!conflicts.isEmpty() && !force) {
            Plan conflictingPlan = conflicts.get(0).getPlan();
            throw new IllegalStateException(String.format(
                    "Trùng lịch! Nhân viên '%s' đã có lịch làm tại Plan '%s' vào ngày %s.",
                    employee.getFullName(), conflictingPlan.getName(), req.getWorkDate()
            ));
        }

        PlanAssignment assignment = PlanAssignment.builder()
                .plan(plan)
                .employee(employee)
                .workDate(req.getWorkDate())
                .location(req.getLocation())
                .role(req.getRole())
                .salaryAmount(req.getSalaryAmount())
                .allowance(req.getAllowance() != null ? req.getAllowance() : BigDecimal.ZERO)
                .note(req.getNote())
                .build();

        return assignmentRepo.save(assignment);
    }

    // 3. Lấy danh sách ca làm của Plan
    @Transactional(readOnly = true)
    public List<PlanAssignment> getAssignmentsByPlan(Long planId) {
        return assignmentRepo.findByPlanId(planId);
    }
}