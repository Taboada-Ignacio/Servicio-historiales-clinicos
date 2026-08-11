package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class FilasHistoriaClinica {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    private FilasHistoriaClinica() {}

    static List<Fila> crear(HistoriaClinicaDocumento documento) {
        var filas = new ArrayList<Fila>();
        var paciente = documento.paciente();
        filas.add(new Fila("PACIENTE", "", "Datos identificatorios", "Nombre y apellido",
                paciente.apellido() + ", " + paciente.nombre()));
        filas.add(new Fila("PACIENTE", "", "Datos identificatorios", "DNI", paciente.dni()));
        filas.add(new Fila("PACIENTE", "", "Datos identificatorios", "Fecha de nacimiento",
                paciente.fechaNacimiento().toString()));
        filas.add(new Fila("PACIENTE", "", "Datos identificatorios", "Sexo", paciente.sexo()));
        filas.add(new Fila("PACIENTE", "", "Datos identificatorios", "Teléfono",
                paciente.telefono() == null || paciente.telefono().isBlank() ? "No informado" : paciente.telefono()));
        documento.archivosPaciente().forEach(archivo -> filas.add(new Fila("ARCHIVOS DEL PACIENTE", "",
                "Documentación clínica directa", "Archivo adjunto", describir(archivo))));
        for (var registro : documento.registros()) {
            String fecha = FECHA.format(registro.fecha());
            if (registro.campos().isEmpty()) {
                filas.add(new Fila(registro.tipo(), fecha, registro.titulo(), "", ""));
            } else {
                registro.campos().forEach(campo -> filas.add(new Fila(registro.tipo(), fecha,
                        registro.titulo(), campo.nombre(), campo.valor())));
            }
            registro.archivosAdjuntos().forEach(archivo -> filas.add(new Fila(registro.tipo(), fecha,
                    registro.titulo(), "Archivo adjunto", describir(archivo))));
        }
        return filas;
    }

    private static String describir(HistoriaClinicaDocumento.ArchivoAdjunto archivo) {
        return archivo.descripcion() == null || archivo.descripcion().isBlank()
                ? archivo.nombreOriginal()
                : archivo.nombreOriginal() + " — " + archivo.descripcion();
    }

    record Fila(String seccion, String fecha, String registro, String campo, String valor) {}
}
