ALTER TABLE service_reminders ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL';

UPDATE service_reminders SET source_type = 'VIGNETTE' WHERE title = 'Vignette renewal';
UPDATE service_reminders SET source_type = 'INSURANCE' WHERE title = 'Insurance renewal';
UPDATE service_reminders SET source_type = 'INSPECTION' WHERE title = 'Inspection due';