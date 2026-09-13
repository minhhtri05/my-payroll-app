package com.example.mypayrollapp.repository;

import com.example.mypayrollapp.entity.StoreLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreLocationRepository extends JpaRepository<StoreLocation, Long> {
    List<StoreLocation> findByIsActiveTrueOrderByNameAsc();
}