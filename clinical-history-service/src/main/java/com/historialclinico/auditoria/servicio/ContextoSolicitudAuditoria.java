package com.historialclinico.auditoria.servicio;

public record ContextoSolicitudAuditoria(Long idProfesional, String nombreProfesional, String matriculaProfesional,
        String ipOrigen, String equipo, String idSesion, String idSolicitud) {}

