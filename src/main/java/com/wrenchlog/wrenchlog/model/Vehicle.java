package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wrenchlog.wrenchlog.enums.DriveType;
import com.wrenchlog.wrenchlog.enums.FuelType;
import com.wrenchlog.wrenchlog.enums.TransmissionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "vehicles",
        indexes = {
                @Index(
                        name = "idx_vehicle_user_id",
                        columnList = "user_id"
                )
        }
)
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 50)
    private String make;


    @Column(nullable = false, length = 50)
    private String model;


    @Column(name = "production_year")
    private Integer year;


    @Column(nullable = false)
    private int kilometers;

    @Column(length = 32)
    private String vin;

    @Column(name = "plate_number", length = 20)
    private String plateNumber;

    @Column(name = "engine_code", length = 30)
    private String engineCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "transmission_type")
    private TransmissionType transmissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "drive_type")
    private DriveType driveType;

    @Column(length = 50)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type")
    private FuelType fuelType;

    @Column(name = "fuel_tank_capacity_liters", precision = 5, scale = 1)
    private BigDecimal fuelTankCapacityLiters;

    @Column(name = "engine_oil_capacity_liters", precision = 4, scale = 2)
    private BigDecimal engineOilCapacityLiters;

    @Column(name = "engine_oil_type", length = 20)
    private String engineOilType;

    @Column(name = "tire_size", length = 20)
    private String tireSize;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @OneToMany(
            mappedBy = "vehicle",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("vehicle")
    private List<ServiceLog> serviceLogs = new ArrayList<>();


    @OneToMany(
            mappedBy = "vehicle",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("vehicle")
    private List<VehicleFile> files = new ArrayList<>();


    @OneToMany(
            mappedBy = "vehicle",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("vehicle")
    private List<ServiceReminder> reminders = new ArrayList<>();


    @OneToMany(
            mappedBy = "vehicle",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @JsonIgnoreProperties("vehicle")
    private List<VehicleNote> notes = new ArrayList<>();


    public Vehicle() {
    }


    public Vehicle(
            String make,
            String model,
            Integer year,
            int kilometers,
            User user
    ) {
        this.make = make;
        this.model = model;
        this.year = year;
        this.kilometers = kilometers;
        this.user = user;
    }


    public void addServiceLog(ServiceLog serviceLog) {
        serviceLogs.add(serviceLog);
        serviceLog.setVehicle(this);
    }


    public void removeServiceLog(ServiceLog serviceLog) {
        serviceLogs.remove(serviceLog);
        serviceLog.setVehicle(null);
    }


    public void addFile(VehicleFile file) {
        files.add(file);
        file.setVehicle(this);
    }


    public void removeFile(VehicleFile file) {
        files.remove(file);
        file.setVehicle(null);
    }


    public void addReminder(ServiceReminder reminder) {
        reminders.add(reminder);
        reminder.setVehicle(this);
    }


    public void removeReminder(ServiceReminder reminder) {
        reminders.remove(reminder);
        reminder.setVehicle(null);
    }


    public void addNote(VehicleNote note) {
        notes.add(note);
        note.setVehicle(this);
    }


    public void removeNote(VehicleNote note) {
        notes.remove(note);
        note.setVehicle(null);
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }


    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }


    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }


    public int getKilometers() {
        return kilometers;
    }

    public void setKilometers(int kilometers) {
        this.kilometers = kilometers;
    }


    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public List<ServiceLog> getServiceLogs() {
        return serviceLogs;
    }

    public void setServiceLogs(List<ServiceLog> serviceLogs) {
        this.serviceLogs = serviceLogs;
    }


    public List<VehicleFile> getFiles() {
        return files;
    }

    public void setFiles(List<VehicleFile> files) {
        this.files = files;
    }


    public List<ServiceReminder> getReminders() {
        return reminders;
    }

    public void setReminders(List<ServiceReminder> reminders) {
        this.reminders = reminders;
    }


    public List<VehicleNote> getNotes() {
        return notes;
    }

    public void setNotes(List<VehicleNote> notes) {
        this.notes = notes;
    }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getEngineCode() { return engineCode; }
    public void setEngineCode(String engineCode) { this.engineCode = engineCode; }

    public TransmissionType getTransmissionType() { return transmissionType; }
    public void setTransmissionType(TransmissionType transmissionType) { this.transmissionType = transmissionType; }

    public DriveType getDriveType() { return driveType; }
    public void setDriveType(DriveType driveType) { this.driveType = driveType; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public BigDecimal getFuelTankCapacityLiters() { return fuelTankCapacityLiters; }
    public void setFuelTankCapacityLiters(BigDecimal fuelTankCapacityLiters) { this.fuelTankCapacityLiters = fuelTankCapacityLiters; }

    public BigDecimal getEngineOilCapacityLiters() { return engineOilCapacityLiters; }
    public void setEngineOilCapacityLiters(BigDecimal engineOilCapacityLiters) { this.engineOilCapacityLiters = engineOilCapacityLiters; }

    public String getEngineOilType() { return engineOilType; }
    public void setEngineOilType(String engineOilType) { this.engineOilType = engineOilType; }

    public String getTireSize() { return tireSize; }
    public void setTireSize(String tireSize) { this.tireSize = tireSize; }

    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
}