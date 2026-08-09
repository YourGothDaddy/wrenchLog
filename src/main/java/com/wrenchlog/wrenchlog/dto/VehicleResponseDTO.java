package com.wrenchlog.wrenchlog.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VehicleResponseDTO {
    private Long id;
    private String make;
    private String model;
    private Integer year;
    private int kilometers;
    private String username;
    private String vin;
    private String plateNumber;
    private String engineCode;
    private String transmissionType;
    private String driveType;
    private String color;
    private String fuelType;
    private BigDecimal fuelTankCapacityLiters;
    private BigDecimal engineOilCapacityLiters;
    private String engineOilType;
    private String tireSize;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;

    public VehicleResponseDTO(Long id, String make, String model, Integer year, int kilometers, String username,
                              String vin, String plateNumber, String engineCode, String transmissionType,
                              String driveType, String color, String fuelType, BigDecimal fuelTankCapacityLiters,
                              BigDecimal engineOilCapacityLiters, String engineOilType, String tireSize,
                              LocalDate purchaseDate, BigDecimal purchasePrice) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.kilometers = kilometers;
        this.username = username;
        this.vin = vin;
        this.plateNumber = plateNumber;
        this.engineCode = engineCode;
        this.transmissionType = transmissionType;
        this.driveType = driveType;
        this.color = color;
        this.fuelType = fuelType;
        this.fuelTankCapacityLiters = fuelTankCapacityLiters;
        this.engineOilCapacityLiters = engineOilCapacityLiters;
        this.engineOilType = engineOilType;
        this.tireSize = tireSize;
        this.purchaseDate = purchaseDate;
        this.purchasePrice = purchasePrice;
    }

    public Long getId() { return id; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public int getKilometers() { return kilometers; }
    public String getUsername() { return username; }
    public String getVin() { return vin; }
    public String getPlateNumber() { return plateNumber; }
    public String getEngineCode() { return engineCode; }
    public String getTransmissionType() { return transmissionType; }
    public String getDriveType() { return driveType; }
    public String getColor() { return color; }
    public String getFuelType() { return fuelType; }
    public BigDecimal getFuelTankCapacityLiters() { return fuelTankCapacityLiters; }
    public BigDecimal getEngineOilCapacityLiters() { return engineOilCapacityLiters; }
    public String getEngineOilType() { return engineOilType; }
    public String getTireSize() { return tireSize; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
}