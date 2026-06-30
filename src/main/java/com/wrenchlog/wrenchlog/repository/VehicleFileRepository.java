package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.VehicleFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleFileRepository extends JpaRepository<VehicleFile, Long> {
    List<VehicleFile> findByVehicleId(Long vehicleId);
}