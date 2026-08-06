ALTER TABLE epicrisis DROP CONSTRAINT fk_epicrisis_ficha_paciente_seguimiento;

ALTER TABLE epicrisis
    ADD CONSTRAINT fk_epicrisis_ficha_paciente_seguimiento
        FOREIGN KEY (id_ficha_paciente_seguimiento) REFERENCES fichas_paciente(id)
        ON DELETE SET NULL;
