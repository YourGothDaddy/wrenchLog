package com.wrenchlog.wrenchlog.repository;

import com.wrenchlog.wrenchlog.model.VehicleCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleCatalogRepository extends JpaRepository<VehicleCatalog, Long> {

    @Query("SELECT DISTINCT c.make FROM VehicleCatalog c ORDER BY c.make ASC")
    List<String> findDistinctMakes();

    @Query("SELECT DISTINCT c.model FROM VehicleCatalog c WHERE c.make = :make ORDER BY c.model ASC")
    List<String> findDistinctModelsByMake(@Param("make") String make);

    @Query("SELECT DISTINCT c.generation FROM VehicleCatalog c WHERE c.make = :make AND c.model = :model ORDER BY c.generation ASC")
    List<String> findDistinctGenerationsByMakeAndModel(@Param("make") String make, @Param("model") String model);

    @Query("SELECT c FROM VehicleCatalog c WHERE c.make = :make AND c.model = :model AND c.generation = :generation ORDER BY c.modification ASC")
    List<VehicleCatalog> findModifications(@Param("make") String make, @Param("model") String model, @Param("generation") String generation);
}

