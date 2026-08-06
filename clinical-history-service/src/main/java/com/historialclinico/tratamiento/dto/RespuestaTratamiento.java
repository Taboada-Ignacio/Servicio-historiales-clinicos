package com.historialclinico.tratamiento.dto;
import java.time.Instant;
import java.util.List;
public record RespuestaTratamiento(Long id, Long idPaciente, String nombre, String descripcion,
    int cantidadSesionesTotal, int cantidadSesionesFaltantes, Instant fechaCreacion, List<RespuestaSesion> sesiones) {
    public record RespuestaSesion(Long id, int nroSesion, String observaciones, Instant fechaHora,
        Long idFichaSeguimiento, String nombreFichaSeguimiento) {}
}
