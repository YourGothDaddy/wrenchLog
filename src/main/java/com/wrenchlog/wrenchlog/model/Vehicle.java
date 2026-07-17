package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

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
}