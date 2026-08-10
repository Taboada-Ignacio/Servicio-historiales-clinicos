package com.historialclinico.exportacion.dto;

import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.modelo.MotivoExportacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudExportacionHistoriaClinica(
        @NotNull(message = "El formato es obligatorio") FormatoExportacion formato,
        @NotNull(message = "El motivo es obligatorio") MotivoExportacion motivo,
        @Size(max = 500, message = "El detalle del motivo no puede superar los 500 caracteres") String detalleMotivo
) {}
