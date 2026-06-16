package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String make;

    @Column(nullable = false)
    private String model;

    @Column(name = "production_year")
    private Integer year;

    private int kilometers;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("vehicle")
    private List<ServiceLog> serviceLogs;

    public Vehicle(){

    }

    public Vehicle(String make, String model, Integer year, int kilometers, String userId){
        this.make = make;
        this.model = model;
        this.year = year;
        this.kilometers = kilometers;
        this.userId  = userId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public int getKilometers() { return kilometers; }
    public void setKilometers(int kilometers) { this.kilometers = kilometers; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<ServiceLog> getServiceLogs() { return serviceLogs; }
    public void setServiceLogs(List<ServiceLog> serviceLogs) { this.serviceLogs = serviceLogs; }
}
