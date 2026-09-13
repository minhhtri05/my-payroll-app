package com.example.mypayrollapp.service;

import com.example.mypayrollapp.dto.ImportResult;
import com.example.mypayrollapp.entity.Employee;
import com.example.mypayrollapp.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeImportService {

    private final EmployeeRepository employeeRepository;

    public ImportResult importFromExcel(MultipartFile file) throws Exception {
        int total = 0;
        int duplicate = 0;
        List<Employee> employeesToSave = new ArrayList<>();
        Set<String> seenIdCards = new HashSet<>();

        // 1. Đọc file Excel
        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                return ImportResult.builder().message("File Excel trống hoặc không có dòng tiêu đề!").build();
            }

            // 2. Nhận diện độc lập từng cột trong 12 cột chuẩn
            Map<String, Integer> colMap = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                String header = getCellValue(headerRow.getCell(c)).toLowerCase().trim();

                if (header.contains("họ và tên") || header.contains("họ tên")) colMap.put("fullName", c);
                else if (header.contains("chức vụ")) colMap.put("role", c);
                else if (header.contains("ngày sinh") || header.contains("năm sinh")) colMap.put("dob", c);
                else if (header.contains("ngày cấp")) colMap.put("idCardIssuedDate", c); // Bắt riêng Ngày Cấp
                else if (header.contains("nơi cấp")) colMap.put("idCardIssuedPlace", c); // Bắt riêng Nơi Cấp
                else if (header.contains("địa chỉ")) colMap.put("address", c);           // Bắt riêng Địa Chỉ
                else if (header.contains("cccd") || header.contains("cmnd")) {
                    if (!header.contains("mặt trước") && !header.contains("mặt sau")) {
                        colMap.put("idCardNumber", c); // Số CCCD
                    }
                }
                else if (header.contains("mã số thuế") || header.contains("mst")) colMap.put("taxCode", c);
                else if (header.contains("số tk") || header.contains("tài khoản")) colMap.put("bankAccount", c);
                else if (header.contains("ngân hàng")) colMap.put("bankInfo", c);
                else if (header.contains("mail") || header.contains("email")) colMap.put("email", c);
                else if (header.contains("số điện thoại") || header.contains("sđt") || header.contains("phone")) colMap.put("phone", c);
            }

            // 3. Đọc dữ liệu từng dòng
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String fullName = getVal(row, colMap.get("fullName"));
                String idCard = getVal(row, colMap.get("idCardNumber")).replaceAll("[^0-9]", "");

                if (fullName.isBlank() || idCard.isBlank()) continue;
                total++;

                // Lọc trùng CCCD
                if (seenIdCards.contains(idCard) || employeeRepository.findByIdCardNumber(idCard).isPresent()) {
                    duplicate++;
                    continue;
                }
                seenIdCards.add(idCard);

                String role = getVal(row, colMap.get("role"));
                String dob = getVal(row, colMap.get("dob"));
                String idCardDate = getVal(row, colMap.get("idCardIssuedDate"));
                String idCardPlace = getVal(row, colMap.get("idCardIssuedPlace"));
                String address = getVal(row, colMap.get("address"));
                String taxCode = getVal(row, colMap.get("taxCode"));
                String bankAccount = getVal(row, colMap.get("bankAccount")).replaceAll("[^0-9a-zA-Z]", "");
                String bankInfo = getVal(row, colMap.get("bankInfo"));
                String email = getVal(row, colMap.get("email"));
                String phone = getVal(row, colMap.get("phone"));

                Employee employee = Employee.builder()
                        .fullName(fullName.trim())
                        .role(role.isBlank() ? "SUP" : role.trim())
                        .dob(dob.trim())
                        .idCardNumber(idCard)
                        .idCardIssuedDate(idCardDate.trim())
                        .idCardIssuedPlace(idCardPlace.trim())
                        .address(address.trim())
                        .taxCode(taxCode.trim())
                        .bankAccountNumber(bankAccount.trim())
                        .bankInfo(bankInfo.trim())
                        .email(email.trim())
                        .phone(phone.trim())
                        .isActive(true)
                        .build();

                employeesToSave.add(employee);
            }
        }

        // 4. Lưu toàn bộ vào Database
        if (!employeesToSave.isEmpty()) {
            employeeRepository.saveAll(employeesToSave);
        }

        return ImportResult.builder()
                .totalRows(total)
                .successCount(employeesToSave.size())
                .duplicateCount(duplicate)
                .message(String.format("Import thành công %d nhân sự đầy đủ 12 cột (Đã lọc bỏ %d dòng trùng CCCD).", employeesToSave.size(), duplicate))
                .build();
    }

    private String getVal(Row row, Integer colIdx) {
        if (row == null || colIdx == null) return "";
        return getCellValue(row.getCell(colIdx));
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}