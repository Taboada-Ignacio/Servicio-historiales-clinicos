package com.historialclinico.archivo.dto;

import com.historialclinico.archivo.modelo.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaDocumentoClinico(
        UUID id,
        Long pacienteId,
        ContextoDocumentoClinico contexto,
        Long contextoId,
        CategoriaDocumentoClinico categoria,
        String descripcion,
        String nombreOriginal,
        String mimeType,
        long sizeBytes,
        int version,
        EstadoDocumentoClinico estado,
        String warningStorage,
        String warningDuplicate,
        Instant createdAt,
        Instant updatedAt,
        List<RespuestaVersionDocumento> versiones
) {}
