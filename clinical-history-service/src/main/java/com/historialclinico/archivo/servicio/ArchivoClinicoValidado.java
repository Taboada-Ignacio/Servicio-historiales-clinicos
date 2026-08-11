package com.historialclinico.archivo.servicio;

public record ArchivoClinicoValidado(
        byte[] contenido,
        String nombreOriginal,
        String extension,
        String mimeType,
        long sizeBytes
) {}
