package com.historialclinico.exportacion.servicio;

import java.nio.file.Path;

record PaqueteExportacion(Path archivo, long longitud, String integridad) {}
