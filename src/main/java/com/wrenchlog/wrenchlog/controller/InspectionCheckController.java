package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.InspectionCheckResponseDTO;
import com.wrenchlog.wrenchlog.dto.InspectionCheckStartResponseDTO;
import com.wrenchlog.wrenchlog.dto.InspectionCheckSubmitRequestDTO;
import com.wrenchlog.wrenchlog.enums.ReminderSourceType;
import com.wrenchlog.wrenchlog.model.ServiceReminder;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.ServiceReminderRepository;
import com.wrenchlog.wrenchlog.service.InspectionCheckService;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Base64;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/inspection-check")
public class InspectionCheckController {

    private final VehicleAccessService vehicleAccessService;
    private final ServiceReminderRepository serviceReminderRepository;
    private final InspectionCheckService inspectionCheckService;

    public InspectionCheckController(VehicleAccessService vehicleAccessService,
                                     ServiceReminderRepository serviceReminderRepository,
                                     InspectionCheckService inspectionCheckService) {
        this.vehicleAccessService = vehicleAccessService;
        this.serviceReminderRepository = serviceReminderRepository;
        this.inspectionCheckService = inspectionCheckService;
    }

    @PostMapping("/start")
    public ResponseEntity<InspectionCheckStartResponseDTO> startCheck(@PathVariable Long vehicleId,
                                                                      @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        if (vehicle.getPlateNumber() == null || vehicle.getPlateNumber().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No plate number on file for this vehicle");
        }

        InspectionCheckService.CaptchaResult result = inspectionCheckService.fetchCaptcha(vehicle.getPlateNumber());
        String base64Image = Base64.getEncoder().encodeToString(result.imageBytes());

        return ResponseEntity.ok(new InspectionCheckStartResponseDTO(result.sessionToken(), base64Image));
    }

    @PostMapping("/submit")
    public ResponseEntity<InspectionCheckResponseDTO> submitCheck(@PathVariable Long vehicleId,
                                                                  @Valid @RequestBody InspectionCheckSubmitRequestDTO dto,
                                                                  @AuthenticationPrincipal User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);

        InspectionCheckService.InspectionResult rtaResult =
                inspectionCheckService.submitCaptcha(dto.sessionToken(), dto.captchaCode());

        if (rtaResult.captchaInvalid()) {
            return ResponseEntity.ok(new InspectionCheckResponseDTO(
                    false, null, false, null, false, true,
                    "Could not read the captcha code. Please try again with a new image."));
        }

        ServiceReminder inspectionReminder = serviceReminderRepository.findByVehicleId(vehicle.getId())
                .stream()
                .filter(r -> r.getSourceType() == ReminderSourceType.INSPECTION)
                .findFirst()
                .orElse(null);

        boolean hasLocalReminder = inspectionReminder != null
                && inspectionReminder.getLastServiceAtDate() != null
                && inspectionReminder.getIntervalMonths() != null;

        LocalDate enteredExpiryDate = hasLocalReminder
                ? inspectionReminder.getLastServiceAtDate().plusMonths(inspectionReminder.getIntervalMonths())
                : null;

        if (rtaResult.inspectionExpiryDate() == null) {
            if (hasLocalReminder) {
                inspectionReminder.setVerifiedExpiryDate(null);
                serviceReminderRepository.save(inspectionReminder);
            }
            String message = hasLocalReminder ? "RTA lookup returned no valid inspection" : "No inspection data found";
            return ResponseEntity.ok(new InspectionCheckResponseDTO(hasLocalReminder, enteredExpiryDate, false, null, false, false, message));
        }

        if (!hasLocalReminder) {
            return ResponseEntity.ok(new InspectionCheckResponseDTO(false, null, true, rtaResult.inspectionExpiryDate(), false, false,
                    "Inspection found via RTA, not yet saved as a reminder"));
        }

        boolean match = enteredExpiryDate.isEqual(rtaResult.inspectionExpiryDate());

        inspectionReminder.setVerifiedExpiryDate(match ? rtaResult.inspectionExpiryDate() : null);
        serviceReminderRepository.save(inspectionReminder);

        String message = match ? "Confirmed by RTA" : "Date does not match RTA records";
        return ResponseEntity.ok(new InspectionCheckResponseDTO(true, enteredExpiryDate, true, rtaResult.inspectionExpiryDate(), match, false, message));
    }
}