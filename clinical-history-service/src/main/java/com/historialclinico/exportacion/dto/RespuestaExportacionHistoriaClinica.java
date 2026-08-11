package com.historialclinico.exportacion.dto;

import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.modelo.FormatoArchivoFinal;
import com.historialclinico.exportacion.modelo.MotivoExportacion;
import com.historialclinico.exportacion.modelo.TipoExportacion;

import java.time.Instant;

public record RespuestaExportacionHistoriaClinica(
        Long id,
        Long pacienteId,
        Long profesionalId,
        MotivoExportacion motivo,
        String detalleMotivo,
        FormatoExportacion formato,
        TipoExportacion tipoExportacion,
        FormatoExportacion formatoHistoriaClinica,
        FormatoArchivoFinal formatoArchivoFinal,
        Instant fechaHoraExportacion,
        String nombreArchivo,
        String hashArchivo
) {}
