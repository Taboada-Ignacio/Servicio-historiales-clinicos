package com.historialclinico.exportacion.servicio;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;

record AdjuntoExportable(HistoriaClinicaDocumento.ArchivoAdjunto referencia, String storageKey) {}
