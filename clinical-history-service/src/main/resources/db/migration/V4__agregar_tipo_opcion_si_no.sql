ALTER TABLE opciones_campo
    DROP CONSTRAINT chk_opciones_campo_tipo;

ALTER TABLE opciones_campo
    ADD CONSTRAINT chk_opciones_campo_tipo
        CHECK (tipo_opcion IN ('SELECCION', 'ENTRADA', 'SI_NO'));
