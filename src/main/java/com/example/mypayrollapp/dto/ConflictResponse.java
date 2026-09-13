package com.example.mypayrollapp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResponse {
    private Long employeeId;
    private String employeeName;
    private String idCardNumber;
    private LocalDate workDate;
    private String currentPlanName;
    private String conflictingPlanName;
    private String currentLocation;
    private String conflictingLocation;
}