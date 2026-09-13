package com.example.mypayrollapp.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class AssignmentRequest {
    private Long employeeId;
    private LocalDate workDate;
    private String location;
    private String role;
    private BigDecimal salaryAmount;
    private BigDecimal allowance;
    private String note;
}