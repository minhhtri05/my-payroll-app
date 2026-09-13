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
    private String idCardIssuedPlace; // 6. Nơi Cấp

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String address; // 7. Địa Chỉ

    @Column(length = 50)
    private String taxCode; // 8. Mã Số Thuế

    @Column(nullable = false, length = 50)
    private String bankAccountNumber; // 9. Số TK

    @Nationalized
    @Column(nullable = false, length = 255)
    private String bankInfo; // 10. Ngân Hàng, Chi Nhánh

    @Column(length = 150)
    private String email; // 11. Mail

    @Column(length = 50)
    private String phone; // 12. Số Điện Thoại

    // 4 trường bổ sung cho Vãng lai
    @Nationalized
    @Column(length = 255)
    private String workplace; // 13. Nơi Làm Việc (Cửa hàng / Điểm chạy)

    @Nationalized
    @Column(length = 100)
    private String totalSalary; // 14. Tổng Tiền Lương Của Team

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String frontIdUrl; // 15. Ảnh Mặt Trước CCCD

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String backIdUrl; // 16. Ảnh Mặt Sau CCCD

    @Column(length = 20)
    @Builder.Default
    private String employeeType = "PERMANENT"; // PERMANENT hoặc TEMPORARY

    @Builder.Default
    private Boolean isActive = true; // true: Đang hoạt động, false: Đã xóa mềm (trong Thùng rác)
}