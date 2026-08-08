package com.historialclinico.auditoria.dto;

import com.historialclinico.auditoria.modelo.TipoMotivoRectificacion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudMotivoRectificacion(
        @NotNull @Min(1) @Max(Integer.MAX_VALUE) Integer versionEsperada,
        @NotNull TipoMotivoRectificacion tipoMotivo,
        @NotBlank @Size(min = 10, max = 500) String motivo
) {}

