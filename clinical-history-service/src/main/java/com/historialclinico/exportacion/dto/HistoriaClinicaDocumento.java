package com.historialclinico.exportacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record HistoriaClinicaDocumento(
        Instant fechaGeneracion,
        Paciente paciente,
        List<RegistroClinico> registros
) {
    public record Paciente(String nombre, String apellido, String dni, String telefono,
                           LocalDate fechaNacimiento, String sexo) {}
    public record RegistroClinico(Instant fecha, String tipo, String titulo, List<Campo> campos) {}
    public record Campo(String nombre, String valor) {}
}
