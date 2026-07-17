package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_reminders",
        indexes = {
                @Index(
                        name = "idx_service_reminders_vehicle_id",
                        columnList = "vehicle_id"
                )
        }
)
public class ServiceReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 100)
    private String title;


    @Column(columnDefinition = "TEXT")
    private String description;


    private Integer lastServiceAtOdometer;


    private Integer intervalOdometer;


    private Integer intervalMonths;


    private LocalDate lastServiceAtDate;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false
    )
    @JsonIgnore
    private Vehicle vehicle;


    public ServiceReminder() {
    }


    public ServiceReminder(
            String title,
            String description,
            Integer lastServiceAtOdometer,
            Integer intervalOdometer,
            Integer intervalMonths,
            LocalDate lastServiceAtDate,
            Vehicle vehicle
    ) {
        this.title = title;
        this.description = description;
        this.lastServiceAtOdometer = lastServiceAtOdometer;
        this.intervalOdometer = intervalOdometer;
        this.intervalMonths = intervalMonths;
        this.lastServiceAtDate = lastServiceAtDate;
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

    public void setLastServiceAtOdometer(Integer value) {
        this.lastServiceAtOdometer = value;
    }


    public Integer getIntervalOdometer() {
        return intervalOdometer;
    }

    public void setIntervalOdometer(Integer intervalOdometer) {
        this.intervalOdometer = intervalOdometer;
    }


    public Integer getIntervalMonths() {
        return intervalMonths;
    }

    public void setIntervalMonths(Integer intervalMonths) {
        this.intervalMonths = intervalMonths;
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