CREATE TABLE vehicle_folders (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    vehicle_id BIGINT NOT NULL REFERENCES vehicles(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vehicle_folders_vehicle_id ON vehicle_folders(vehicle_id);

ALTER TABLE vehicle_files ADD COLUMN folder_id BIGINT REFERENCES vehicle_folders(id) ON DELETE SET NULL;