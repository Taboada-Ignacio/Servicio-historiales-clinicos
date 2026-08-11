package com.historialclinico.excepcion;

public class ExcepcionServicioArchivosNoDisponible extends RuntimeException {
    public ExcepcionServicioArchivosNoDisponible(String mensaje, Throwable causa) { super(mensaje, causa); }
}
