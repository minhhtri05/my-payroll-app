package com.example.mypayrollapp.controller;

import com.example.mypayrollapp.dto.ImportResult;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.repository.EmployeeRepository;
import com.example.mypayrollapp.repository.PlanAssignmentRepository;
import com.example.mypayrollapp.service.EmployeeImportService;
import com.example.mypayrollapp.service.GoogleDriveUploadService;
import com.example.mypayrollapp.service.PayrollExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmployeeController {

    private final EmployeeImportService importService;
    private final EmployeeRepository employeeRepository;
    private final PlanAssignmentRepository assignmentRepository;
    private final PayrollExportService exportService;
    private final GoogleDriveUploadService driveUploadService; // Inject service upload Drive

    // API 1: Public Form cho nhân viên vãng lai tự điền + Tự tải 2 ảnh CCCD lên Google Drive
    @PostMapping(value = "/public-submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> publicSubmit(
            @ModelAttribute Employee employee,
            @RequestParam(value = "frontImage", required = false) MultipartFile frontImage,
            @RequestParam(value = "backImage", required = false) MultipartFile backImage) {
        try {
            String idCard = employee.getIdCardNumber() != null
                    ? employee.getIdCardNumber().trim().replaceAll("[^0-9]", "")
                    : "";

            if (idCard.isBlank()) {
                return ResponseEntity.badRequest().body("Số CCCD không được để trống!");
            }

            // Kiểm tra trùng CCCD
            Optional<Employee> existing = employeeRepository.findByIdCardNumber(idCard);
            if (existing.isPresent()) {
                Employee oldEmp = existing.get();
                return ResponseEntity.badRequest().body(String.format(
                        "Thông tin đã tồn tại trong hệ thống! Số CCCD '%s' đã được đăng ký trước đó cho nhân sự '%s'. Vui lòng kiểm tra lại!",
                        idCard, oldEmp.getFullName()
                ));
            }

            // 1. Tải ảnh mặt trước lên Google Drive của bạn (nếu có chọn)
            if (frontImage != null && !frontImage.isEmpty()) {
                String frontUrl = driveUploadService.uploadToDrive(frontImage, "MAT_TRUOC", idCard);
                employee.setFrontIdUrl(frontUrl);
            }

            // 2. Tải ảnh mặt sau lên Google Drive của bạn (nếu có chọn)
            if (backImage != null && !backImage.isEmpty()) {
                String backUrl = driveUploadService.uploadToDrive(backImage, "MAT_SAU", idCard);
                employee.setBackIdUrl(backUrl);
            }

            employee.setIdCardNumber(idCard);
            employee.setEmployeeType("TEMPORARY");
            employee.setIsActive(true);
            return ResponseEntity.ok(employeeRepository.save(employee));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi khi gửi thông tin: " + e.getMessage());
        }
    }

    // API 2: Lấy danh sách nhân viên đang hoạt động (Mục 2 & Mục 3)
    @GetMapping
    public ResponseEntity<List<Employee>> getEmployees(@RequestParam(required = false, defaultValue = "PERMANENT") String type) {
        return ResponseEntity.ok(employeeRepository.findActiveByType(type));
    }

    // API 3: Thêm tay nhân viên (Mục 3)
    @PostMapping("/manual")
    public ResponseEntity<Employee> addManualEmployee(@RequestBody Employee employee) {
        employee.setEmployeeType("PERMANENT");
        employee.setIsActive(true);
        return ResponseEntity.ok(employeeRepository.save(employee));
    }

    // API 4: Import Excel vào Mục 3
    @PostMapping("/import-excel")
    public ResponseEntity<ImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(importService.importFromExcel(file));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    ImportResult.builder().message("Lỗi: " + e.getMessage()).build()
            );
        }
    }

    // API 5: Xuất Excel tùy chỉnh (12 cột chuẩn hoặc 16 cột đầy đủ)
    @PostMapping("/export-custom")
    public ResponseEntity<byte[]> exportCustomList(
            @RequestBody List<Employee> employees,
            @RequestParam(defaultValue = "false") boolean isFull) {
        try {
            byte[] excelBytes = exportService.exportCustomEmployeeList(employees, isFull);
            String filename = isFull ? "Bang_Luong_Chi_Tiet_16_Cot.xlsx" : "Bang_Luong_Chuan_12_Cot.xlsx";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // API 6: Xóa mềm (Đưa vào Thùng Rác)
    @PostMapping("/soft-delete")
    @Transactional
    public ResponseEntity<?> softDelete(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) {
            e.setIsActive(false);
        }
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã chuyển " + ids.size() + " nhân sự vào Thùng rác!");
    }

    // API 7: Lấy danh sách trong Thùng Rác
    @GetMapping("/trash")
    public ResponseEntity<List<Employee>> getTrashList() {
        return ResponseEntity.ok(employeeRepository.findByIsActiveFalseOrderByIdDesc());
    }

    // API 8: Khôi phục từ Thùng Rác
    @PostMapping("/restore")
    @Transactional
    public ResponseEntity<?> restore(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) {
            e.setIsActive(true);
        }
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã khôi phục thành công " + ids.size() + " nhân sự!");
    }

    // API 9: Xóa vĩnh viễn khỏi Database
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
        return ResponseEntity.ok("Đã xóa vĩnh viễn " + ids.size() + " nhân sự khỏi hệ thống!");
    }

    // API 10: Tải file sao lưu Backup (Excel 16 cột)
    @GetMapping("/backup")
    public ResponseEntity<byte[]> backupData() {
        try {
            List<Employee> all = employeeRepository.findAll();
            byte[] excelBytes = exportService.exportCustomEmployeeList(all, true);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Payroll_Backup_All_16_Cot.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // API 11: Khôi phục từ file Sao lưu
    @PostMapping("/restore-backup")
    public ResponseEntity<ImportResult> restoreBackup(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(importService.importFromExcel(file));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    ImportResult.builder().message("Lỗi phục hồi: " + e.getMessage()).build()
            );
        }
    }
}