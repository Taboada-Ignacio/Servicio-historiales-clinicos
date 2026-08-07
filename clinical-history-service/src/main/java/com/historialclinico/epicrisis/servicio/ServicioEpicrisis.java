package com.historialclinico.epicrisis.servicio;

import com.historialclinico.epicrisis.dto.RespuestaEpicrisis;
import com.historialclinico.epicrisis.dto.SolicitudEpicrisis;
import com.historialclinico.epicrisis.modelo.Epicrisis;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.fichamedica.modelo.FichaMedica;
import com.historialclinico.fichamedica.repositorio.RepositorioFichaMedica;
import com.historialclinico.fichamedica.modelo.CampoParaLlenar;
import com.historialclinico.fichamedica.modelo.OpcionCampo;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
import com.historialclinico.paciente.modelo.FichaPaciente;
import com.historialclinico.paciente.modelo.RespuestaCampo;
import com.historialclinico.paciente.modelo.OrigenFichaPaciente;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServicioEpicrisis {

    private final RepositorioEpicrisis repositorio;
    private final RepositorioPaciente repositorioPacientes;
    private final RepositorioFichaMedica repositorioFichas;

    public ServicioEpicrisis(RepositorioEpicrisis repositorio, RepositorioPaciente repositorioPacientes,
                             RepositorioFichaMedica repositorioFichas) {
        this.repositorio = repositorio;
        this.repositorioPacientes = repositorioPacientes;
        this.repositorioFichas = repositorioFichas;
    }

    @Transactional
    public RespuestaEpicrisis registrar(Long idProfesional, Long idPaciente, SolicitudEpicrisis solicitud) {
        Paciente paciente = buscarPaciente(idProfesional, idPaciente);
        FichaMedica fichaSeguimiento = solicitud.idFichaSeguimiento() == null ? null
                : repositorioFichas.buscarPorIdYProfesional(solicitud.idFichaSeguimiento(), idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica de seguimiento no encontrada"));
        FichaPaciente fichaCompletada = fichaSeguimiento == null ? null
                : construirFichaCompletada(fichaSeguimiento, solicitud.respuestasFichaSeguimiento());
        if (fichaCompletada != null) paciente.asignarFicha(fichaCompletada);
        Epicrisis epicrisis = new Epicrisis(paciente, fichaSeguimiento, fichaCompletada,
                normalizarObservaciones(solicitud.observaciones()));
        return convertir(repositorio.save(epicrisis));
    }

    private String normalizarObservaciones(String observaciones) {
        return observaciones == null || observaciones.isBlank() ? "Sin observaciones" : observaciones.trim();
    }

    private FichaPaciente construirFichaCompletada(FichaMedica ficha,
            List<SolicitudEpicrisis.SolicitudRespuesta> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty())
            throw new ExcepcionReglaNegocio("Debe completar la ficha de seguimiento seleccionada");
        Map<Long, SolicitudEpicrisis.SolicitudRespuesta> respuestas = solicitudes.stream()
                .collect(Collectors.toMap(SolicitudEpicrisis.SolicitudRespuesta::idOpcion, Function.identity(),
                        (a, b) -> { throw new ExcepcionReglaNegocio("No se puede responder dos veces la misma opción"); }));
        List<OpcionCampo> opciones = ficha.getDetalles().stream().flatMap(d -> d.getCampos().stream())
                .flatMap(c -> c.getOpciones().stream()).toList();
        Set<Long> idsValidos = opciones.stream().map(OpcionCampo::getId).collect(Collectors.toSet());
        if (!idsValidos.equals(respuestas.keySet()))
            throw new ExcepcionReglaNegocio("Deben completarse todas las opciones de la ficha de seguimiento");
        ficha.getDetalles().stream().flatMap(d -> d.getCampos().stream())
                .forEach(campo -> validarCampo(campo, respuestas));
        FichaPaciente completada = new FichaPaciente(ficha, OrigenFichaPaciente.EPICRISIS);
        opciones.forEach(opcion -> {
            var respuesta = respuestas.get(opcion.getId());
            String valor = opcion.getTipo() == TipoOpcion.ENTRADA &&
                    (respuesta.valor() == null || respuesta.valor().isBlank()) ? "No aplica" :
                    (respuesta.valor() == null || respuesta.valor().isBlank() ? null : respuesta.valor().trim());
            completada.agregarRespuesta(new RespuestaCampo(opcion, valor, respuesta.seleccionada()));
        });
        return completada;
    }

    private void validarCampo(CampoParaLlenar campo,
            Map<Long, SolicitudEpicrisis.SolicitudRespuesta> respuestas) {
        List<OpcionCampo> seleccionables = campo.getOpciones().stream()
                .filter(o -> o.getTipo() == TipoOpcion.SELECCION).toList();
        for (OpcionCampo opcion : campo.getOpciones()) {
            var respuesta = respuestas.get(opcion.getId());
            if (opcion.getTipo() == TipoOpcion.SI_NO && !("SI".equals(respuesta.valor()) || "NO".equals(respuesta.valor())))
                throw new ExcepcionReglaNegocio("Debe responderse Sí o No en " + campo.getTitulo());
            if (opcion.getTipo() == TipoOpcion.SELECCION && respuesta.seleccionada() == null)
                throw new ExcepcionReglaNegocio("Debe indicarse la selección de todas las opciones");
        }
        long cantidad = seleccionables.stream().filter(o -> Boolean.TRUE.equals(respuestas.get(o.getId()).seleccionada())).count();
        if (!campo.isPermiteSeleccionMultiple() && cantidad > 1)
            throw new ExcepcionReglaNegocio("El campo " + campo.getTitulo() + " admite una sola selección");
        seleccionables.stream().filter(o -> o.getGrupoExclusion() != null)
                .collect(Collectors.groupingBy(OpcionCampo::getGrupoExclusion)).forEach((grupo, opciones) -> {
                    if (opciones.stream().filter(o -> Boolean.TRUE.equals(respuestas.get(o.getId()).seleccionada())).count() > 1)
                        throw new ExcepcionReglaNegocio("Las opciones del grupo " + grupo + " son excluyentes");
                });
    }

    public List<RespuestaEpicrisis> buscarDelPaciente(Long idProfesional, Long idPaciente) {
        buscarPaciente(idProfesional, idPaciente);
        return repositorio.findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaHoraDesc(idPaciente, idProfesional)
                .stream().map(this::convertir).toList();
    }

    private Paciente buscarPaciente(Long idProfesional, Long idPaciente) {
        return repositorioPacientes.findByIdAndIdProfesional(idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Paciente no encontrado"));
    }

    private RespuestaEpicrisis convertir(Epicrisis epicrisis) {
        Paciente paciente = epicrisis.getPaciente();
        FichaMedica ficha = epicrisis.getFichaSeguimiento();
        return new RespuestaEpicrisis(epicrisis.getId(), paciente.getId(), paciente.getNombre(),
                paciente.getApellido(), ficha == null ? null : ficha.getId(), ficha == null ? null : ficha.getNombre(),
                epicrisis.getFechaHora(), epicrisis.getObservaciones());
    }
}
