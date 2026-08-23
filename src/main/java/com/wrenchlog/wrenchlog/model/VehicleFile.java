package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vehicle_files",
        indexes = {
                @Index(
                        name = "idx_vehicle_files_vehicle_id",
                        columnList = "vehicle_id"
                ),
                @Index(
                        name = "idx_vehicle_files_folder_id",
                        columnList = "folder_id"
                )
        }
)
public class VehicleFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(nullable = false, length = 255)
    private String name;


    @Column(nullable = false, length = 100)
    private String type;


    @Column(nullable = false, length = 500)
    private String path;


    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "vehicle_id",
            nullable = false
    )
    @JsonIgnoreProperties({
            "files",
            "folders",
            "serviceLogs",
            "reminders",
            "notes",
            "user"
    })
    private Vehicle vehicle;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "folder_id",
            nullable = true
    )
    @JsonIgnoreProperties({
            "vehicle"
    })
    private VehicleFolder folder;


    public VehicleFile() {
    }


    public VehicleFile(
            String name,
            String type,
            String path,
            Vehicle vehicle
    ) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.vehicle = vehicle;
    }


    public VehicleFile(
            String name,
            String type,
            String path,
            Vehicle vehicle,
            VehicleFolder folder
    ) {
        this.name = name;
        this.type = type;
        this.path = path;
        this.vehicle = vehicle;
        this.folder = folder;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getFileName() {
        return name;
    }

    public void setFileName(String name) {
        this.name = name;
    }


    public String getFileType() {
        return type;
    }

    public void setFileType(String type) {
        this.type = type;
    }


    public String getFilePath() {
        return path;
    }

    public void setFilePath(String path) {
        this.path = path;
    }


    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }


    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }


    public VehicleFolder getFolder() {
        return folder;
    }

    public void setFolder(VehicleFolder folder) {
        this.folder = folder;
    }
}