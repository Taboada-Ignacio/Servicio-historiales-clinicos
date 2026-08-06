CREATE TABLE tratamientos (
    id BIGSERIAL PRIMARY KEY,
    id_paciente BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(1000),
    cantidad_sesiones_total INTEGER NOT NULL,
    cantidad_sesiones_faltantes INTEGER NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_tratamiento_paciente FOREIGN KEY (id_paciente) REFERENCES pacientes (id) ON DELETE CASCADE,
    CONSTRAINT chk_tratamiento_sesiones_total CHECK (cantidad_sesiones_total > 0),
    CONSTRAINT chk_tratamiento_sesiones_faltantes CHECK (
        cantidad_sesiones_faltantes >= 0 AND cantidad_sesiones_faltantes <= cantidad_sesiones_total
    )
);

CREATE TABLE sesiones_tratamiento (
    id BIGSERIAL PRIMARY KEY,
    id_tratamiento BIGINT NOT NULL,
    nro_sesion INTEGER NOT NULL,
    observaciones VARCHAR(1000) NOT NULL,
    fecha_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    id_ficha_seguimiento BIGINT,
    id_ficha_paciente_seguimiento BIGINT,
    CONSTRAINT fk_sesion_tratamiento FOREIGN KEY (id_tratamiento) REFERENCES tratamientos (id) ON DELETE CASCADE,
    CONSTRAINT fk_sesion_ficha FOREIGN KEY (id_ficha_seguimiento) REFERENCES fichas_medicas (id) ON DELETE SET NULL,
    CONSTRAINT fk_sesion_ficha_completada FOREIGN KEY (id_ficha_paciente_seguimiento) REFERENCES fichas_paciente (id) ON DELETE SET NULL,
    CONSTRAINT uq_sesion_numero UNIQUE (id_tratamiento, nro_sesion),
    CONSTRAINT chk_sesion_numero CHECK (nro_sesion > 0)
);

CREATE INDEX idx_tratamientos_paciente ON tratamientos (id_paciente, fecha_creacion DESC);
CREATE INDEX idx_sesiones_tratamiento ON sesiones_tratamiento (id_tratamiento, nro_sesion);
