package com.historialclinico.auditoria.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.historialclinico.auditoria.modelo.TipoMotivoRectificacion;
import com.historialclinico.auditoria.modelo.TipoRegistroClinico;

import java.time.Instant;

public record RespuestaAuditoriaRectificacion(Long id, TipoRegistroClinico tipoRegistro, Long idRegistro,
        Long idPaciente, Long idProfesional, String nombreProfesional, String matriculaProfesional,
        int versionAnterior, int versionNueva, TipoMotivoRectificacion tipoMotivo, String motivo,
        String resultado, Instant fechaHoraUtc, Instant conservarHasta, String ipOrigen, String equipo,
        String idSesion, String idSolicitud, JsonNode antes, JsonNode despues, String hashAntes,
        String hashDespues, String hashAnteriorCadena, String hashCadena, boolean integridadValida) {}

