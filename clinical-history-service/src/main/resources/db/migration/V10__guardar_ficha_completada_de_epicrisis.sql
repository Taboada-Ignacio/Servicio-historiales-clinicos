ALTER TABLE epicrisis
    ADD COLUMN id_ficha_paciente_seguimiento BIGINT NULL;

ALTER TABLE epicrisis
    ADD CONSTRAINT fk_epicrisis_ficha_paciente_seguimiento
        FOREIGN KEY (id_ficha_paciente_seguimiento) REFERENCES fichas_paciente(id);

CREATE INDEX idx_epicrisis_ficha_paciente_seguimiento
    ON epicrisis(id_ficha_paciente_seguimiento);
