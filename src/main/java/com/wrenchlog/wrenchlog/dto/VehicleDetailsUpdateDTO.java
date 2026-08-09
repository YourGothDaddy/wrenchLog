package com.wrenchlog.wrenchlog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleDetailsUpdateDTO(
        @Size(max = 32, message = "VIN is too long") String vin,
        @Size(max = 20, message = "Plate number is too long") String plateNumber,
        @Size(max = 30, message = "Engine code is too long") String engineCode,
        String transmissionType,
        String driveType,
        @Size(max = 50, message = "Color is too long") String color,
        String fuelType,
        @DecimalMin(value = "0.0", message = "Fuel tank capacity cannot be negative") BigDecimal fuelTankCapacityLiters,
        @DecimalMin(value = "0.0", message = "Engine oil capacity cannot be negative") BigDecimal engineOilCapacityLiters,
        @Size(max = 20, message = "Engine oil type is too long") String engineOilType,
        @Size(max = 20, message = "Tire size is too long") String tireSize,
        LocalDate purchaseDate,
        @DecimalMin(value = "0.0", message = "Purchase price cannot be negative") BigDecimal purchasePrice,
        LocalDate insuranceExpiryDate,
        LocalDate vignetteExpiryDate,
        LocalDate inspectionDueDate
) {}