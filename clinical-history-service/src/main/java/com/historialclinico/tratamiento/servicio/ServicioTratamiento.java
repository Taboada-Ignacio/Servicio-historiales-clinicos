package com.historialclinico.tratamiento.servicio;

import com.historialclinico.excepcion.*;
import com.historialclinico.fichamedica.modelo.*;
import com.historialclinico.fichamedica.repositorio.RepositorioFichaMedica;
import com.historialclinico.paciente.modelo.*;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.paciente.repositorio.RepositorioFichaPaciente;
import com.historialclinico.tratamiento.dto.*;
import com.historialclinico.tratamiento.modelo.*;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServicioTratamiento {
    private final RepositorioTratamiento repositorio;
    private final RepositorioPaciente repositorioPacientes;
    private final RepositorioFichaMedica repositorioFichas;
    private final RepositorioFichaPaciente repositorioFichasPaciente;
    public ServicioTratamiento(RepositorioTratamiento repositorio, RepositorioPaciente repositorioPacientes,
            RepositorioFichaMedica repositorioFichas, RepositorioFichaPaciente repositorioFichasPaciente) {
        this.repositorio = repositorio; this.repositorioPacientes = repositorioPacientes;
        this.repositorioFichas = repositorioFichas; this.repositorioFichasPaciente = repositorioFichasPaciente;
    }

    @Transactional
    public RespuestaTratamiento crear(Long idProfesional, Long idPaciente, SolicitudTratamiento solicitud) {
        Paciente paciente = buscarPaciente(idProfesional, idPaciente);
        String descripcion = solicitud.descripcion() == null || solicitud.descripcion().isBlank() ? null : solicitud.descripcion().trim();
        Tratamiento tratamiento = new Tratamiento(paciente, solicitud.nombre().trim(), descripcion, solicitud.cantidadSesionesTotal());
        if (solicitud.primeraSesion() != null) {
            var solicitudSesion = solicitud.primeraSesion();
            FichaMedica ficha = solicitudSesion.idFichaSeguimiento() == null ? null : repositorioFichas
                    .buscarPorIdYProfesional(solicitudSesion.idFichaSeguimiento(), idProfesional)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica de la sesión no encontrada"));
            FichaPaciente completada = ficha == null ? null : construirFicha(ficha, solicitudSesion.respuestasFichaSeguimiento());
            if (completada != null) {
                paciente.asignarFicha(completada);
                repositorioFichasPaciente.save(completada);
            }
            tratamiento.agregarSesion(new SesionTratamiento(solicitudSesion.observaciones().trim(), ficha, completada));
        }
        return convertir(repositorio.save(tratamiento));
    }

    public List<RespuestaTratamiento> buscarDelPaciente(Long idProfesional, Long idPaciente) {
        buscarPaciente(idProfesional, idPaciente);
        return repositorio.findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaCreacionDesc(idPaciente, idProfesional)
                .stream().map(this::convertir).toList();
    }

    public List<RespuestaTratamiento> buscarSinTerminar(Long idProfesional, Long idPaciente) {
        buscarPaciente(idProfesional, idPaciente);
        return repositorio
                .findAllByPacienteIdAndPacienteIdProfesionalAndCantidadSesionesFaltantesGreaterThanOrderByFechaCreacionDesc(
                        idPaciente, idProfesional, 0)
                .stream().map(this::convertir).toList();
    }

    @Transactional
    public RespuestaTratamiento registrarSesion(Long idProfesional, Long idPaciente, Long idTratamiento,
            SolicitudTratamiento.SolicitudSesion solicitud) {
        Tratamiento tratamiento = repositorio.buscarParaContinuar(idTratamiento, idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Tratamiento no encontrado"));
        if (tratamiento.getCantidadSesionesFaltantes() == 0)
            throw new ExcepcionReglaNegocio("El tratamiento ya está finalizado");
        FichaMedica ficha = solicitud.idFichaSeguimiento() == null ? null : repositorioFichas
                .buscarPorIdYProfesional(solicitud.idFichaSeguimiento(), idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica de la sesión no encontrada"));
        FichaPaciente completada = ficha == null ? null : construirFicha(ficha, solicitud.respuestasFichaSeguimiento());
        if (completada != null) {
            tratamiento.getPaciente().asignarFicha(completada);
            repositorioFichasPaciente.saveAndFlush(completada);
        }
        tratamiento.agregarSesion(new SesionTratamiento(solicitud.observaciones().trim(), ficha, completada));
        return convertir(repositorio.save(tratamiento));
    }

    private Paciente buscarPaciente(Long idProfesional, Long idPaciente) {
        return repositorioPacientes.findByIdAndIdProfesional(idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Paciente no encontrado"));
    }

    private FichaPaciente construirFicha(FichaMedica ficha, List<SolicitudTratamiento.SolicitudRespuesta> solicitudes) {
        if (solicitudes == null || solicitudes.isEmpty()) throw new ExcepcionReglaNegocio("Debe completar la ficha de la sesión seleccionada");
        Map<Long, SolicitudTratamiento.SolicitudRespuesta> respuestas = solicitudes.stream().collect(Collectors.toMap(
                SolicitudTratamiento.SolicitudRespuesta::idOpcion, Function.identity(),
                (a,b) -> { throw new ExcepcionReglaNegocio("No se puede responder dos veces la misma opción"); }));
        List<OpcionCampo> opciones = ficha.getDetalles().stream().flatMap(d -> d.getCampos().stream()).flatMap(c -> c.getOpciones().stream()).toList();
        if (!opciones.stream().map(OpcionCampo::getId).collect(Collectors.toSet()).equals(respuestas.keySet()))
            throw new ExcepcionReglaNegocio("Deben completarse todas las opciones de la ficha de la sesión");
        ficha.getDetalles().stream().flatMap(d -> d.getCampos().stream()).forEach(c -> validarCampo(c, respuestas));
        FichaPaciente completada = new FichaPaciente(ficha, OrigenFichaPaciente.TRATAMIENTO);
        opciones.forEach(opcion -> {
            var respuesta = respuestas.get(opcion.getId());
            String valor = opcion.getTipo() == TipoOpcion.ENTRADA && (respuesta.valor() == null || respuesta.valor().isBlank())
                    ? "No aplica" : (respuesta.valor() == null || respuesta.valor().isBlank() ? null : respuesta.valor().trim());
            completada.agregarRespuesta(new RespuestaCampo(opcion, valor, respuesta.seleccionada()));
        });
        return completada;
    }

    private void validarCampo(CampoParaLlenar campo, Map<Long, SolicitudTratamiento.SolicitudRespuesta> respuestas) {
        List<OpcionCampo> seleccionables = campo.getOpciones().stream().filter(o -> o.getTipo() == TipoOpcion.SELECCION).toList();
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
        seleccionables.stream().filter(o -> o.getGrupoExclusion() != null).collect(Collectors.groupingBy(OpcionCampo::getGrupoExclusion))
                .forEach((grupo, opciones) -> { if (opciones.stream().filter(o -> Boolean.TRUE.equals(respuestas.get(o.getId()).seleccionada())).count() > 1)
                    throw new ExcepcionReglaNegocio("Las opciones del grupo " + grupo + " son excluyentes"); });
    }

    private RespuestaTratamiento convertir(Tratamiento tratamiento) {
        return new RespuestaTratamiento(tratamiento.getId(), tratamiento.getPaciente().getId(), tratamiento.getNombre(),
                tratamiento.getDescripcion(), tratamiento.getCantidadSesionesTotal(), tratamiento.getCantidadSesionesFaltantes(),
                tratamiento.getFechaCreacion(), tratamiento.getSesiones().stream().map(s -> new RespuestaTratamiento.RespuestaSesion(
                    s.getId(), s.getNroSesion(), s.getObservaciones(), s.getFechaHora(),
                    s.getFichaSeguimiento() == null ? null : s.getFichaSeguimiento().getId(),
                    s.getFichaSeguimiento() == null ? null : s.getFichaSeguimiento().getNombre())).toList());
    }
}
