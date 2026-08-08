package com.historialclinico.tratamiento.dto;

import com.historialclinico.auditoria.dto.SolicitudMotivoRectificacion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SolicitudRectificacionSesion(
        @NotNull @Valid SolicitudMotivoRectificacion rectificacion,
        @Size(max = 1000) String observaciones,
        @Positive Long idFichaSeguimiento,
        @Valid List<SolicitudTratamiento.SolicitudRespuesta> respuestasFichaSeguimiento
) {}
