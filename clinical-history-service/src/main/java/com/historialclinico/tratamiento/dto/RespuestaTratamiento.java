package com.historialclinico.tratamiento.dto;
import java.time.Instant;
import java.util.List;
import com.historialclinico.auditoria.modelo.EstadoRegistroClinico;
import com.historialclinico.compartido.dto.RespuestaFichaClinica;
public record RespuestaTratamiento(Long id, Long idPaciente, String nombre, String descripcion,
    int cantidadSesionesTotal, int cantidadSesionesFaltantes, Instant fechaCreacion, int versionClinica,
    EstadoRegistroClinico estadoRegistro, Instant fechaUltimaRectificacion, List<RespuestaSesion> sesiones) {
    public record RespuestaSesion(Long id, int nroSesion, String observaciones, Instant fechaHora,
        Long idFichaSeguimiento, String nombreFichaSeguimiento, RespuestaFichaClinica fichaCompletada, int versionClinica,
        EstadoRegistroClinico estadoRegistro, Instant fechaUltimaRectificacion) {}
}
