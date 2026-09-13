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

    // 1. Xuất Excel bảng lương (12 cột chuẩn hoặc 16 cột đầy đủ)
    public byte[] exportCustomEmployeeList(List<Employee> employees, boolean isFullMode) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bảng Lương");

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers;
            if (!isFullMode) {
                headers = new String[]{
                        "Họ Và Tên Nhân Viên", "Chức Vụ", "Ngày Tháng Năm Sinh ", "Số CCCD",
                        "Ngày Cấp", "Nơi Cấp ", "Địa Chỉ \n(Trên CCCD)", "Mã Số Thuế",
                        "Số TK", "Ngân Hàng, Chi Nhánh", "Mail", "Số điện thoại"
                };
            } else {
                headers = new String[]{
                        "Họ Và Tên Nhân Viên", "Chức Vụ", "Ngày Tháng Năm Sinh ", "Số CCCD",
                        "Ngày Cấp", "Nơi Cấp ", "Địa Chỉ \n(Trên CCCD)", "Mã Số Thuế",
                        "Số TK", "Ngân Hàng, Chi Nhánh", "Mail", "Số điện thoại",
                        "Nơi Làm Việc", "Tổng Tiền Lương", "Ảnh Mặt Trước CCCD", "Ảnh Mặt Sau CCCD"
                };
            }

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Employee emp : employees) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(emp.getFullName() != null ? emp.getFullName() : "");
                row.createCell(1).setCellValue(emp.getRole() != null ? emp.getRole() : "SUP");
                row.createCell(2).setCellValue(emp.getDob() != null ? emp.getDob() : "");
                row.createCell(3).setCellValue(emp.getIdCardNumber() != null ? emp.getIdCardNumber() : "");
                row.createCell(4).setCellValue(emp.getIdCardIssuedDate() != null ? emp.getIdCardIssuedDate() : "");
                row.createCell(5).setCellValue(emp.getIdCardIssuedPlace() != null ? emp.getIdCardIssuedPlace() : "");
                row.createCell(6).setCellValue(emp.getAddress() != null ? emp.getAddress() : "");
                row.createCell(7).setCellValue(emp.getTaxCode() != null ? emp.getTaxCode() : "");
                row.createCell(8).setCellValue(emp.getBankAccountNumber() != null ? emp.getBankAccountNumber() : "");
                row.createCell(9).setCellValue(emp.getBankInfo() != null ? emp.getBankInfo() : "");
                row.createCell(10).setCellValue(emp.getEmail() != null ? emp.getEmail() : "");
                row.createCell(11).setCellValue(emp.getPhone() != null ? emp.getPhone() : "");

                if (isFullMode) {
                    row.createCell(12).setCellValue(emp.getWorkplace() != null ? emp.getWorkplace() : "");
                    row.createCell(13).setCellValue(emp.getTotalSalary() != null ? emp.getTotalSalary() : "");
                    row.createCell(14).setCellValue(emp.getFrontIdUrl() != null ? emp.getFrontIdUrl() : "");
                    row.createCell(15).setCellValue(emp.getBackIdUrl() != null ? emp.getBackIdUrl() : "");
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // 2. Xuất Excel cho Bảng Đăng Ký Trực Tiếp (Khách tải được luôn)
    public byte[] exportLiveRegistrations(List<Employee> employees) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Dang Ky Truc Tiep");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"STT", "Họ Và Tên Nhân Viên", "Số Điện Thoại", "Số CCCD", "Cửa Hàng / Điểm Làm Việc", "Chức Vụ", "Ghi Chú (Note)"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(i + 1);
                row.createCell(1).setCellValue(emp.getFullName() != null ? emp.getFullName() : "");
                row.createCell(2).setCellValue(emp.getPhone() != null ? emp.getPhone() : "");
                row.createCell(3).setCellValue(emp.getIdCardNumber() != null ? emp.getIdCardNumber() : "");
                row.createCell(4).setCellValue(emp.getWorkplace() != null ? emp.getWorkplace() : "");
                row.createCell(5).setCellValue(emp.getRole() != null ? emp.getRole() : "SUP");
                row.createCell(6).setCellValue(emp.getNote() != null ? emp.getNote() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}