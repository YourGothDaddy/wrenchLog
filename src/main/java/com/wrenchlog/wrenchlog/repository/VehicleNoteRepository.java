package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.VehicleNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleNoteRepository extends JpaRepository<VehicleNote, Long> {
    List<VehicleNote> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);
}
