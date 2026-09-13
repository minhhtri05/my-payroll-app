package com.example.mypayrollapp.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // Mã đợt (ví dụ: GRAB_T5_2026)

    @Column(nullable = false, length = 200)
    private String name; // Tên đợt (ví dụ: Grab Dine Out Đợt 1)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private PlanStatus status = PlanStatus.ACTIVE;

    public enum PlanStatus {
        PLANNING,
        ACTIVE,
        COMPLETED
    }
}