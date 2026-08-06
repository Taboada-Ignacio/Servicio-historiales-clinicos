package com.historialclinico.epicrisis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SolicitudEpicrisis(
        @NotBlank(message = "no puede estar vacía")
        @Size(max = 1000, message = "no puede superar los 1000 caracteres")
        String observaciones,
        @Positive Long idFichaSeguimiento,
        List<@Valid SolicitudRespuesta> respuestasFichaSeguimiento
) {
    public record SolicitudRespuesta(
            @NotNull @Positive Long idOpcion,
            @Size(max = 1000) String valor,
            Boolean seleccionada
    ) {}
}
