package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "service_logs")
public class ServiceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;
    private double cost;
    private int kilometersAtService;
    private LocalDate serviceDate;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnoreProperties({"serviceLogs", "hibernateLazyInitializer", "handler"})
    private Vehicle vehicle;

    public ServiceLog() {
    }

    public ServiceLog(Long id, String description, double cost, int kilometersAtService, LocalDate serviceDate, Vehicle vehicle) {
        this.id = id;
        this.description = description;
        this.cost = cost;
        this.kilometersAtService = kilometersAtService;
        this.serviceDate = serviceDate;
        this.vehicle = vehicle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public int getKilometersAtService() { return kilometersAtService; }
    public void setKilometersAtService(int kilometersAtService) { this.kilometersAtService = kilometersAtService; }

    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }

    public Vehicle getVehicle() { return vehicle; }
    public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }


}
