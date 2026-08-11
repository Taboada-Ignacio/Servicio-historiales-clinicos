package com.historialclinico.exportacion.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HistoriaClinicaDocumento(
        Instant fechaGeneracion,
        Paciente paciente,
        List<RegistroClinico> registros,
        List<ArchivoAdjunto> archivosPaciente
) {
    public HistoriaClinicaDocumento {
        registros = List.copyOf(registros);
        archivosPaciente = List.copyOf(archivosPaciente);
    }

    public HistoriaClinicaDocumento(Instant fechaGeneracion, Paciente paciente, List<RegistroClinico> registros) {
        this(fechaGeneracion, paciente, registros, List.of());
    }

    public record Paciente(String nombre, String apellido, String dni, String telefono,
                           LocalDate fechaNacimiento, String sexo) {}
    public record RegistroClinico(Instant fecha, String tipo, String titulo, List<Campo> campos,
                                  List<ArchivoAdjunto> archivosAdjuntos) {
        public RegistroClinico {
            campos = List.copyOf(campos);
            archivosAdjuntos = List.copyOf(archivosAdjuntos);
        }

        public RegistroClinico(Instant fecha, String tipo, String titulo, List<Campo> campos) {
            this(fecha, tipo, titulo, campos, List.of());
        }
    }
    public record Campo(String nombre, String valor) {}
    public record ArchivoAdjunto(UUID documentoId, String nombreOriginal, String categoria,
                                 String contexto, Long contextoId, String mimeType, long sizeBytes,
                                 String integridad, String descripcion) {}
}
