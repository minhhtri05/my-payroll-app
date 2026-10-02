package com.example.mypayrollapp.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false, length = 255)
    private String fullName; // 1. Họ Và Tên

    @Column(length = 100)
    private String role; // 2. Chức Vụ

    @Column(length = 100)
    private String dob; // 3. Ngày Sinh

    @Column(nullable = false, length = 100)
    private String idCardNumber; // 4. Số CCCD / CMND

    @Column(length = 100)
    private String idCardIssuedDate; // 5. Ngày Cấp

    @Column(length = 255)
    private String idCardIssuedPlace; // 6. Nơi Cấp

    @Column(length = 1000)
    private String address; // 7. Địa Chỉ (Trên CCCD)

    @Column(length = 100)
    private String taxCode; // 8. Mã Số Thuế

    @Column(length = 100)
    private String bankAccountNumber; // 9. Số TK

    @Column(length = 255)
    private String bankName;

    @Column(length = 255)
    private String bankBranch;

    @Column(length = 500)
    private String bankInfo; // 10. Ngân Hàng, Chi Nhánh

    @Column(length = 255)
    private String email; // 11. Mail

    @Column(length = 100)
    private String phone; // 12. Số Điện Thoại

    @Column(length = 255)
    private String workplace; // 13. Nơi Làm Việc

    @Column(length = 255)
    private String totalSalary; // 14. Tổng Tiền Lương

    @Column(length = 1000)
    private String frontIdUrl; // 15. Ảnh Mặt Trước CCCD

    @Column(length = 1000)
    private String backIdUrl; // 16. Ảnh Mặt Sau CCCD

    @Column(length = 1000)
    private String note; // Ghi chú (Note)

    @Column(length = 50)
    @Builder.Default
    private String employeeType = "PERMANENT";

    @Builder.Default
    private Boolean isActive = true;
}