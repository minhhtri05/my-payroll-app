package com.example.mypayrollapp.controller;

import com.example.mypayrollapp.dto.ImportResult;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.entity.StoreLocation;
import com.example.mypayrollapp.repository.EmployeeRepository;
import com.example.mypayrollapp.repository.PlanAssignmentRepository;
import com.example.mypayrollapp.repository.StoreLocationRepository;
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
    private final GoogleDriveUploadService driveUploadService;
    private final StoreLocationRepository storeRepository;

    // API 1: Public Form - Đăng ký nhận lương vãng lai
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
                        "Thông tin đã tồn tại trong hệ thống! Số CCCD '%s' đã được đăng ký trước đó cho nhân sự '%s'.",
                        idCard, oldEmp.getFullName()
                ));
            }

            // Kiểm tra xem CỬA HÀNG đã có ai chọn chưa (chỉ tính những ai đang active, chưa bị xóa)
            if (employee.getWorkplace() != null && !employee.getWorkplace().isBlank()) {
                String wp = employee.getWorkplace().trim();
                boolean isTaken = employeeRepository.findActiveByType("TEMPORARY").stream()
                        .anyMatch(e -> wp.equalsIgnoreCase(e.getWorkplace()));
                if (isTaken) {
                    return ResponseEntity.badRequest().body(String.format(
                            "Cửa hàng '%s' vừa có người đăng ký xong! Vui lòng chọn cửa hàng khác còn trống.",
                            wp
                    ));
                }
            }

            // Tự động chuẩn hóa format Ngân Hàng, Chi Nhánh: "Tên Ngân Hàng - Chi Nhánh"
            employee.setBankInfo(standardizeBankInfo(employee.getBankInfo()));

            // Tải ảnh lên Google Drive
            if (frontImage != null && !frontImage.isEmpty()) {
                String frontUrl = driveUploadService.uploadToDrive(frontImage, "MAT_TRUOC", idCard);
                employee.setFrontIdUrl(frontUrl);
            }
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

    // API 2: Public - Lấy danh sách đăng ký hiển thị trực tiếp cho khách xem
    @GetMapping("/public/live-registrations")
    public ResponseEntity<List<Map<String, Object>>> getLiveRegistrations() {
        List<Employee> list = employeeRepository.findActiveByType("TEMPORARY");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee e : list) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("fullName", e.getFullName());
            map.put("phone", e.getPhone());
            map.put("idCardNumber", e.getIdCardNumber());
            map.put("workplace", e.getWorkplace());
            map.put("role", e.getRole());
            map.put("note", e.getNote() != null ? e.getNote() : "");
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // API 3: Public - Tải file Excel Bảng Đăng Ký Trực Tiếp (Khách tải trực tiếp)
    @GetMapping("/public/export-live-registrations")
    public ResponseEntity<byte[]> exportLiveRegistrations() {
        try {
            List<Employee> list = employeeRepository.findActiveByType("TEMPORARY");
            byte[] excelBytes = exportService.exportLiveRegistrations(list);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_Sach_Dang_Ky_Truc_Tiep.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // API 4: Public - Lấy danh sách cửa hàng kèm trạng thái còn trống (tự giải phóng khi xóa người)
    @GetMapping("/public/stores")
    public ResponseEntity<List<Map<String, Object>>> getStoresWithStatus() {
        List<StoreLocation> stores = storeRepository.findByIsActiveTrueOrderByNameAsc();
        // Chỉ tính những nhân viên VÃNG LAI ĐANG ACTIVE (chưa bị xóa)
        List<Employee> activeTemps = employeeRepository.findActiveByType("TEMPORARY");
        Set<String> takenStores = new HashSet<>();
        for (Employee e : activeTemps) {
            if (e.getWorkplace() != null && !e.getWorkplace().isBlank()) {
                takenStores.add(e.getWorkplace().trim().toLowerCase());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StoreLocation s : stores) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("isTaken", takenStores.contains(s.getName().trim().toLowerCase()));
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }

    // API 5: Admin - Lưu danh sách cửa hàng cho Job
    @PostMapping("/admin/stores")
    @Transactional
    public ResponseEntity<?> saveStores(@RequestBody List<String> storeNames) {
        storeRepository.deleteAll();
        List<StoreLocation> list = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>(storeNames);
        for (String name : unique) {
            if (name != null && !name.isBlank()) {
                list.add(StoreLocation.builder().name(name.trim()).isActive(true).build());
            }
        }
        storeRepository.saveAll(list);
        return ResponseEntity.ok("Đã cập nhật danh sách " + list.size() + " cửa hàng cho Job thành công!");
    }

    // API 6: Lấy danh sách nhân viên đang hoạt động
    @GetMapping
    public ResponseEntity<List<Employee>> getEmployees(@RequestParam(required = false, defaultValue = "PERMANENT") String type) {
        return ResponseEntity.ok(employeeRepository.findActiveByType(type));
    }

    // API 7: Thêm tay nhân viên (Mục 3)
    @PostMapping("/manual")
    public ResponseEntity<Employee> addManualEmployee(@RequestBody Employee employee) {
        employee.setEmployeeType("PERMANENT");
        employee.setIsActive(true);
        employee.setBankInfo(standardizeBankInfo(employee.getBankInfo()));
        return ResponseEntity.ok(employeeRepository.save(employee));
    }

    // API 8: Import Excel vào Mục 3
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

    // API 9: Xuất Excel (12 cột hoặc 16 cột)
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

    // API 10: Xóa mềm (Đưa vào Thùng Rác)
    @PostMapping("/soft-delete")
    @Transactional
    public ResponseEntity<?> softDelete(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) e.setIsActive(false);
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã chuyển " + ids.size() + " nhân sự vào Thùng rác!");
    }

    // API 11: Thùng rác
    @GetMapping("/trash")
    public ResponseEntity<List<Employee>> getTrashList() {
        return ResponseEntity.ok(employeeRepository.findByIsActiveFalseOrderByIdDesc());
    }

    // API 12: Khôi phục
    @PostMapping("/restore")
    @Transactional
    public ResponseEntity<?> restore(@RequestBody List<Long> ids) {
        List<Employee> list = employeeRepository.findAllById(ids);
        for (Employee e : list) e.setIsActive(true);
        employeeRepository.saveAll(list);
        return ResponseEntity.ok("Đã khôi phục thành công " + ids.size() + " nhân sự!");
    }

    // API 13: Xóa vĩnh viễn
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

    // API 14: Backup
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

    // API 15: Restore
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

    // Hàm tự động chuẩn hóa định dạng ngân hàng: "Tên Ngân Hàng - Chi Nhánh"
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