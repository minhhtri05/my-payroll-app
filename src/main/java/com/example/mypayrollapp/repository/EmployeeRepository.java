package com.example.mypayrollapp.repository;

import com.example.mypayrollapp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByIdCardNumber(String idCardNumber);

    // Lấy danh sách nhân sự còn hoạt động (chưa xóa)
    @Query("SELECT e FROM Employee e WHERE (e.employeeType = :type OR (:type = 'PERMANENT' AND e.employeeType IS NULL)) AND (e.isActive = true OR e.isActive IS NULL) ORDER BY e.id DESC")
    List<Employee> findActiveByType(@Param("type") String type);

    // Lấy danh sách trong Thùng rác
    List<Employee> findByIsActiveFalseOrderByIdDesc();
}