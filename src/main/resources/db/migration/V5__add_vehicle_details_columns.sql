ALTER TABLE vehicles
    ADD COLUMN vin VARCHAR(32),
    ADD COLUMN plate_number VARCHAR(20),
    ADD COLUMN engine_code VARCHAR(30),
    ADD COLUMN transmission_type VARCHAR(20),
    ADD COLUMN drive_type VARCHAR(10),
    ADD COLUMN color VARCHAR(50),
    ADD COLUMN fuel_type VARCHAR(20),
    ADD COLUMN fuel_tank_capacity_liters NUMERIC(5,1),
    ADD COLUMN engine_oil_capacity_liters NUMERIC(4,2),
    ADD COLUMN engine_oil_type VARCHAR(20),
    ADD COLUMN tire_size VARCHAR(20),
    ADD COLUMN purchase_date DATE,
    ADD COLUMN purchase_price NUMERIC(10,2),

    ADD CONSTRAINT chk_fuel_tank_capacity_non_negative
        CHECK (fuel_tank_capacity_liters IS NULL OR fuel_tank_capacity_liters >= 0),

    ADD CONSTRAINT chk_engine_oil_capacity_non_negative
        CHECK (engine_oil_capacity_liters IS NULL OR engine_oil_capacity_liters >= 0),

    ADD CONSTRAINT chk_purchase_price_non_negative
        CHECK (purchase_price IS NULL OR purchase_price >= 0);