package com.example.mypayrollapp.dto;

import com.example.mypayrollapp.entity.Employee;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private List<Employee> employees;
    private List<Map<String, String>> columns; // Danh sách cột theo thứ tự tùy chỉnh
}