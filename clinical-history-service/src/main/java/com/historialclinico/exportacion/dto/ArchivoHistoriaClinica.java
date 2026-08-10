package com.historialclinico.exportacion.dto;

public record ArchivoHistoriaClinica(byte[] contenido, String tipoContenido, String nombreArchivo) {
    public ArchivoHistoriaClinica {
        contenido = contenido.clone();
    }

    @Override public byte[] contenido() { return contenido.clone(); }
}
