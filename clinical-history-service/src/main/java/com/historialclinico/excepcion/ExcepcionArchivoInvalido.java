package com.historialclinico.excepcion;

public class ExcepcionArchivoInvalido extends RuntimeException {
    public ExcepcionArchivoInvalido(String mensaje) { super(mensaje); }
    public ExcepcionArchivoInvalido(String mensaje, Throwable causa) { super(mensaje, causa); }
}
