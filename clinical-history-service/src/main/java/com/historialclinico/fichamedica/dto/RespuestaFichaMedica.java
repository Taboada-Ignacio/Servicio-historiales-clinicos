package com.historialclinico.fichamedica.dto;

import com.historialclinico.fichamedica.modelo.TipoOpcion;

import java.time.Instant;
import java.util.List;

public record RespuestaFichaMedica(
        Long id,
        Long idProfesional,
        String nombre,
        String descripcion,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        long version,
        List<RespuestaDetalle> detalles
) {
    public record RespuestaDetalle(
            Long id,
            String titulo,
            String descripcion,
            int orden,
            List<RespuestaCampo> campos
    ) {}

    public record RespuestaCampo(
            Long id,
            String titulo,
            String descripcion,
            int orden,
            boolean permiteSeleccionMultiple,
            List<RespuestaOpcion> opciones
    ) {}

    public record RespuestaOpcion(
            Long id,
            String titulo,
            TipoOpcion tipo,
            String descripcion,
            int orden,
            String grupoExclusion
    ) {}
}
