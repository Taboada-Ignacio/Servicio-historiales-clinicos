package com.historialclinico.epicrisis.dto;

import java.time.Instant;

public record RespuestaEpicrisis(
        Long id,
        Long idPaciente,
        String nombrePaciente,
        String apellidoPaciente,
        Long idFichaSeguimiento,
        String nombreFichaSeguimiento,
        Instant fechaHora,
        String observaciones
) {}
