package com.wrenchlog.wrenchlog;

import com.wrenchlog.wrenchlog.model.Vehicle;
import com.wrenchlog.wrenchlog.repository.VehicleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final VehicleRepository vehicleRepository;

    public DataInitializer(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Override
    public void run(String... args) throws Exception{
        if(vehicleRepository.count() == 0){
            System.out.println("Seeding default test vehicle.");

            Vehicle defaultCar = new Vehicle();
            defaultCar.setMake("Mercedes-Benz");
            defaultCar.setModel("W211 E270");
            defaultCar.setYear(2003);
            defaultCar.setKilometers(350000);
            defaultCar.setUserId("alexander");

            vehicleRepository.save(defaultCar);

            System.out.println("Successfully seeded Mercedes-Benz W211 E270");
        }
    }
}
