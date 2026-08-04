package com.historialclinico.paciente.dto;

import com.historialclinico.paciente.modelo.Sexo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RespuestaPaciente(
        Long id,
        Long idProfesional,
        String nombre,
        String apellido,
        String dni,
        String telefono,
        LocalDate fechaNacimiento,
        Sexo sexo,
        Instant fechaCreacion,
        Instant fechaActualizacion,
        long version,
        List<RespuestaFichaPaciente> fichas
) {
    public record RespuestaFichaPaciente(
            Long id,
            Long idFichaMedica,
            String nombreFicha,
            Instant fechaAsignacion,
            List<RespuestaFicha> respuestas
    ) {}

    public record RespuestaFicha(
            Long id,
            Long idOpcion,
            String tituloDetalle,
            String tituloCampo,
            String tituloOpcion,
            String tipo,
            String valor,
            Boolean seleccionada
    ) {}
}
