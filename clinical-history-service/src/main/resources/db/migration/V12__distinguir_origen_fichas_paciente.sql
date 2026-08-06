ALTER TABLE fichas_paciente
    ADD COLUMN origen VARCHAR(20) NOT NULL DEFAULT 'DIRECTA';

UPDATE fichas_paciente
SET origen = 'EPICRISIS'
WHERE id IN (
    SELECT id_ficha_paciente_seguimiento
    FROM epicrisis
    WHERE id_ficha_paciente_seguimiento IS NOT NULL
);
