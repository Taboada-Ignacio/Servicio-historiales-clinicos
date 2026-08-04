package com.historialclinico.excepcion;

public class ExcepcionRecursoNoEncontrado extends RuntimeException {
    public ExcepcionRecursoNoEncontrado(String mensaje) {
        super(mensaje);
    }
}
