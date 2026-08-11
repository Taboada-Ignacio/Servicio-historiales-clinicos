package com.historialclinico.archivo.dto;

public record ArchivoClinicoDescargado(byte[] contenido, String mimeType, String nombreOriginal) {}
