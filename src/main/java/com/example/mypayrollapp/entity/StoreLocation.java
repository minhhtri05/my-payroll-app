package com.example.mypayrollapp.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "store_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nationalized
    @Column(nullable = false, unique = true, length = 255)
    private String name; // Tên cửa hàng / điểm làm việc (ví dụ: Haidilao - Vạn Hạnh Mall)

    @Builder.Default
    private Boolean isActive = true;
}