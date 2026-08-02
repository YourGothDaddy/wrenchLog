package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.VehicleCatalog;
import com.wrenchlog.wrenchlog.repository.VehicleCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleCatalogControllerTest {

    private VehicleCatalogRepository catalogRepository;
    private VehicleCatalogController vehicleCatalogController;

    @BeforeEach
    void setUp() {
        catalogRepository = mock(VehicleCatalogRepository.class);
        vehicleCatalogController = new VehicleCatalogController(catalogRepository);
    }

    @Test
    void getMakes_returnsDistinctMakes() {
        when(catalogRepository.findDistinctMakes()).thenReturn(List.of("Toyota", "Honda"));

        List<String> response = vehicleCatalogController.getMakes();

        assertEquals(2, response.size());
        assertTrue(response.contains("Toyota"));
        assertTrue(response.contains("Honda"));
    }

    @Test
    void getModels_returnsModelsForMake() {
        when(catalogRepository.findDistinctModelsByMake("Toyota")).thenReturn(List.of("Corolla", "Camry"));

        List<String> response = vehicleCatalogController.getModels("Toyota");

        assertEquals(2, response.size());
        verify(catalogRepository).findDistinctModelsByMake("Toyota");
    }

    @Test
    void getGenerations_returnsGenerationsForMakeAndModel() {
        when(catalogRepository.findDistinctGenerationsByMakeAndModel("Toyota", "Corolla"))
                .thenReturn(List.of("E210"));

        List<String> response = vehicleCatalogController.getGenerations("Toyota", "Corolla");

        assertEquals(1, response.size());
        assertEquals("E210", response.get(0));
    }

    @Test
    void getModifications_returnsModificationsForFullSpec() {
        VehicleCatalog modification = new VehicleCatalog();
        ReflectionTestUtils.setField(modification, "id", 1L);
        ReflectionTestUtils.setField(modification, "make", "Toyota");
        ReflectionTestUtils.setField(modification, "model", "Corolla");
        ReflectionTestUtils.setField(modification, "generation", "E210");
        ReflectionTestUtils.setField(modification, "modification", "1.8 Hybrid");

        when(catalogRepository.findModifications("Toyota", "Corolla", "E210"))
                .thenReturn(List.of(modification));

        List<VehicleCatalog> response = vehicleCatalogController.getModifications("Toyota", "Corolla", "E210");

        assertEquals(1, response.size());
        assertEquals("1.8 Hybrid", response.get(0).getModification());
    }
}