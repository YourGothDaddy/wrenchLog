package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reminders")
public class ServiceReminderController {
    private final VehicleRepository vehicleRepository;
    private final ServiceReminderRepository serviceReminderRepository;

    public ServiceReminderController(VehicleRepository vehicleRepository,
                                     ServiceReminderRepository serviceReminderRepository) {
        this.vehicleRepository = vehicleRepository;
        this.serviceReminderRepository = serviceReminderRepository;
    }

    @GetMapping
    public List<ServiceReminder> getServiceRemindersForVehicle(@RequestParam Long vehicleId){
        return serviceReminderRepository.findByVehicleId(vehicleId);
    }

    @PostMapping
    public ResponseEntity<ServiceReminder> addServiceReminder(@RequestParam Long vehicleId, @RequestBody ServiceReminder serviceReminder){
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    serviceReminder.setVehicle(vehicle);
                    serviceReminder.setCreatedAt(LocalDateTime.now());
                    ServiceReminder savedReminder = serviceReminderRepository.save(serviceReminder);
                    return new ResponseEntity<>(savedReminder, HttpStatus.CREATED);
                })
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceReminder(@PathVariable Long id){
        if(serviceReminderRepository.existsById(id)){
            serviceReminderRepository.deleteById(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }else{
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceReminder> modifyServiceReminder(@PathVariable Long id,
                                                                 @RequestParam Long vehicleId,
                                                                 @RequestBody ServiceReminder serviceReminder){
        if(serviceReminderRepository.existsById(id)){
            return vehicleRepository.findById(vehicleId)
                    .map(vehicle -> {
                        serviceReminder.setVehicle(vehicle);
                        serviceReminder.setId(id);
                        serviceReminder.setCreatedAt(LocalDateTime.now());
                        ServiceReminder savedReminder = serviceReminderRepository.save(serviceReminder);
                        return new ResponseEntity<>(savedReminder, HttpStatus.OK);
                    })
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
