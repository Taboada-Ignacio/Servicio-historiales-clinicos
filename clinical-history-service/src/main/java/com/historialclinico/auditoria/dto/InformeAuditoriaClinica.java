package com.historialclinico.auditoria.dto;

import com.historialclinico.auditoria.modelo.TipoRegistroClinico;
import java.time.Instant;
import java.util.List;

public record InformeAuditoriaClinica(String formato, Instant generadoEnUtc, Long idProfesional,
        Long idPaciente, TipoRegistroClinico tipoRegistro, Long idRegistro, int cantidadRectificaciones,
        boolean integridadCadenaValida, String advertencia, List<RespuestaAuditoriaRectificacion> rectificaciones) {}

