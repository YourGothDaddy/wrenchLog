package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.VehicleFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleFolderRepository extends JpaRepository<VehicleFolder, Long> {
    List<VehicleFolder> findByVehicleId(Long vehicleId);
}