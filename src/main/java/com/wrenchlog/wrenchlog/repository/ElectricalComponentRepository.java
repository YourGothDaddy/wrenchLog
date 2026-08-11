package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ElectricalComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElectricalComponentRepository extends JpaRepository<ElectricalComponent, Long> {
    List<ElectricalComponent> findByVehicleId(Long vehicleId);
}