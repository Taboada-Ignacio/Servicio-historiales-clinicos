package com.historialclinico.exportacion.dto;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ArchivoHistoriaClinica {
    private final byte[] contenido;
    private final Path archivoTemporal;
    private final long longitud;
    private final String tipoContenido;
    private final String nombreArchivo;

    private ArchivoHistoriaClinica(byte[] contenido, Path archivoTemporal, long longitud,
            String tipoContenido, String nombreArchivo) {
        this.contenido = contenido == null ? null : contenido.clone();
        this.archivoTemporal = archivoTemporal;
        this.longitud = longitud;
        this.tipoContenido = tipoContenido;
        this.nombreArchivo = nombreArchivo;
    }

    public static ArchivoHistoriaClinica enMemoria(byte[] contenido, String tipoContenido, String nombreArchivo) {
        return new ArchivoHistoriaClinica(contenido, null, contenido.length, tipoContenido, nombreArchivo);
    }

    public static ArchivoHistoriaClinica temporal(Path archivo, long longitud,
            String tipoContenido, String nombreArchivo) {
        return new ArchivoHistoriaClinica(null, archivo, longitud, tipoContenido, nombreArchivo);
    }

    public void escribirEn(OutputStream salida) throws IOException {
        if (contenido != null) salida.write(contenido);
        else try (var entrada = Files.newInputStream(archivoTemporal)) { entrada.transferTo(salida); }
    }

    public InputStream abrirLectura() throws IOException {
        if (contenido != null) return new ByteArrayInputStream(contenido);
        return new FilterInputStream(Files.newInputStream(archivoTemporal)) {
            @Override public void close() throws IOException {
                try { super.close(); }
                finally { limpiar(); }
            }
        };
    }

    public void limpiar() {
        if (archivoTemporal == null) return;
        try { Files.deleteIfExists(archivoTemporal); }
        catch (IOException ignored) {
            // El archivo temporal no contiene credenciales y será eliminado por el sistema operativo.
        }
    }

    public long longitud() { return longitud; }
    public String tipoContenido() { return tipoContenido; }
    public String nombreArchivo() { return nombreArchivo; }
}
