package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_reminders")
public class ServiceReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer lastServiceAtOdometer;

    private Integer intervalOdometer;

    private Integer intervalMonths;

    private LocalDate lastServiceAtDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore
    private Vehicle vehicle;

    public ServiceReminder() {
    }

    public ServiceReminder(String title,
                           String description,
                           Integer lastServiceAtOdometer,
                           Integer intervalOdometer,
                           Integer intervalMonths,
                           LocalDate lastServiceAtDate,
                           LocalDateTime createdAt,
                           Vehicle vehicle) {
        this.title = title;
        this.description = description;
        this.lastServiceAtOdometer = lastServiceAtOdometer;
        this.intervalOdometer = intervalOdometer;
        this.intervalMonths = intervalMonths;
        this.lastServiceAtDate = lastServiceAtDate;
        this.createdAt = createdAt;
        this.vehicle = vehicle;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLastServiceAtOdometer() {
        return lastServiceAtOdometer;
    }

    public void setLastServiceAtOdometer(Integer lastServiceAtOdometer) {
        this.lastServiceAtOdometer = lastServiceAtOdometer;
    }

    public Integer getIntervalMonths() {
        return intervalMonths;
    }

    public void setIntervalMonths(Integer intervalMonths) {
        this.intervalMonths = intervalMonths;
    }

    public Integer getIntervalOdometer() {
        return intervalOdometer;
    }

    public void setIntervalOdometer(Integer intervalOdometer) {
        this.intervalOdometer = intervalOdometer;
    }

    public LocalDate getLastServiceAtDate() {
        return lastServiceAtDate;
    }

    public void setLastServiceAtDate(LocalDate lastServiceAtDate) {
        this.lastServiceAtDate = lastServiceAtDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}
