package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.dto.VehicleNoteCreateDTO;
import com.wrenchlog.wrenchlog.dto.VehicleNoteResponseDTO;
import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleNote;
import com.wrenchlog.wrenchlog.repository.VehicleNoteRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/notes")
public class VehicleNoteController {
    private final VehicleNoteRepository vehicleNoteRepository;
    private final VehicleAccessService vehicleAccessService;

    public VehicleNoteController(VehicleNoteRepository vehicleNoteRepository,
                                 VehicleAccessService vehicleAccessService){
        this.vehicleNoteRepository = vehicleNoteRepository;
        this.vehicleAccessService = vehicleAccessService;
    }

    private VehicleNoteResponseDTO toResponseDTO(VehicleNote note) {
        return new VehicleNoteResponseDTO(
                note.getId(), note.getTitle(), note.getContent(), note.getCreatedAt(), note.getVehicle().getId()
        );
    }

    @GetMapping
    public ResponseEntity<List<VehicleNoteResponseDTO>> getVehicleNotes(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal User user
    ){
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        List<VehicleNoteResponseDTO> notes = vehicleNoteRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId)
                .stream().map(this::toResponseDTO).toList();
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<VehicleNoteResponseDTO> createNote(
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleNoteCreateDTO dto,
            @AuthenticationPrincipal User user
    ){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleNote note = new VehicleNote(dto.title(), dto.content(), vehicle);
        VehicleNote savedNote = vehicleNoteRepository.save(note);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDTO(savedNote));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<VehicleNoteResponseDTO> updateNote(
            @PathVariable Long vehicleId,
            @PathVariable Long noteId,
            @Valid @RequestBody VehicleNoteCreateDTO dto,
            @AuthenticationPrincipal User user
    ){
        VehicleNote existingNote = vehicleNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!existingNote.getVehicle().getId().equals(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        vehicleAccessService.assertOwnership(existingNote.getVehicle(), user);

        existingNote.setTitle(dto.title());
        existingNote.setContent(dto.content());
        VehicleNote saved = vehicleNoteRepository.save(existingNote);
        return ResponseEntity.ok(toResponseDTO(saved));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long vehicleId,
            @PathVariable Long noteId,
            @AuthenticationPrincipal User user
    ) {
        VehicleNote note = vehicleNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!note.getVehicle().getId().equals(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        vehicleAccessService.assertOwnership(note.getVehicle(), user);

        vehicleNoteRepository.deleteById(noteId);
        return ResponseEntity.noContent().build();
    }
}