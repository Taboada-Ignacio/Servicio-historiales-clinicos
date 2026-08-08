package com.historialclinico.compartido.dto;

import java.util.List;

public record RespuestaFichaClinica(Long idFichaSeguimiento, String nombreFichaSeguimiento,
        List<Respuesta> respuestas) {
    public record Respuesta(Long idOpcion, String valor, Boolean seleccionada) {}
}
