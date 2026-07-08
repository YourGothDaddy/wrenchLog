package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.ServiceReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceReminderRepository extends JpaRepository<ServiceReminder, Long> {
    List<ServiceReminder> findByVehicleId(Long vehicleId);
}
