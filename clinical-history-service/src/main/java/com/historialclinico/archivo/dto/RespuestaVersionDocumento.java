package com.historialclinico.archivo.dto;

import com.historialclinico.archivo.modelo.EstadoVersionDocumento;

import java.time.Instant;
import java.util.UUID;

public record RespuestaVersionDocumento(
        UUID id,
        int numeroVersion,
        String nombreOriginal,
        String extension,
        String mimeType,
        long sizeBytes,
        EstadoVersionDocumento estado,
        String motivoCambio,
        Instant createdAt
) {}
