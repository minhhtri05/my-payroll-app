package com.example.mypayrollapp.controller;

import com.example.mypayrollapp.dto.AssignmentRequest;
import com.example.mypayrollapp.dto.ConflictResponse;
import com.example.mypayrollapp.entity.Plan;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.repository.PlanRepository;
import com.example.mypayrollapp.service.PayrollExportService;
import com.example.mypayrollapp.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PlanController {

    private final PayrollService payrollService;
    private final PayrollExportService exportService;
    private final PlanRepository planRepository;

    // 1. Lấy danh sách tất cả các Plan
    @GetMapping
    public ResponseEntity<List<Plan>> getAllPlans() {
        return ResponseEntity.ok(planRepository.findAll());
    }

    // 2. Tạo Plan mới
    @PostMapping
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
        return ResponseEntity.ok(planRepository.save(plan));
    }

    // 3. Quét xung đột trùng ngày của một Plan
    @GetMapping("/{id}/conflicts")
    public ResponseEntity<List<ConflictResponse>> getConflicts(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.checkPlanConflicts(id));
    }

    // 4. Gán nhân sự vào Plan (tự động chặn nếu bị trùng ngày)
    @PostMapping("/{id}/assignments")
    public ResponseEntity<?> addAssignment(
            @PathVariable Long id,
            @RequestBody AssignmentRequest req,
            @RequestParam(defaultValue = "false") boolean force) {
        try {
            PlanAssignment assignment = payrollService.assignEmployeeToPlan(id, req, force);
            return ResponseEntity.ok(assignment);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 5. Lấy danh sách ca làm của Plan
    @GetMapping("/{id}/assignments")
    public ResponseEntity<List<PlanAssignment>> getAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getAssignmentsByPlan(id));
    }

    // 6. Tải file Excel Bảng Lương thành phẩm 12 cột
    @GetMapping("/{id}/export-payroll")
    public ResponseEntity<byte[]> exportPayroll(@PathVariable Long id) {
        try {
            byte[] excelBytes = exportService.exportPlanPayroll(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bang_Luong_Plan_" + id + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}