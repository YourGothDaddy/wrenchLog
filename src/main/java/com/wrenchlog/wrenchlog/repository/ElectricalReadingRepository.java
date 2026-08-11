package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ElectricalReading;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ElectricalReadingRepository extends JpaRepository<ElectricalReading, Long> {
    List<ElectricalReading> findBySessionId(Long sessionId);
    List<ElectricalReading> findByPinId(Long pinId);
    Optional<ElectricalReading> findBySessionIdAndPinId(Long sessionId, Long pinId);
}