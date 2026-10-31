ALTER TABLE payslips ADD COLUMN created_at TIMESTAMP;
ALTER TABLE payslips ADD COLUMN updated_at TIMESTAMP;

UPDATE payslips
SET created_at = issued_at,
    updated_at = issued_at;

ALTER TABLE payslips ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE payslips ALTER COLUMN updated_at SET NOT NULL;
