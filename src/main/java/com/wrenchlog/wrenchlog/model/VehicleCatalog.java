package com.wrenchlog.wrenchlog.model;

import jakarta.persistence.*;

@Entity
@Table(name = "car_specifications")
public class VehicleCatalog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "generation")
    private String generation;

    @Column(name = "modification")
    private String modification;

    @Column(name = "start_year")
    private int startYear;

    @Column(name = "end_year")
    private Integer endYear;

    public VehicleCatalog(){}

    public Long getId() { return id; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public String getGeneration() { return generation; }
    public String getModification() { return modification; }
    public int getStartYear() { return startYear; }
    public Integer getEndYear() { return endYear; }
}
