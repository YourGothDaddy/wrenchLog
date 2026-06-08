package com.wrenchlog.wrenchlog.controller;

import com.wrenchlog.wrenchlog.model.VehicleCatalog;
import com.wrenchlog.wrenchlog.repository.VehicleCatalogRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class VehicleCatalogController {
    private final VehicleCatalogRepository catalogRepository;

    public VehicleCatalogController(VehicleCatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @GetMapping("/makes")
    public List<String> getMakes() {
        return catalogRepository.findDistinctMakes();
    }

    @GetMapping("/models")
    public List<String> getModels(@RequestParam("make") String make) {
        return catalogRepository.findDistinctModelsByMake(make);
    }

    @GetMapping("/generations")
    public List<String> getGenerations(@RequestParam("make") String make, @RequestParam("model") String model) {
        return catalogRepository.findDistinctGenerationsByMakeAndModel(make, model);
    }

    @GetMapping("/modifications")
    public List<VehicleCatalog> getModifications(@RequestParam("make") String make,
                                                 @RequestParam("model") String model,
                                                 @RequestParam("generation") String generation) {
        return catalogRepository.findModifications(make, model, generation);
    }


}
