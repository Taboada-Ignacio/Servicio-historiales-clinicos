ALTER TABLE epicrisis ADD COLUMN id_ficha_seguimiento BIGINT;

ALTER TABLE epicrisis
    ADD CONSTRAINT fk_epicrisis_ficha_seguimiento
    FOREIGN KEY (id_ficha_seguimiento) REFERENCES fichas_medicas (id) ON DELETE SET NULL;

CREATE INDEX idx_epicrisis_ficha_seguimiento ON epicrisis (id_ficha_seguimiento);
