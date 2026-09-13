package com.example.mypayrollapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "plan_assignments", indexes = {
        // Đánh index để truy vấn kiểm tra trùng lịch chạy nhanh
        @Index(name = "idx_emp_workdate", columnList = "employee_id, work_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate workDate; // Ngày làm việc cụ thể

    private String location; // Điểm làm việc (Cửa hàng / Mall)

    @Column(nullable = false, length = 50)
    private String role; // Vị trí: SUP, PG, PB...

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal salaryAmount; // Tiền lương ca/ngày

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal allowance = BigDecimal.ZERO; // Tiền phụ cấp (gửi xe, bốc xếp...)

    private String note;
}