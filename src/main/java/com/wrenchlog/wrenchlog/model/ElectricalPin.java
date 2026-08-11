package com.wrenchlog.wrenchlog.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "electrical_pins")
public class ElectricalPin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "expected_range", length = 100)
    private String expectedRange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    @JsonIgnore
    private ElectricalComponent component;

    @Column(nullable = false)
    private int position;

    public ElectricalPin() {}

    public ElectricalPin(String name, String expectedRange, ElectricalComponent component) {
        this.name = name;
        this.expectedRange = expectedRange;
        this.component = component;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getExpectedRange() { return expectedRange; }
    public void setExpectedRange(String expectedRange) { this.expectedRange = expectedRange; }
    public ElectricalComponent getComponent() { return component; }
    public void setComponent(ElectricalComponent component) { this.component = component; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}