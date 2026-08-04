package com.historialclinico.excepcion;

import java.time.Instant;
import java.util.List;

public record ErrorApi(
        Instant marcaTiempo,
        int estado,
        String error,
        String mensaje,
        String ruta,
        List<ViolacionCampo> violaciones
) {
    public record ViolacionCampo(String campo, String mensaje) {}
}
