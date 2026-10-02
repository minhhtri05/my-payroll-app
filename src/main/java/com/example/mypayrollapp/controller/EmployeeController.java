package com.example.mypayrollapp.controller;

import com.example.mypayrollapp.dto.ExportRequest;
import com.example.mypayrollapp.dto.ImportResult;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.repository.EmployeeRepository;
import com.example.mypayrollapp.repository.PlanAssignmentRepository;
import com.example.mypayrollapp.service.EmployeeImportService;
import com.example.mypayrollapp.service.PayrollExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeImportService importService;
    private final EmployeeRepository employeeRepository;
    private final PlanAssignmentRepository assignmentRepository;
    private final PayrollExportService exportService;

    // 1. Lấy danh sách nhân viên đang hoạt động
    @GetMapping
    public ResponseEntity<List<Employee>> getEmployees() {
        List<Employee> activeList = employeeRepository.findAllByOrderByIdDesc().stream()
                .filter(e -> e.getIsActive() == null || e.getIsActive())
                .toList();
        return ResponseEntity.ok(activeList);
    }

    // 2. Chỉnh sửa trực tiếp thông tin nhân sự theo ID
    @PostMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateEmployee(@PathVariable Long id, @RequestBody Employee updated) {
        try {
            Optional<Employee> opt = employeeRepository.findById(id);
            if (opt.isEmpty()) {
                return ResponseEntity.badRequest().body("Không tìm thấy nhân sự có ID: " + id);
            }
            Employee emp = opt.get();
            emp.setFullName(updated.getFullName());
            emp.setRole(updated.getRole() != null ? updated.getRole() : "SUP");
            emp.setDob(updated.getDob());
            if (updated.getIdCardNumber() != null && !updated.getIdCardNumber().isBlank()) {
                emp.setIdCardNumber(updated.getIdCardNumber().trim().replaceAll("[^0-9]", ""));
            }
            emp.setIdCardIssuedDate(updated.getIdCardIssuedDate());
            emp.setIdCardIssuedPlace(updated.getIdCardIssuedPlace());
            emp.setAddress(updated.getAddress());
            emp.setTaxCode(updated.getTaxCode());
            emp.setBankAccountNumber(updated.getBankAccountNumber());
            emp.setBankInfo(standardizeBankInfo(updated.getBankInfo()));
            emp.setEmail(updated.getEmail());
            emp.setPhone(updated.getPhone());
            emp.setTotalSalary(updated.getTotalSalary());
            emp.setNote(updated.getNote());

            return ResponseEntity.ok(employeeRepository.save(emp));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi cập nhật: " + e.getMessage());
        }
    }

    // 3. Thêm tay nhân viên mới
    @PostMapping("/manual")
    public ResponseEntity<?> addManualEmployee(@RequestBody Employee employee) {
        try {
            if (employee.getFullName() == null || employee.getFullName().isBlank()) {
                return ResponseEntity.badRequest().body("Họ và tên không được để trống!");
            }
            String idCard = employee.getIdCardNumber() != null
                    ? employee.getIdCardNumber().trim().replaceAll("[^0-9]", "")
                    : "";

            if (idCard.isBlank()) {
                return ResponseEntity.badRequest().body("Số CCCD không được để trống!");
            }

            Optional<Employee> existing = employeeRepository.findByIdCardNumber(idCard);
            Employee emp = existing.orElseGet(Employee::new);

            emp.setFullName(employee.getFullName().trim());
            emp.setRole(employee.getRole() != null ? employee.getRole() : "SUP");
            emp.setDob(employee.getDob());
            emp.setIdCardNumber(idCard);
            emp.setIdCardIssuedDate(employee.getIdCardIssuedDate());
            emp.setIdCardIssuedPlace(employee.getIdCardIssuedPlace());
            emp.setAddress(employee.getAddress());
            emp.setTaxCode(employee.getTaxCode());
            emp.setBankAccountNumber(employee.getBankAccountNumber());
            emp.setBankInfo(standardizeBankInfo(employee.getBankInfo()));
            emp.setEmail(employee.getEmail());
            emp.setPhone(employee.getPhone());
            emp.setTotalSalary(employee.getTotalSalary());
            emp.setNote(employee.getNote());
            emp.setEmployeeType("PERMANENT");
            emp.setIsActive(true);

            return ResponseEntity.ok(employeeRepository.save(emp));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi thêm nhân sự: " + e.getMessage());
        }
    }

    // 4. Import Excel
    @PostMapping("/import-excel")
    public ResponseEntity<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(importService.importFromExcel(file));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    ImportResult.builder().message("Lỗi import: " + e.getMessage()).build()
            );
        }
    }

    // 5. Xuất Excel tùy biến thứ tự cột
    @PostMapping("/export-custom-columns")
    public ResponseEntity<byte[]> exportCustomColumns(@RequestBody ExportRequest request) {
        try {
            byte[] excelBytes = exportService.exportWithCustomColumns(request.getEmployees(), request.getColumns());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Bang_Luong.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6. Xóa mềm (Thùng Rác)
    @PostMapping("/soft-delete")
    @Transactional
    public ResponseEntity<?> softDelete(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) e.setIsActive(false);
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã chuyển " + ids.size() + " nhân sự vào Thùng rác!");
    }

    // 7. Danh sách Thùng Rác
    @GetMapping("/trash")
    public ResponseEntity<List<Employee>> getTrashList() {
        return ResponseEntity.ok(employeeRepository.findByIsActiveFalseOrderByIdDesc());
    }

    // 8. Khôi phục từ Thùng Rác
    @PostMapping("/restore")
    @Transactional
    public ResponseEntity<?> restore(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) e.setIsActive(true);
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã khôi phục thành công " + ids.size() + " nhân sự!");
    }

    // 9. Xóa vĩnh viễn
    @PostMapping("/hard-delete")
    @Transactional
    public ResponseEntity<?> hardDelete(@RequestBody List<Long> ids) {
        for (Long id : ids) {
            List<PlanAssignment> assignments = assignmentRepository.findAll().stream()
                    .filter(a -> a.getEmployee() != null && a.getEmployee().getId().equals(id))
                    .toList();
            assignmentRepository.deleteAll(assignments);
            employeeRepository.deleteById(id);
        }
        return ResponseEntity.ok("Đã xóa vĩnh viễn " + ids.size() + " nhân sự!");
    }

    // 10. Tải file Backup
    @GetMapping("/backup")
    public ResponseEntity<byte[]> backupData() {
        try {
            List<Employee> all = employeeRepository.findAll();
            List<Map<String, String>> defaultCols = getDefaultColumns();
            byte[] excelBytes = exportService.exportWithCustomColumns(all, defaultCols);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Payroll_Backup_All.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // 11. Phục hồi Backup
    @PostMapping("/restore-backup")
    public ResponseEntity<ImportResult> restoreBackup(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(importService.importFromExcel(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    ImportResult.builder().message("Lỗi phục hồi: " + e.getMessage()).build()
            );
        }
    }

    private List<Map<String, String>> getDefaultColumns() {
        return List.of(
                Map.of("key", "fullName", "label", "Họ Và Tên Nhân Viên"),
                Map.of("key", "role", "label", "Chức Vụ"),
                Map.of("key", "dob", "label", "Ngày Tháng Năm Sinh"),
                Map.of("key", "idCardNumber", "label", "Số CCCD"),
                Map.of("key", "idCardIssuedDate", "label", "Ngày Cấp"),
                Map.of("key", "idCardIssuedPlace", "label", "Nơi Cấp"),
                Map.of("key", "address", "label", "Địa Chỉ (Trên CCCD)"),
                Map.of("key", "taxCode", "label", "Mã Số Thuế"),
                Map.of("key", "bankAccountNumber", "label", "Số TK"),
                Map.of("key", "bankInfo", "label", "Ngân Hàng, Chi Nhánh"),
                Map.of("key", "email", "label", "Mail"),
                Map.of("key", "phone", "label", "Số điện thoại")
        );
    }

    private String standardizeBankInfo(String input) {
        if (input == null || input.isBlank()) return "";
        String s = input.trim();
        int dashIdx = s.indexOf("-");
        if (dashIdx != -1) {
            String bank = cleanBankName(s.substring(0, dashIdx));
            String branch = s.substring(dashIdx + 1).trim();
            return branch.isBlank() ? bank : bank + " - " + branch;
        }
        int commaIdx = s.indexOf(",");
        if (commaIdx != -1) {
            String bank = cleanBankName(s.substring(0, commaIdx));
            String branch = s.substring(commaIdx + 1).trim();
            return branch.isBlank() ? bank : bank + " - " + branch;
        }
        return cleanBankName(s);
    }

    private String cleanBankName(String b) {
        if (b == null) return "";
        String name = b.trim();
        String lower = name.toLowerCase();
        if (lower.equals("vcb") || lower.equals("vietcom")) return "Vietcombank";
        if (lower.equals("ctg") || lower.equals("vietin") || lower.equals("vietinbank")) return "Vietinbank";
        if (lower.equals("tcb") || lower.equals("techcom") || lower.equals("techcombank")) return "Techcombank";
        if (lower.equals("mb") || lower.equals("mbbank") || lower.equals("mb bank")) return "MB Bank";
        if (lower.equals("bidv")) return "BIDV";
        if (lower.equals("acb")) return "ACB";
        if (lower.equals("tpb") || lower.equals("tpbank")) return "TPBank";
        if (lower.equals("vpb") || lower.equals("vpbank")) return "VPBank";
        if (lower.equals("sacom") || lower.equals("sacombank")) return "Sacombank";
        if (lower.equals("agri") || lower.equals("agribank")) return "Agribank";
        return name;
    }
}