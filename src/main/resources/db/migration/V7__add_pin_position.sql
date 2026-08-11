ALTER TABLE electrical_pins
    ADD COLUMN position INTEGER NOT NULL DEFAULT 0;

UPDATE electrical_pins ep
SET position = sub.rn
    FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY component_id ORDER BY id) AS rn
    FROM electrical_pins
) sub
WHERE ep.id = sub.id;