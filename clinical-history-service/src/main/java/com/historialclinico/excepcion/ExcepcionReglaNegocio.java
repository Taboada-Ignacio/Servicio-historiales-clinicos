package com.historialclinico.excepcion;

public class ExcepcionReglaNegocio extends RuntimeException {
    public ExcepcionReglaNegocio(String mensaje) {
        super(mensaje);
    }
}
