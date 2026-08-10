package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;

public interface HistoriaClinicaExporter {
    FormatoExportacion formato();
    byte[] exportar(HistoriaClinicaDocumento documento);
}
