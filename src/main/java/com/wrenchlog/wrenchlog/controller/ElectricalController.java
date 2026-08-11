package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.*;
import com.wrenchlog.wrenchlog.model.*;
import com.wrenchlog.wrenchlog.service.ElectricalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/electrical")
public class ElectricalController {

    private final ElectricalService electricalService;

    public ElectricalController(ElectricalService electricalService) {
        this.electricalService = electricalService;
    }

    private ElectricalComponentResponseDTO toComponentDTO(ElectricalComponent c) {
        return new ElectricalComponentResponseDTO(c.getId(), c.getName(), c.getDescription(), c.getVehicle().getId());
    }

    private ElectricalPinResponseDTO toPinDTO(ElectricalPin p) {
        return new ElectricalPinResponseDTO(p.getId(), p.getName(), p.getExpectedRange(), p.getPosition());
    }

    private ElectricalReadingResponseDTO toReadingDTO(ElectricalReading r) {
        return new ElectricalReadingResponseDTO(r.getPin().getId(), r.getValue(), r.getUnit());
    }

    private ElectricalSessionResponseDTO toSessionDTO(ElectricalSession s) {
        List<ElectricalReadingResponseDTO> readings = electricalService.getReadingsForSession(s.getId())
                .stream().map(this::toReadingDTO).toList();
        return new ElectricalSessionResponseDTO(s.getId(), s.getLabel(), s.getSessionDate(), s.getNotes(), readings);
    }

    @GetMapping("/components")
    public List<ElectricalComponentResponseDTO> getComponents(@PathVariable Long vehicleId,
                                                              @AuthenticationPrincipal User user) {
        return electricalService.getComponentsForVehicle(vehicleId, user).stream().map(this::toComponentDTO).toList();
    }

    @PostMapping("/components")
    public ResponseEntity<ElectricalComponentResponseDTO> createComponent(
            @PathVariable Long vehicleId,
            @Valid @RequestBody ElectricalComponentCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalComponent saved = electricalService.createComponent(vehicleId, dto.name(), dto.description(), user);
        return new ResponseEntity<>(toComponentDTO(saved), HttpStatus.CREATED);
    }

    @GetMapping("/components/{componentId}")
    public ElectricalComponentDetailDTO getComponentDetail(@PathVariable Long vehicleId,
                                                           @PathVariable Long componentId,
                                                           @AuthenticationPrincipal User user) {
        ElectricalComponent component = electricalService.getOwnedComponentOrThrow(vehicleId, componentId, user);
        List<ElectricalPinResponseDTO> pins = electricalService.getPinsForComponent(componentId)
                .stream().map(this::toPinDTO).toList();
        List<ElectricalSessionResponseDTO> sessions = electricalService.getSessionsForComponent(componentId)
                .stream().map(this::toSessionDTO).toList();
        return new ElectricalComponentDetailDTO(component.getId(), component.getName(), component.getDescription(),
                vehicleId, pins, sessions);
    }

    @PutMapping("/components/{componentId}")
    public ResponseEntity<ElectricalComponentResponseDTO> updateComponent(
            @PathVariable Long vehicleId, @PathVariable Long componentId,
            @Valid @RequestBody ElectricalComponentCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalComponent updated = electricalService.updateComponent(vehicleId, componentId, dto.name(), dto.description(), user);
        return ResponseEntity.ok(toComponentDTO(updated));
    }

    @DeleteMapping("/components/{componentId}")
    public ResponseEntity<Void> deleteComponent(@PathVariable Long vehicleId, @PathVariable Long componentId,
                                                @AuthenticationPrincipal User user) {
        electricalService.deleteComponent(vehicleId, componentId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/components/{componentId}/pins")
    public ResponseEntity<ElectricalPinResponseDTO> createPin(
            @PathVariable Long vehicleId, @PathVariable Long componentId,
            @Valid @RequestBody ElectricalPinCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalPin saved = electricalService.createPin(vehicleId, componentId, dto.name(), dto.expectedRange(), user);
        return new ResponseEntity<>(toPinDTO(saved), HttpStatus.CREATED);
    }

    @PutMapping("/components/{componentId}/pins/{pinId}")
    public ResponseEntity<ElectricalPinResponseDTO> updatePin(
            @PathVariable Long vehicleId, @PathVariable Long componentId, @PathVariable Long pinId,
            @Valid @RequestBody ElectricalPinCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalPin updated = electricalService.updatePin(vehicleId, componentId, pinId, dto.name(), dto.expectedRange(), user);
        return ResponseEntity.ok(toPinDTO(updated));
    }

    @DeleteMapping("/components/{componentId}/pins/{pinId}")
    public ResponseEntity<Void> deletePin(@PathVariable Long vehicleId, @PathVariable Long componentId,
                                          @PathVariable Long pinId, @AuthenticationPrincipal User user) {
        electricalService.deletePin(vehicleId, componentId, pinId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/components/{componentId}/pins/{pinId}/move")
    public ResponseEntity<Void> movePin(
            @PathVariable Long vehicleId, @PathVariable Long componentId, @PathVariable Long pinId,
            @RequestParam String direction,
            @AuthenticationPrincipal User user
    ) {
        electricalService.movePin(vehicleId, componentId, pinId, direction, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/components/{componentId}/sessions")
    public ResponseEntity<ElectricalSessionResponseDTO> createSession(
            @PathVariable Long vehicleId, @PathVariable Long componentId,
            @Valid @RequestBody ElectricalSessionCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalSession saved = electricalService.createSession(
                vehicleId, componentId, dto.label(), dto.sessionDate(), dto.notes(), user);
        return new ResponseEntity<>(toSessionDTO(saved), HttpStatus.CREATED);
    }

    @PutMapping("/components/{componentId}/sessions/{sessionId}")
    public ResponseEntity<ElectricalSessionResponseDTO> updateSession(
            @PathVariable Long vehicleId, @PathVariable Long componentId, @PathVariable Long sessionId,
            @Valid @RequestBody ElectricalSessionCreateDTO dto,
            @AuthenticationPrincipal User user
    ) {
        ElectricalSession updated = electricalService.updateSession(
                vehicleId, componentId, sessionId, dto.label(), dto.sessionDate(), dto.notes(), user);
        return ResponseEntity.ok(toSessionDTO(updated));
    }

    @DeleteMapping("/components/{componentId}/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long vehicleId, @PathVariable Long componentId,
                                              @PathVariable Long sessionId, @AuthenticationPrincipal User user) {
        electricalService.deleteSession(vehicleId, componentId, sessionId, user);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/components/{componentId}/sessions/{sessionId}/readings/{pinId}")
    public ResponseEntity<ElectricalReadingResponseDTO> upsertReading(
            @PathVariable Long vehicleId, @PathVariable Long componentId,
            @PathVariable Long sessionId, @PathVariable Long pinId,
            @Valid @RequestBody ElectricalReadingUpsertDTO dto,
            @AuthenticationPrincipal User user
    ) {
        Optional<ElectricalReading> result = electricalService.upsertReading(
                vehicleId, componentId, sessionId, pinId, dto, user);

        return result
                .map(reading -> ResponseEntity.ok(toReadingDTO(reading)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}