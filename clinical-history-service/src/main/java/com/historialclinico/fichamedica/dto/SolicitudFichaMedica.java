package com.historialclinico.fichamedica.dto;

import com.historialclinico.fichamedica.modelo.TipoOpcion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SolicitudFichaMedica(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @NotEmpty List<@Valid SolicitudDetalle> detalles
) {
    public record SolicitudDetalle(
            @NotBlank @Size(max = 150) String titulo,
            @Size(max = 500) String descripcion,
            @PositiveOrZero int orden,
            @NotEmpty List<@Valid SolicitudCampo> campos
    ) {}

    public record SolicitudCampo(
            @NotBlank @Size(max = 150) String titulo,
            @Size(max = 500) String descripcion,
            @PositiveOrZero int orden,
            boolean permiteSeleccionMultiple,
            @NotEmpty List<@Valid SolicitudOpcion> opciones
    ) {}

    public record SolicitudOpcion(
            @Size(max = 150) String titulo,
            @NotNull TipoOpcion tipo,
            @Size(max = 500) String descripcion,
            @PositiveOrZero int orden,
            @Size(max = 80) String grupoExclusion
    ) {}
}
