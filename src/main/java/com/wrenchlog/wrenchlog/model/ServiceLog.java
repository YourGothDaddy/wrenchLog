package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "service_logs",
        indexes = {
                @Index(
                        name = "idx_service_logs_vehicle_id",
                        columnList = "vehicle_id"
                )
        }
)
public class ServiceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 500)
    private String description;


    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal cost;


    @Column(nullable = false)
    private int kilometersAtService;


    private LocalDate serviceDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false
    )
    @JsonIgnoreProperties({
            "serviceLogs",
            "files",
            "reminders",
            "notes"
    })
    private Vehicle vehicle;


    public ServiceLog() {
    }


    public ServiceLog(
            String description,
            BigDecimal cost,
            int kilometersAtService,
            LocalDate serviceDate,
            Vehicle vehicle
    ) {
        this.description = description;
        this.cost = cost;
        this.kilometersAtService = kilometersAtService;
        this.serviceDate = serviceDate;
        this.vehicle = vehicle;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }


    public int getKilometersAtService() {
        return kilometersAtService;
    }

    public void setKilometersAtService(int kilometersAtService) {
        this.kilometersAtService = kilometersAtService;
    }


    public LocalDate getServiceDate() {
        return serviceDate;
    }

    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }


    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }
}