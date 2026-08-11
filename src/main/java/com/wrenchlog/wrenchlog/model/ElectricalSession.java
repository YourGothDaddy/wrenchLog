package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "electrical_sessions")
public class ElectricalSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "session_date", nullable = false)
    private LocalDateTime sessionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    @JsonIgnore
    private ElectricalComponent component;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ElectricalReading> readings = new ArrayList<>();

    public ElectricalSession() {}

    public ElectricalSession(String label, LocalDateTime sessionDate, String notes, ElectricalComponent component) {
        this.label = label;
        this.sessionDate = sessionDate;
        this.notes = notes;
        this.component = component;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public LocalDateTime getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDateTime sessionDate) { this.sessionDate = sessionDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public ElectricalComponent getComponent() { return component; }
    public void setComponent(ElectricalComponent component) { this.component = component; }
    public List<ElectricalReading> getReadings() { return readings; }
}