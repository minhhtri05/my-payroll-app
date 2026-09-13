package com.example.mypayrollapp.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ImportResult {
    private int totalRows;       // Tổng số dòng đọc được
    private int successCount;    // Số nhân sự mới được thêm
    private int duplicateCount;  // Số nhân sự bị trùng CCCD (bỏ qua)
    private String message;
}