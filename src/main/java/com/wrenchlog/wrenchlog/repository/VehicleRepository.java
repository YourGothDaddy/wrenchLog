package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
