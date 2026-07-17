package com.wrenchlog.wrenchlog.model;

import jakarta.persistence.*;

@Entity
@Table(
        name = "car_specifications",
        indexes = {
                @Index(
                        name = "idx_car_specifications_make_model",
                        columnList = "make, model"
                )
        }
)
public class VehicleCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(length = 50)
    private String make;


    @Column(length = 100)
    private String model;


    @Column(length = 100)
    private String generation;


    @Column(length = 150)
    private String modification;


    @Column(name = "start_year")
    private Integer startYear;


    @Column(name = "end_year")
    private Integer endYear;


    public VehicleCatalog() {
    }


    public Long getId() {
        return id;
    }


    public String getMake() {
        return make;
    }


    public String getModel() {
        return model;
    }


    public String getGeneration() {
        return generation;
    }


    public String getModification() {
        return modification;
    }


    public Integer getStartYear() {
        return startYear;
    }


    public Integer getEndYear() {
        return endYear;
    }
}