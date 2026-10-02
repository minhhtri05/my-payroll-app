package com.example.mypayrollapp.service;

import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.entity.Plan;
import com.example.mypayrollapp.entity.PlanAssignment;
import com.example.mypayrollapp.repository.PlanAssignmentRepository;
import com.example.mypayrollapp.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PayrollExportService {

    private final PlanRepository planRepository;
    private final PlanAssignmentRepository assignmentRepository;

    // 1. Hàm nhận 1 tham số id (Xử lý dứt điểm lỗi Expected 2 arguments but found 1)
    public byte[] exportWithCustomColumns(Long planId) throws Exception {
        return exportPlanPayroll(planId);
    }

    // 2. Xuất theo Plan ID
    public byte[] exportPlanPayroll(Long planId) throws Exception {
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Plan: " + planId));

        List<PlanAssignment> assignments = assignmentRepository.findByPlanId(planId);
        Map<Long, Employee> unique = new LinkedHashMap<>();

        for (PlanAssignment pa : assignments) {
            Employee emp = pa.getEmployee();
            if (emp != null) {
                if (pa.getRole() != null && !pa.getRole().isBlank()) emp.setRole(pa.getRole());
                unique.putIfAbsent(emp.getId(), emp);
            }
        }
        return exportCustomEmployeeList(new ArrayList<>(unique.values()), false);
    }

    // 3. Hàm nhận 2 tham số: Danh sách nhân sự & Danh sách cột tùy chỉnh
    public byte[] exportWithCustomColumns(List<Employee> employees, List<Map<String, String>> columns) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bảng Lương");

            // Header Style: Nền xanh nhạt, chữ đậm, căn giữa, viền mỏng theo ảnh mẫu
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontName("Arial");
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setWrapText(true);

            // Data Style: Viền ô
            CellStyle dataStyle = workbook.createCellStyle();
            Font dataFont = workbook.createFont();
            dataFont.setFontName("Arial");
            dataStyle.setFont(dataFont);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(32);
            for (int i = 0; i < columns.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns.get(i).getOrDefault("label", ""));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);
                for (int colIdx = 0; colIdx < columns.size(); colIdx++) {
                    String key = columns.get(colIdx).getOrDefault("key", "");
                    String val = getEmployeeFieldValue(emp, key);
                    Cell cell = row.createCell(colIdx);
                    cell.setCellValue(val);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < columns.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 4. Hàm tương thích xuất 12 cột hoặc 16 cột
    public byte[] exportCustomEmployeeList(List<Employee> employees, boolean isFullMode) throws Exception {
        List<Map<String, String>> cols = isFullMode ? getAllColumns() : getDefaultColumns();
        return exportWithCustomColumns(employees, cols);
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

    private List<Map<String, String>> getAllColumns() {
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
                Map.of("key", "phone", "label", "Số điện thoại"),
                Map.of("key", "totalSalary", "label", "Tổng Lương"),
                Map.of("key", "note", "label", "Ghi Chú"),
                Map.of("key", "frontIdUrl", "label", "Ảnh Mặt Trước CCCD"),
                Map.of("key", "backIdUrl", "label", "Ảnh Mặt Sau CCCD")
        );
    }

    private String getEmployeeFieldValue(Employee emp, String key) {
        if (emp == null || key == null) return "";
        return switch (key) {
            case "fullName" -> emp.getFullName() != null ? emp.getFullName() : "";
            case "role" -> emp.getRole() != null ? emp.getRole() : "";
            case "dob" -> emp.getDob() != null ? emp.getDob() : "";
            case "idCardNumber" -> emp.getIdCardNumber() != null ? emp.getIdCardNumber() : "";
            case "idCardIssuedDate" -> emp.getIdCardIssuedDate() != null ? emp.getIdCardIssuedDate() : "";
            case "idCardIssuedPlace" -> emp.getIdCardIssuedPlace() != null ? emp.getIdCardIssuedPlace() : "";
            case "address" -> emp.getAddress() != null ? emp.getAddress() : "";
            case "taxCode" -> emp.getTaxCode() != null ? emp.getTaxCode() : "";
            case "bankAccountNumber" -> emp.getBankAccountNumber() != null ? emp.getBankAccountNumber() : "";
            case "bankInfo" -> emp.getBankInfo() != null ? emp.getBankInfo() : "";
            case "email" -> emp.getEmail() != null ? emp.getEmail() : "";
            case "phone" -> emp.getPhone() != null ? emp.getPhone() : "";
            case "totalSalary" -> emp.getTotalSalary() != null ? emp.getTotalSalary() : "";
            case "note" -> emp.getNote() != null ? emp.getNote() : "";
            case "frontIdUrl" -> emp.getFrontIdUrl() != null ? emp.getFrontIdUrl() : "";
            case "backIdUrl" -> emp.getBackIdUrl() != null ? emp.getBackIdUrl() : "";
            default -> "";
        };
    }
}