CREATE TABLE documentos_clinicos (
    id UUID PRIMARY KEY,
    id_paciente BIGINT NOT NULL,
    contexto VARCHAR(20) NOT NULL,
    contexto_id BIGINT NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    descripcion VARCHAR(1000),
    estado VARCHAR(20) NOT NULL,
    current_version_id UUID,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_eliminacion TIMESTAMP WITH TIME ZONE,
    conservar_hasta TIMESTAMP WITH TIME ZONE,
    motivo_eliminacion VARCHAR(500),
    version_lock BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_documento_clinico_paciente FOREIGN KEY (id_paciente) REFERENCES pacientes (id),
    CONSTRAINT chk_documento_contexto CHECK (contexto IN ('PACIENTE', 'TRATAMIENTO', 'SESION', 'EPICRISIS')),
    CONSTRAINT chk_documento_contexto_paciente CHECK (contexto <> 'PACIENTE' OR contexto_id = id_paciente),
    CONSTRAINT chk_documento_estado CHECK (estado IN ('ACTIVE', 'DELETED')),
    CONSTRAINT chk_documento_borrado CHECK (
        (estado = 'ACTIVE' AND fecha_eliminacion IS NULL AND conservar_hasta IS NULL AND motivo_eliminacion IS NULL)
        OR
        (estado = 'DELETED' AND fecha_eliminacion IS NOT NULL AND conservar_hasta IS NOT NULL AND motivo_eliminacion IS NOT NULL)
    )
);

CREATE TABLE documentos_clinicos_versiones (
    id UUID PRIMARY KEY,
    documento_id UUID NOT NULL,
    numero_version INTEGER NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    extension VARCHAR(5) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    integridad_hash VARCHAR(64) NOT NULL,
    estado_version VARCHAR(20) NOT NULL,
    current_slot SMALLINT,
    motivo_cambio VARCHAR(500),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_version_documento FOREIGN KEY (documento_id) REFERENCES documentos_clinicos (id),
    CONSTRAINT uq_documento_numero_version UNIQUE (documento_id, numero_version),
    CONSTRAINT uq_documento_current_slot UNIQUE (documento_id, current_slot),
    CONSTRAINT uq_version_documento_compuesta UNIQUE (id, documento_id),
    CONSTRAINT uq_version_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_numero_version CHECK (numero_version > 0),
    CONSTRAINT chk_version_size CHECK (size_bytes > 0 AND size_bytes <= 20971520),
    CONSTRAINT chk_estado_version CHECK (
        (estado_version = 'CURRENT' AND current_slot = 1)
        OR
        (estado_version = 'HISTORICAL' AND current_slot IS NULL)
    )
);

ALTER TABLE documentos_clinicos
    ADD CONSTRAINT fk_documento_version_actual
    FOREIGN KEY (current_version_id, id)
    REFERENCES documentos_clinicos_versiones (id, documento_id);

CREATE INDEX idx_documentos_paciente_estado
    ON documentos_clinicos (id_paciente, estado, fecha_creacion);
CREATE INDEX idx_documentos_contexto
    ON documentos_clinicos (contexto, contexto_id);
CREATE INDEX idx_versiones_documento
    ON documentos_clinicos_versiones (documento_id, numero_version DESC);
CREATE INDEX idx_versiones_integridad_paciente
    ON documentos_clinicos_versiones (integridad_hash, documento_id);

ALTER TABLE audit_log ADD COLUMN documento_id UUID;
ALTER TABLE audit_log ADD COLUMN contexto VARCHAR(20);
ALTER TABLE audit_log ADD COLUMN contexto_id BIGINT;
ALTER TABLE audit_log ADD COLUMN motivo VARCHAR(500);
ALTER TABLE audit_log ADD COLUMN version_anterior INTEGER;
ALTER TABLE audit_log ADD COLUMN version_restaurada INTEGER;

CREATE INDEX idx_audit_log_documento
    ON audit_log (documento_id, fecha_hora);
