UPDATE car_specifications
SET end_year = NULL
WHERE end_year < start_year;

ALTER TABLE car_specifications
    ADD CONSTRAINT uk_car_specification
        UNIQUE (
                make,
                model,
                generation,
                modification,
                start_year,
                end_year
            );


ALTER TABLE car_specifications
    ADD CONSTRAINT chk_car_year_range
        CHECK (
            end_year IS NULL
                OR start_year IS NULL
                OR end_year >= start_year
            );