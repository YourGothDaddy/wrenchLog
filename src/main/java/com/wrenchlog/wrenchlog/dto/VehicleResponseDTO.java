package com.wrenchlog.wrenchlog.dto;

public class VehicleResponseDTO {
    private Long id;
    private String make;
    private String model;
    private Integer year;
    private int kilometers;
    private String username;

    public VehicleResponseDTO(Long id, String make, String model, Integer year, int kilometers, String username) {
        this.id = id;
        this.make = make;
        this.model = model;
        this.year = year;
        this.kilometers = kilometers;
        this.username = username;
    }

    public Long getId() { return id; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public int getKilometers() { return kilometers; }
    public String getUsername() { return username; }
}