package com.example.mypayrollapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, length = 150)
    private String fullName; // 1. Họ Và Tên

    @Nationalized
    @Column(length = 50)
    private String role; // 2. Chức Vụ

    @Nationalized
    @Column(length = 50)
    private String dob; // 3. Ngày Sinh

    @Column(nullable = false, length = 50)
    private String idCardNumber; // 4. Số CCCD

    @Nationalized
    @Column(length = 50)
    private String idCardIssuedDate; // 5. Ngày Cấp

    @Nationalized
    @Column(length = 255)
    private String idCardIssuedPlace; // 6. Nơi Cấp (BỘ CÔNG AN / Cục CS QLHC và TTXH)

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String address; // 7. Địa Chỉ (Trên CCCD)

    @Column(length = 50)
    private String taxCode; // 8. Mã Số Thuế

    @Column(nullable = false, length = 50)
    private String bankAccountNumber; // 9. Số TK

    @Nationalized
    @Column(length = 150)
    private String bankName; // Tên ngân hàng tách riêng

    @Nationalized
    @Column(length = 150)
    private String bankBranch; // Chi nhánh tách riêng

    @Nationalized
    @Column(nullable = false, length = 255)
    private String bankInfo; // 10. Ngân Hàng, Chi Nhánh (ghép tự động để xuất file)

    @Column(length = 150)
    private String email; // 11. Mail

    @Column(length = 50)
    private String phone; // 12. Số Điện Thoại

    @Nationalized
    @Column(length = 255)
    private String workplace; // 13. Nơi Làm Việc (Cửa hàng / Mall)

    @Nationalized
    @Column(length = 100)
    private String totalSalary; // 14. Tổng Tiền Lương

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String frontIdUrl; // 15. Ảnh Mặt Trước CCCD

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String backIdUrl; // 16. Ảnh Mặt Sau CCCD

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String note; // Ghi chú (Note)

    @Column(length = 20)
    @Builder.Default
    private String employeeType = "PERMANENT"; // PERMANENT hoặc TEMPORARY

    @Builder.Default
    private Boolean isActive = true;
}