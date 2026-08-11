package com.wrenchlog.wrenchlog.service;

import com.wrenchlog.wrenchlog.dto.ElectricalReadingUpsertDTO;
import com.wrenchlog.wrenchlog.model.*;
import com.wrenchlog.wrenchlog.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ElectricalService {

    private final VehicleAccessService vehicleAccessService;
    private final ElectricalComponentRepository componentRepository;
    private final ElectricalPinRepository pinRepository;
    private final ElectricalSessionRepository sessionRepository;
    private final ElectricalReadingRepository readingRepository;

    public ElectricalService(VehicleAccessService vehicleAccessService,
                             ElectricalComponentRepository componentRepository,
                             ElectricalPinRepository pinRepository,
                             ElectricalSessionRepository sessionRepository,
                             ElectricalReadingRepository readingRepository) {
        this.vehicleAccessService = vehicleAccessService;
        this.componentRepository = componentRepository;
        this.pinRepository = pinRepository;
        this.sessionRepository = sessionRepository;
        this.readingRepository = readingRepository;
    }

    public ElectricalComponent getOwnedComponentOrThrow(Long vehicleId, Long componentId, User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        ElectricalComponent component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found"));
        if (!component.getVehicle().getId().equals(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found");
        }
        return component;
    }

    public ElectricalPin getOwnedPinOrThrow(Long vehicleId, Long componentId, Long pinId, User user) {
        getOwnedComponentOrThrow(vehicleId, componentId, user);
        ElectricalPin pin = pinRepository.findById(pinId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin not found"));
        if (!pin.getComponent().getId().equals(componentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin not found");
        }
        return pin;
    }

    public ElectricalSession getOwnedSessionOrThrow(Long vehicleId, Long componentId, Long sessionId, User user) {
        getOwnedComponentOrThrow(vehicleId, componentId, user);
        ElectricalSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getComponent().getId().equals(componentId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        return session;
    }

    public ElectricalPin createPin(Long vehicleId, Long componentId, String name, String expectedRange, User user) {
        ElectricalComponent component = getOwnedComponentOrThrow(vehicleId, componentId, user);
        List<ElectricalPin> existing = pinRepository.findByComponentIdOrderByPositionAsc(componentId);
        int nextPosition = existing.isEmpty() ? 1 : existing.get(existing.size() - 1).getPosition() + 1;

        ElectricalPin pin = new ElectricalPin(name, expectedRange, component);
        pin.setPosition(nextPosition);
        return pinRepository.save(pin);
    }

    public void deletePin(Long vehicleId, Long componentId, Long pinId, User user) {
        ElectricalPin pin = getOwnedPinOrThrow(vehicleId, componentId, pinId, user);
        if (!readingRepository.findByPinId(pinId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete a pin that already has readings recorded against it");
        }
        pinRepository.delete(pin);
    }

    public void movePin(Long vehicleId, Long componentId, Long pinId, String direction, User user) {
        getOwnedComponentOrThrow(vehicleId, componentId, user);
        List<ElectricalPin> pins = pinRepository.findByComponentIdOrderByPositionAsc(componentId);

        int index = -1;
        for (int i = 0; i < pins.size(); i++) {
            if (pins.get(i).getId().equals(pinId)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pin not found");
        }

        int targetIndex = "up".equals(direction) ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= pins.size()) {
            return;
        }

        ElectricalPin current = pins.get(index);
        ElectricalPin target = pins.get(targetIndex);
        int tempPosition = current.getPosition();
        current.setPosition(target.getPosition());
        target.setPosition(tempPosition);

        pinRepository.save(current);
        pinRepository.save(target);
    }

    public ElectricalSession createSession(Long vehicleId, Long componentId, String label,
                                           LocalDateTime sessionDate, String notes, User user) {
        ElectricalComponent component = getOwnedComponentOrThrow(vehicleId, componentId, user);
        LocalDateTime resolvedDate = sessionDate != null ? sessionDate : LocalDateTime.now();
        return sessionRepository.save(new ElectricalSession(label, resolvedDate, notes, component));
    }

    public Optional<ElectricalReading> upsertReading(Long vehicleId, Long componentId, Long sessionId,
                                                     Long pinId, ElectricalReadingUpsertDTO dto, User user) {
        ElectricalSession session = getOwnedSessionOrThrow(vehicleId, componentId, sessionId, user);
        ElectricalPin pin = getOwnedPinOrThrow(vehicleId, componentId, pinId, user);

        Optional<ElectricalReading> existing = readingRepository.findBySessionIdAndPinId(sessionId, pinId);
        boolean isBlank = (dto.value() == null || dto.value().isBlank());

        if (isBlank) {
            existing.ifPresent(readingRepository::delete);
            return Optional.empty();
        }

        ElectricalReading reading = existing.orElse(new ElectricalReading(null, null, session, pin));
        reading.setValue(dto.value());
        reading.setUnit(dto.unit());
        return Optional.of(readingRepository.save(reading));
    }

    public List<ElectricalReading> getReadingsForSession(Long sessionId) {
        return readingRepository.findBySessionId(sessionId);
    }

    public List<ElectricalPin> getPinsForComponent(Long componentId) {
        return pinRepository.findByComponentIdOrderByPositionAsc(componentId);
    }

    public List<ElectricalSession> getSessionsForComponent(Long componentId) {
        return sessionRepository.findByComponentId(componentId);
    }

    public List<ElectricalComponent> getComponentsForVehicle(Long vehicleId, User user) {
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        return componentRepository.findByVehicleId(vehicleId);
    }

    public ElectricalComponent createComponent(Long vehicleId, String name, String description, User user) {
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        return componentRepository.save(new ElectricalComponent(name, description, vehicle));
    }

    public ElectricalComponent updateComponent(Long vehicleId, Long componentId, String name,
                                               String description, User user) {
        ElectricalComponent component = getOwnedComponentOrThrow(vehicleId, componentId, user);
        component.setName(name);
        component.setDescription(description);
        return componentRepository.save(component);
    }

    public void deleteComponent(Long vehicleId, Long componentId, User user) {
        ElectricalComponent component = getOwnedComponentOrThrow(vehicleId, componentId, user);
        componentRepository.delete(component);
    }

    public ElectricalPin updatePin(Long vehicleId, Long componentId, Long pinId,
                                   String name, String expectedRange, User user) {
        ElectricalPin pin = getOwnedPinOrThrow(vehicleId, componentId, pinId, user);
        pin.setName(name);
        pin.setExpectedRange(expectedRange);
        return pinRepository.save(pin);
    }

    public ElectricalSession updateSession(Long vehicleId, Long componentId, Long sessionId,
                                           String label, LocalDateTime sessionDate, String notes, User user) {
        ElectricalSession session = getOwnedSessionOrThrow(vehicleId, componentId, sessionId, user);
        session.setLabel(label);
        if (sessionDate != null) session.setSessionDate(sessionDate);
        session.setNotes(notes);
        return sessionRepository.save(session);
    }

    public void deleteSession(Long vehicleId, Long componentId, Long sessionId, User user) {
        ElectricalSession session = getOwnedSessionOrThrow(vehicleId, componentId, sessionId, user);
        sessionRepository.delete(session);
    }
}