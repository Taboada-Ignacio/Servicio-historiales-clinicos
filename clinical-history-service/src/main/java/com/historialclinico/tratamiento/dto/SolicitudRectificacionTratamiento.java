package com.historialclinico.tratamiento.dto;

import com.historialclinico.auditoria.dto.SolicitudMotivoRectificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRectificacionTratamiento(
        @NotNull @Valid SolicitudMotivoRectificacion rectificacion,
        @NotBlank @Size(max = 150) String nombre,
        @Size(max = 1000) String descripcion,
        @NotNull @Min(1) @Max(1000) Integer cantidadSesionesTotal
) {}

