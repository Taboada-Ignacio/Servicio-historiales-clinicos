package com.historialclinico.epicrisis.dto;

import java.time.Instant;
import com.historialclinico.auditoria.modelo.EstadoRegistroClinico;
import com.historialclinico.compartido.dto.RespuestaFichaClinica;

public record RespuestaEpicrisis(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        String apellidoPaciente,
        Long idFichaSeguimiento,
        String nombreFichaSeguimiento,
        RespuestaFichaClinica fichaCompletada,
        Instant fechaHora,
        String observaciones,
        int versionClinica,
        EstadoRegistroClinico estadoRegistro,
        Instant fechaUltimaRectificacion
) {}
