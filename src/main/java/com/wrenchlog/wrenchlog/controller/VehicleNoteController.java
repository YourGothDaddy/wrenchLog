package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.VehicleNote;
import com.wrenchlog.wrenchlog.repository.VehicleNoteRepository;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/notes")
public class VehicleNoteController {
    private final VehicleNoteRepository vehicleNoteRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleNoteController(VehicleNoteRepository vehicleNoteRepository,
                                 VehicleRepository vehicleRepository){
        this.vehicleNoteRepository = vehicleNoteRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @GetMapping
    public ResponseEntity<List<VehicleNote>> getVehicleNotes(@PathVariable Long vehicleId){
        List<VehicleNote> notes = vehicleNoteRepository.findByVehicleIdOrderByCreatedAtDesc(vehicleId);
        return ResponseEntity.ok(notes);
    }

    @PostMapping
    public ResponseEntity<?> createNote(@PathVariable Long vehicleId,
                                        @RequestBody VehicleNote incomingNote){
        return vehicleRepository.findById(vehicleId)
                .map(vehicle -> {
                    VehicleNote note = new VehicleNote(incomingNote.getTitle(), incomingNote.getContent(), vehicle);
                    VehicleNote savedNote = vehicleNoteRepository.save(note);
                    return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PutMapping("/{noteId}")
    public ResponseEntity<?> updateNote(@PathVariable Long noteId,
                                        @RequestBody VehicleNote updatedDetails){
        return vehicleNoteRepository.findById(noteId)
                .map(existingNote -> {
                    existingNote.setTitle(updatedDetails.getTitle());
                    existingNote.setContent(updatedDetails.getContent());
                    VehicleNote saved = vehicleNoteRepository.save(existingNote);
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId) {
        if (vehicleNoteRepository.existsById(noteId)) {
            vehicleNoteRepository.deleteById(noteId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
