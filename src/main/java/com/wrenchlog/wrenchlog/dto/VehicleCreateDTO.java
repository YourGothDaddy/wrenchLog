package com.wrenchlog.wrenchlog.dto;

public class VehicleCreateDTO {
    private String make;
    private String model;
    private Integer year;
    private int kilometers;
    private String username;

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public int getKilometers() { return kilometers; }
    public void setKilometers(int kilometers) { this.kilometers = kilometers; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}