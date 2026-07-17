package com.wrenchlog.wrenchlog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ServiceLogResponseDTO {
    private Long id;
    private String description;
    private BigDecimal cost;
    private int kilometersAtService;
    private LocalDate serviceDate;
    private Long vehicleId;

    public ServiceLogResponseDTO(Long id, String description, BigDecimal cost, int kilometersAtService, LocalDate serviceDate, Long vehicleId) {
        this.id = id;
        this.description = description;
        this.cost = cost;
        this.kilometersAtService = kilometersAtService;
        this.serviceDate = serviceDate;
        this.vehicleId = vehicleId;
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getCost() { return cost; }
    public int getKilometersAtService() { return kilometersAtService; }
    public LocalDate getServiceDate() { return serviceDate; }
    public Long getVehicleId() { return vehicleId; }
}