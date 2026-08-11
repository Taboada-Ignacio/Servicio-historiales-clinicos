package com.historialclinico.archivo.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public interface ClinicalFileStorage {
    void almacenar(String storageKey, byte[] contenido, String mimeType);
    byte[] leer(String storageKey);
    default InputStream abrirLectura(String storageKey) { return new ByteArrayInputStream(leer(storageKey)); }
    boolean existe(String storageKey);
    void eliminar(String storageKey);
}
