package com.example.mypayrollapp.repository;

import com.example.mypayrollapp.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByIdCardNumber(String idCardNumber);

    // Dùng đúng chuẩn cú pháp của Spring Data JPA
    List<Employee> findAllByOrderByIdDesc();

    List<Employee> findByIsActiveFalseOrderByIdDesc();
}