ALTER TABLE exportaciones_historia_clinica
    RENAME COLUMN formato TO formato_historia_clinica;

ALTER TABLE exportaciones_historia_clinica
    ADD COLUMN tipo_exportacion VARCHAR(40) NOT NULL DEFAULT 'HISTORIA_CLINICA';

ALTER TABLE exportaciones_historia_clinica
    ADD COLUMN formato_archivo_final VARCHAR(10);

UPDATE exportaciones_historia_clinica
SET formato_archivo_final = formato_historia_clinica;

ALTER TABLE exportaciones_historia_clinica
    ALTER COLUMN formato_archivo_final SET NOT NULL;
