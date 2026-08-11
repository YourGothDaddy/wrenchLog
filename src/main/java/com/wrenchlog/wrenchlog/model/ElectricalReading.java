package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "electrical_readings")
public class ElectricalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String value;

    @Column(length = 20)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @JsonIgnore
    private ElectricalSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    @JsonIgnore
    private ElectricalPin pin;

    public ElectricalReading() {}

    public ElectricalReading(String value, String unit, ElectricalSession session, ElectricalPin pin) {
        this.value = value;
        this.unit = unit;
        this.session = session;
        this.pin = pin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public ElectricalSession getSession() { return session; }
    public void setSession(ElectricalSession session) { this.session = session; }
    public ElectricalPin getPin() { return pin; }
    public void setPin(ElectricalPin pin) { this.pin = pin; }
}