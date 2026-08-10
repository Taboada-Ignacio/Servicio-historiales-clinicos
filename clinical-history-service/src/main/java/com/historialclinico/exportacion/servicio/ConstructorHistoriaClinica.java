package com.historialclinico.exportacion.servicio;

import com.historialclinico.epicrisis.modelo.Epicrisis;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
import com.historialclinico.paciente.modelo.*;
import com.historialclinico.tratamiento.modelo.SesionTratamiento;
import com.historialclinico.tratamiento.modelo.Tratamiento;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ConstructorHistoriaClinica {
    private final RepositorioEpicrisis repositorioEpicrisis;
    private final RepositorioTratamiento repositorioTratamientos;

    public ConstructorHistoriaClinica(RepositorioEpicrisis repositorioEpicrisis,
            RepositorioTratamiento repositorioTratamientos) {
        this.repositorioEpicrisis = repositorioEpicrisis;
        this.repositorioTratamientos = repositorioTratamientos;
    }

    public HistoriaClinicaDocumento construir(Paciente paciente, Instant fechaGeneracion) {
        List<HistoriaClinicaDocumento.RegistroClinico> registros = new ArrayList<>();
        paciente.getFichasAsignadas().stream()
                .filter(ficha -> ficha.getOrigen() == OrigenFichaPaciente.DIRECTA)
                .forEach(ficha -> registros.add(registroFicha(ficha)));

        repositorioEpicrisis.findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaHoraDesc(
                paciente.getId(), paciente.getIdProfesional()).forEach(epicrisis -> registros.add(registroEpicrisis(epicrisis)));

        repositorioTratamientos.findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaCreacionDesc(
                paciente.getId(), paciente.getIdProfesional()).forEach(tratamiento -> {
                    registros.add(registroTratamiento(tratamiento));
                    tratamiento.getSesiones().forEach(sesion -> registros.add(registroSesion(tratamiento, sesion)));
                });

        registros.sort(Comparator.comparing(HistoriaClinicaDocumento.RegistroClinico::fecha)
                .thenComparing(HistoriaClinicaDocumento.RegistroClinico::tipo));
        var datosPaciente = new HistoriaClinicaDocumento.Paciente(paciente.getNombre(), paciente.getApellido(),
                paciente.getDni(), paciente.getTelefono(), paciente.getFechaNacimiento(), paciente.getSexo().name());
        return new HistoriaClinicaDocumento(fechaGeneracion, datosPaciente, List.copyOf(registros));
    }

    private HistoriaClinicaDocumento.RegistroClinico registroFicha(FichaPaciente ficha) {
        return new HistoriaClinicaDocumento.RegistroClinico(ficha.getFechaAsignacion(), "FICHA MÉDICA",
                ficha.getNombreFicha(), camposFicha(ficha));
    }

    private HistoriaClinicaDocumento.RegistroClinico registroEpicrisis(Epicrisis epicrisis) {
        List<HistoriaClinicaDocumento.Campo> campos = new ArrayList<>();
        campos.add(campo("Observaciones", epicrisis.getObservaciones()));
        agregarEstado(campos, epicrisis.getEstadoRegistro().name());
        agregarFicha(campos, epicrisis.getFichaPacienteSeguimiento());
        return new HistoriaClinicaDocumento.RegistroClinico(epicrisis.getFechaHora(), "EPICRISIS",
                "Epicrisis", List.copyOf(campos));
    }

    private HistoriaClinicaDocumento.RegistroClinico registroTratamiento(Tratamiento tratamiento) {
        List<HistoriaClinicaDocumento.Campo> campos = new ArrayList<>();
        campos.add(campo("Descripción", tratamiento.getDescripcion()));
        campos.add(campo("Sesiones planificadas", String.valueOf(tratamiento.getCantidadSesionesTotal())));
        campos.add(campo("Sesiones realizadas", String.valueOf(
                tratamiento.getCantidadSesionesTotal() - tratamiento.getCantidadSesionesFaltantes())));
        agregarEstado(campos, tratamiento.getEstadoRegistro().name());
        return new HistoriaClinicaDocumento.RegistroClinico(tratamiento.getFechaCreacion(), "TRATAMIENTO",
                tratamiento.getNombre(), List.copyOf(campos));
    }

    private HistoriaClinicaDocumento.RegistroClinico registroSesion(Tratamiento tratamiento, SesionTratamiento sesion) {
        List<HistoriaClinicaDocumento.Campo> campos = new ArrayList<>();
        campos.add(campo("Tratamiento", tratamiento.getNombre()));
        campos.add(campo("Observaciones", sesion.getObservaciones()));
        agregarEstado(campos, sesion.getEstadoRegistro().name());
        agregarFicha(campos, sesion.getFichaPacienteSeguimiento());
        return new HistoriaClinicaDocumento.RegistroClinico(sesion.getFechaHora(), "CONSULTA / SESIÓN",
                "Sesión N.º " + sesion.getNroSesion(), List.copyOf(campos));
    }

    private void agregarEstado(List<HistoriaClinicaDocumento.Campo> campos, String estado) {
        if (!"VIGENTE".equals(estado)) campos.add(campo("Estado del registro", estado));
    }

    private void agregarFicha(List<HistoriaClinicaDocumento.Campo> campos, FichaPaciente ficha) {
        if (ficha == null) return;
        campos.add(campo("Ficha médica", ficha.getNombreFicha()));
        campos.addAll(camposFicha(ficha));
    }

    private List<HistoriaClinicaDocumento.Campo> camposFicha(FichaPaciente ficha) {
        List<HistoriaClinicaDocumento.Campo> campos = new ArrayList<>();
        ficha.getRespuestas().stream()
                .sorted(Comparator.comparingInt(RespuestaCampo::getOrdenDetalle)
                        .thenComparingInt(RespuestaCampo::getOrdenCampo)
                        .thenComparingInt(RespuestaCampo::getOrdenOpcion))
                .forEach(respuesta -> {
                    if (respuesta.getTipoOpcion() == TipoOpcion.SELECCION) {
                        if (Boolean.TRUE.equals(respuesta.getSeleccionada()))
                            campos.add(campo(respuesta.getTituloCampo(), respuesta.getTituloOpcion()));
                    } else if (respuesta.getTipoOpcion() == TipoOpcion.SI_NO) {
                        String valor = respuesta.getValor();
                        if (valor == null || valor.isBlank()) valor = Boolean.TRUE.equals(respuesta.getSeleccionada()) ? "Sí" : "No";
                        campos.add(campo(respuesta.getTituloCampo(), valor));
                    } else campos.add(campo(respuesta.getTituloCampo(), respuesta.getValor()));
                });
        return campos;
    }

    private HistoriaClinicaDocumento.Campo campo(String nombre, String valor) {
        return new HistoriaClinicaDocumento.Campo(nombre,
                valor == null || valor.isBlank() ? "No informado" : valor.trim());
    }
}
