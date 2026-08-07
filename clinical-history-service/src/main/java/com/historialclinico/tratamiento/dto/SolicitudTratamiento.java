package com.historialclinico.tratamiento.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record SolicitudTratamiento(
    @NotBlank @Size(max=150) String nombre,
    @Size(max=1000) String descripcion,
    @NotNull @Min(1) @Max(1000) Integer cantidadSesionesTotal,
    @Valid SolicitudSesion primeraSesion
) {
    public record SolicitudSesion(@Size(max=1000) String observaciones,
        @Positive Long idFichaSeguimiento, List<@Valid SolicitudRespuesta> respuestasFichaSeguimiento) {}
    public record SolicitudRespuesta(@NotNull @Positive Long idOpcion, @Size(max=1000) String valor, Boolean seleccionada) {}
}
