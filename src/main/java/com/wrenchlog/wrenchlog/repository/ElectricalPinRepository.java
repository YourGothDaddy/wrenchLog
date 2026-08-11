package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ElectricalPin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElectricalPinRepository extends JpaRepository<ElectricalPin, Long> {
    List<ElectricalPin> findByComponentId(Long componentId);
    List<ElectricalPin> findByComponentIdOrderByPositionAsc(Long componentId);
}