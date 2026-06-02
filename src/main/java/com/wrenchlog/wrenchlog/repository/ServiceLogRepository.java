package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ServiceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceLogRepository extends JpaRepository<ServiceLog, Long> {
    List<ServiceLog> findByVehicleId(Long vehicleId);
}
