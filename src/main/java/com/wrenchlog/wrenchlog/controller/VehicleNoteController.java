package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.User;
import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.model.VehicleNote;
import com.wrenchlog.wrenchlog.repository.VehicleNoteRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import com.wrenchlog.wrenchlog.service.VehicleAccessService;
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
                                 VehicleRepository vehicleRepository,
                                 VehicleAccessService vehicleAccessService){
        this.vehicleNoteRepository = vehicleNoteRepository;
        this.vehicleAccessService = vehicleAccessService;
    }

    @GetMapping
    public ResponseEntity<List<VehicleNote>> getVehicleNotes(
            @PathVariable Long vehicleId,
            @AuthenticationPrincipal User user
    ){
        vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        List<VehicleNote> notes = vehicleNoteRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<?> createNote(
            @PathVariable Long vehicleId,
            @RequestBody VehicleNote incomingNote,
            @AuthenticationPrincipal User user
    ){
        Vehicle vehicle = vehicleAccessService.getOwnedVehicleOrThrow(vehicleId, user);
        VehicleNote note = new VehicleNote(incomingNote.getTitle(), incomingNote.getContent(), vehicle);
        VehicleNote savedNote = vehicleNoteRepository.save(note);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<?> updateNote(
            @PathVariable Long vehicleId,
            @PathVariable Long noteId,
            @RequestBody VehicleNote updatedDetails,
            @AuthenticationPrincipal User user
    ){
        VehicleNote existingNote = vehicleNoteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!existingNote.getVehicle().getId().equals(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        vehicleAccessService.assertOwnership(existingNote.getVehicle(), user);

        existingNote.setTitle(updatedDetails.getTitle());
        existingNote.setContent(updatedDetails.getContent());
        VehicleNote saved = vehicleNoteRepository.save(existingNote);
        return ResponseEntity.ok(saved);
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
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
