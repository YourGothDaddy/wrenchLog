package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ElectricalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ElectricalSessionRepository extends JpaRepository<ElectricalSession, Long> {
    List<ElectricalSession> findByComponentId(Long componentId);
}