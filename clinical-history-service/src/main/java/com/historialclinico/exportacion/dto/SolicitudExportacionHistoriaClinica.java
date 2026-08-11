package com.historialclinico.exportacion.dto;

import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.modelo.MotivoExportacion;
import com.historialclinico.exportacion.modelo.TipoExportacion;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudExportacionHistoriaClinica(
        @NotNull(message = "El formato es obligatorio") FormatoExportacion formato,
        TipoExportacion tipoExportacion,
        @NotNull(message = "El motivo es obligatorio") MotivoExportacion motivo,
        @Size(max = 500, message = "El detalle del motivo no puede superar los 500 caracteres") String detalleMotivo
) {
    public SolicitudExportacionHistoriaClinica {
        if (tipoExportacion == null) tipoExportacion = TipoExportacion.HISTORIA_CLINICA;
    }

    @AssertTrue(message = "La exportación con adjuntos sólo admite PDF o DOCX como documento principal")
    public boolean isCombinacionFormatoValida() {
        return formato == null || tipoExportacion == TipoExportacion.HISTORIA_CLINICA
                || formato == FormatoExportacion.PDF || formato == FormatoExportacion.DOCX;
    }
}
