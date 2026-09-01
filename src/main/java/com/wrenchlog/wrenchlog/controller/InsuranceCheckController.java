package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.InsuranceCheckResponseDTO;
import com.wrenchlog.wrenchlog.enums.ReminderSourceType;
import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.service.InsuranceCheckService;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/insurance-check")
public class InsuranceCheckController {

    private final VehicleAccessService vehicleAccessService;
    private final ServiceReminderRepository serviceReminderRepository;
    private final InsuranceCheckService insuranceCheckService;

    public InsuranceCheckController(VehicleAccessService vehicleAccessService,
                                    ServiceReminderRepository serviceReminderRepository,
                                    InsuranceCheckService insuranceCheckService) {
        this.vehicleAccessService = vehicleAccessService;
        this.serviceReminderRepository = serviceReminderRepository;
        this.insuranceCheckService = insuranceCheckService;
    }

    @PostMapping
    public ResponseEntity<InsuranceCheckResponseDTO> checkInsurance(@PathVariable Long vehicleId,
                                                                    @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        if (vehicle.getPlateNumber() == null || vehicle.getPlateNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No plate number on file for this vehicle");
        }

        InsuranceCheckService.InsuranceResult result = insuranceCheckService.checkInsurance(vehicle.getPlateNumber());

        ServiceReminder insuranceReminder = serviceReminderRepository.findByVehicleId(vehicle.getId())
                .stream()
                .filter(r -> r.getSourceType() == ReminderSourceType.INSURANCE)
                .findFirst()
                .orElse(null);

        boolean hasLocalReminder = insuranceReminder != null
                && insuranceReminder.getLastServiceAtDate() != null
                && insuranceReminder.getIntervalMonths() != null;

        LocalDate enteredExpiryDate = hasLocalReminder
                ? insuranceReminder.getLastServiceAtDate().plusMonths(insuranceReminder.getIntervalMonths())
                : null;

        if (!result.found()) {
            if (hasLocalReminder) {
                insuranceReminder.setVerifiedExpiryDate(null);
                serviceReminderRepository.save(insuranceReminder);
            }
            String message = hasLocalReminder ? "No active insurance found" : "No insurance data found";
            return ResponseEntity.ok(new InsuranceCheckResponseDTO(hasLocalReminder, enteredExpiryDate, false, null, null, false, message));
        }

        if (!hasLocalReminder) {
            return ResponseEntity.ok(new InsuranceCheckResponseDTO(false, null, true, result.insurerName(), result.endDate(), false,
                    "Insurance found via Guarantee Fund, not yet saved as a reminder"));
        }

        boolean match = enteredExpiryDate.isEqual(result.endDate());

        insuranceReminder.setVerifiedExpiryDate(match ? result.endDate() : null);
        serviceReminderRepository.save(insuranceReminder);

        String message = match ? "Confirmed by Guarantee Fund" : "Date does not match Guarantee Fund records";
        return ResponseEntity.ok(new InsuranceCheckResponseDTO(true, enteredExpiryDate, true, result.insurerName(), result.endDate(), match, message));
    }
}