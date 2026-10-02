package com.example.mypayrollapp.service;

import com.example.mypayrollapp.dto.ImportResult;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeImportService {

    private final EmployeeRepository employeeRepository;

    @Transactional
    public ImportResult importFromExcel(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ImportResult.builder().success(false).message("File tải lên trống!").build();
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            DataFormatter formatter = new DataFormatter();
            Sheet targetSheet = null;
            int headerRowIndex = -1;
            Map<String, Integer> colMap = new HashMap<>();

            // Quét tìm Sheet chứa danh sách nhân sự (nhận diện cả CCCD và CMND)
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                Sheet sheet = workbook.getSheetAt(s);
                int lastRow = Math.min(sheet.getLastRowNum(), 100);

                for (int r = 0; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    Map<String, Integer> tempMap = scanHeaderRow(row, formatter);
                    if (tempMap.containsKey("fullName") && tempMap.containsKey("idCardNumber") && tempMap.containsKey("bankAccountNumber")) {
                        targetSheet = sheet;
                        headerRowIndex = r;
                        colMap = tempMap;
                        break;
                    }
                }
                if (targetSheet != null) break;
            }

            if (targetSheet == null) {
                return ImportResult.builder()
                        .success(false)
                        .message("Không tìm thấy Sheet chứa thông tin nhân sự (yêu cầu cột Họ Tên, Số CCCD/CMND, Số TK)!")
                        .build();
            }

            int count = 0;
            int totalRows = targetSheet.getLastRowNum();

            for (int r = headerRowIndex + 1; r <= totalRows; r++) {
                Row row = targetSheet.getRow(r);
                if (row == null) continue;

                String fullName = getVal(row, colMap.get("fullName"), formatter);
                String idCard = getVal(row, colMap.get("idCardNumber"), formatter);
                String cleanIdCard = idCard.replaceAll("[^0-9]", "").trim();

                // Bỏ qua dòng trống hoặc dòng chữ ký cuối trang
                if (fullName.isBlank() || cleanIdCard.isBlank()) continue;
                if (fullName.toLowerCase().contains("trưởng bộ phận") || fullName.toLowerCase().contains("kế toán")) continue;

                String role = getVal(row, colMap.get("role"), formatter);
                if (role.isBlank()) role = "SUP";
                String dob = getVal(row, colMap.get("dob"), formatter);
                String issuedDate = getVal(row, colMap.get("idCardIssuedDate"), formatter);
                String issuedPlace = getVal(row, colMap.get("idCardIssuedPlace"), formatter);
                String address = getVal(row, colMap.get("address"), formatter);
                String taxCode = getVal(row, colMap.get("taxCode"), formatter);
                String bankAcc = getVal(row, colMap.get("bankAccountNumber"), formatter);
                String bankInfo = getVal(row, colMap.get("bankInfo"), formatter);
                String email = getVal(row, colMap.get("email"), formatter);
                String phone = getVal(row, colMap.get("phone"), formatter);
                String salary = getVal(row, colMap.get("totalSalary"), formatter);

                Optional<Employee> existing = employeeRepository.findByIdCardNumber(cleanIdCard);
                Employee emp = existing.orElseGet(Employee::new);

                emp.setFullName(fullName.trim());
                emp.setRole(role);
                emp.setDob(dob);
                emp.setIdCardNumber(cleanIdCard);
                emp.setIdCardIssuedDate(issuedDate);
                emp.setIdCardIssuedPlace(!issuedPlace.isBlank() ? issuedPlace : "Cục CS QLHC và TTXH");
                emp.setAddress(address);
                emp.setTaxCode(!taxCode.isBlank() ? taxCode : cleanIdCard);
                emp.setBankAccountNumber(bankAcc);
                emp.setBankInfo(bankInfo);
                emp.setEmail(email);
                emp.setPhone(phone);
                if (!salary.isBlank()) emp.setTotalSalary(salary);
                emp.setEmployeeType("PERMANENT");
                emp.setIsActive(true);

                employeeRepository.save(emp);
                count++;
            }

            return ImportResult.builder()
                    .success(true)
                    .message(String.format("Import thành công! Đã nạp %d nhân sự từ sheet '%s'.", count, targetSheet.getSheetName()))
                    .totalImported(count)
                    .build();
        }
    }

    private Map<String, Integer> scanHeaderRow(Row row, DataFormatter formatter) {
        Map<String, Integer> map = new HashMap<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            String text = formatter.formatCellValue(cell).toLowerCase().trim();

            if (text.contains("họ tên") || text.contains("họ và tên") || text.contains("tên nhân viên") || text.equals("tên nv")) {
                map.putIfAbsent("fullName", c);
            } else if (text.contains("chức vụ") || text.contains("chức danh")) {
                map.putIfAbsent("role", c);
            } else if (text.contains("ngày sinh") || text.contains("năm sinh") || text.contains("ngày tháng năm sinh")) {
                map.putIfAbsent("dob", c);
            } else if (text.contains("cccd") || text.contains("cmnd")) {
                map.putIfAbsent("idCardNumber", c);
            } else if (text.contains("ngày cấp")) {
                map.putIfAbsent("idCardIssuedDate", c);
            } else if (text.contains("nơi cấp")) {
                map.putIfAbsent("idCardIssuedPlace", c);
            } else if (text.contains("địa chỉ")) {
                map.putIfAbsent("address", c);
            } else if (text.contains("mã số thuế") || text.contains("mst")) {
                map.putIfAbsent("taxCode", c);
            } else if (text.contains("số tk") || text.contains("tài khoản") || text.contains("stk") || text.contains("số tài khoản")) {
                map.putIfAbsent("bankAccountNumber", c);
            } else if (text.contains("ngân hàng") || text.contains("chi nhánh")) {
                map.putIfAbsent("bankInfo", c);
            } else if (text.contains("mail") || text.contains("email")) {
                map.putIfAbsent("email", c);
            } else if (text.contains("điện thoại") || text.contains("sđt") || text.contains("số đt")) {
                map.putIfAbsent("phone", c);
            } else if (text.contains("lương") || text.contains("số tiền")) {
                map.putIfAbsent("totalSalary", c);
            }
        }
        return map;
    }

    private String getVal(Row row, Integer colIdx, DataFormatter formatter) {
        if (colIdx == null || colIdx < 0) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }
}