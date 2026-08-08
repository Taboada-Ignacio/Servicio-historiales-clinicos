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
import com.historialclinico.tratamiento.repositorio.RepositorioSesionTratamiento;
import com.historialclinico.auditoria.dto.InformeAuditoriaClinica;
import com.historialclinico.auditoria.dto.RespuestaAuditoriaRectificacion;
import com.historialclinico.auditoria.modelo.TipoMotivoRectificacion;
import com.historialclinico.auditoria.modelo.TipoRegistroClinico;
import com.historialclinico.auditoria.servicio.ServicioAuditoriaClinica;
import com.historialclinico.compartido.dto.RespuestaFichaClinica;
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
    private final RepositorioSesionTratamiento repositorioSesiones;
    private final ServicioAuditoriaClinica auditoria;
    public ServicioTratamiento(RepositorioTratamiento repositorio, RepositorioPaciente repositorioPacientes,
            RepositorioFichaMedica repositorioFichas, RepositorioFichaPaciente repositorioFichasPaciente,
            RepositorioSesionTratamiento repositorioSesiones, ServicioAuditoriaClinica auditoria) {
        this.repositorio = repositorio; this.repositorioPacientes = repositorioPacientes;
        this.repositorioFichas = repositorioFichas; this.repositorioFichasPaciente = repositorioFichasPaciente;
        this.repositorioSesiones = repositorioSesiones; this.auditoria = auditoria;
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
            tratamiento.agregarSesion(new SesionTratamiento(normalizarObservaciones(solicitudSesion.observaciones()), ficha, completada));
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
        tratamiento.agregarSesion(new SesionTratamiento(normalizarObservaciones(solicitud.observaciones()), ficha, completada));
        return convertir(repositorio.save(tratamiento));
    }

    @Transactional
    public RespuestaTratamiento rectificarTratamiento(Long idProfesional, Long idPaciente, Long idTratamiento,
            SolicitudRectificacionTratamiento solicitud) {
        Tratamiento tratamiento = repositorio.buscarParaRectificar(idTratamiento, idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Tratamiento no encontrado"));
        validarVersion(tratamiento.getVersionClinica(), solicitud.rectificacion().versionEsperada());
        Object antes = snapshotTratamiento(tratamiento);
        boolean anular = solicitud.rectificacion().tipoMotivo() == TipoMotivoRectificacion.ANULACION_CARGA_ERRONEA;
        int versionAnterior = tratamiento.getVersionClinica();
        try {
            tratamiento.rectificar(anular ? tratamiento.getNombre() : solicitud.nombre().trim(),
                    anular ? tratamiento.getDescripcion() : normalizar(solicitud.descripcion()),
                    anular ? tratamiento.getCantidadSesionesTotal() : solicitud.cantidadSesionesTotal(), anular);
        } catch (IllegalArgumentException ex) { throw new ExcepcionReglaNegocio(ex.getMessage()); }
        repositorio.saveAndFlush(tratamiento);
        auditoria.registrar(TipoRegistroClinico.TRATAMIENTO, tratamiento.getId(), idPaciente, idProfesional,
                versionAnterior, tratamiento.getVersionClinica(), solicitud.rectificacion(), antes,
                snapshotTratamiento(tratamiento));
        return convertir(tratamiento);
    }

    @Transactional
    public RespuestaTratamiento.RespuestaSesion rectificarSesion(Long idProfesional, Long idPaciente,
            Long idTratamiento, Long idSesion, SolicitudRectificacionSesion solicitud) {
        SesionTratamiento sesion = repositorioSesiones.buscarParaRectificar(idSesion, idTratamiento, idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Sesión no encontrada"));
        validarVersion(sesion.getVersionClinica(), solicitud.rectificacion().versionEsperada());
        Object antes = snapshotSesion(sesion);
        boolean anular = solicitud.rectificacion().tipoMotivo() == TipoMotivoRectificacion.ANULACION_CARGA_ERRONEA;
        FichaMedica ficha = sesion.getFichaSeguimiento(); FichaPaciente completada = sesion.getFichaPacienteSeguimiento();
        String observaciones = sesion.getObservaciones();
        if (!anular) {
            observaciones = normalizarObservaciones(solicitud.observaciones());
            boolean conservarFicha = ficha != null && ficha.getId().equals(solicitud.idFichaSeguimiento())
                    && solicitud.respuestasFichaSeguimiento() == null;
            if (!conservarFicha) {
                ficha = solicitud.idFichaSeguimiento() == null ? null : repositorioFichas
                        .buscarPorIdYProfesional(solicitud.idFichaSeguimiento(), idProfesional)
                        .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica de la sesión no encontrada"));
                completada = ficha == null ? null : construirFicha(ficha, solicitud.respuestasFichaSeguimiento());
                if (completada != null) {
                    sesion.getTratamiento().getPaciente().asignarFicha(completada);
                    repositorioFichasPaciente.save(completada);
                }
            }
        }
        int versionAnterior = sesion.getVersionClinica();
        sesion.rectificar(observaciones, ficha, completada, anular);
        repositorioSesiones.saveAndFlush(sesion);
        auditoria.registrar(TipoRegistroClinico.SESION, sesion.getId(), idPaciente, idProfesional,
                versionAnterior, sesion.getVersionClinica(), solicitud.rectificacion(), antes, snapshotSesion(sesion));
        return convertirSesion(sesion);
    }

    public List<RespuestaAuditoriaRectificacion> auditoria(Long idProfesional, Long idPaciente,
            TipoRegistroClinico tipo, Long idTratamiento, Long idRegistro) {
        verificarRegistro(idProfesional, idPaciente, tipo, idTratamiento, idRegistro);
        return auditoria.listar(idProfesional, idPaciente, tipo, idRegistro);
    }

    public InformeAuditoriaClinica informeAuditoria(Long idProfesional, Long idPaciente,
            TipoRegistroClinico tipo, Long idTratamiento, Long idRegistro) {
        verificarRegistro(idProfesional, idPaciente, tipo, idTratamiento, idRegistro);
        return auditoria.informe(idProfesional, idPaciente, tipo, idRegistro);
    }

    private void verificarRegistro(Long idProfesional, Long idPaciente, TipoRegistroClinico tipo,
            Long idTratamiento, Long idRegistro) {
        boolean existe = tipo == TipoRegistroClinico.TRATAMIENTO
                ? repositorio.buscarParaRectificar(idRegistro, idPaciente, idProfesional).isPresent()
                : repositorioSesiones.buscarParaRectificar(idRegistro, idTratamiento, idPaciente, idProfesional).isPresent();
        if (!existe) throw new ExcepcionRecursoNoEncontrado("Registro clínico no encontrado");
    }

    private void validarVersion(int actual, int esperada) {
        if (actual != esperada)
            throw new ExcepcionReglaNegocio("El registro fue modificado desde otra sesión; recargue la historia clínica");
    }

    private String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private Object snapshotTratamiento(Tratamiento t) {
        Map<String, Object> datos = new LinkedHashMap<>(); datos.put("id", t.getId());
        datos.put("idPaciente", t.getPaciente().getId()); datos.put("fechaClinicaOriginal", t.getFechaCreacion());
        datos.put("nombre", t.getNombre()); datos.put("descripcion", t.getDescripcion());
        datos.put("cantidadSesionesTotal", t.getCantidadSesionesTotal());
        datos.put("cantidadSesionesFaltantes", t.getCantidadSesionesFaltantes());
        datos.put("versionClinica", t.getVersionClinica()); datos.put("estadoRegistro", t.getEstadoRegistro()); return datos;
    }

    private Object snapshotSesion(SesionTratamiento s) {
        Map<String, Object> datos = new LinkedHashMap<>(); datos.put("id", s.getId());
        datos.put("idTratamiento", s.getTratamiento().getId()); datos.put("idPaciente", s.getTratamiento().getPaciente().getId());
        datos.put("numeroSesion", s.getNroSesion()); datos.put("fechaClinicaOriginal", s.getFechaHora());
        datos.put("observaciones", s.getObservaciones()); datos.put("versionClinica", s.getVersionClinica());
        datos.put("estadoRegistro", s.getEstadoRegistro());
        datos.put("idFichaSeguimiento", s.getFichaSeguimiento() == null ? null : s.getFichaSeguimiento().getId());
        datos.put("fichaCompletada", snapshotFicha(s.getFichaPacienteSeguimiento())); return datos;
    }

    private Object snapshotFicha(FichaPaciente ficha) {
        if (ficha == null) return null;
        Map<String, Object> datos = new LinkedHashMap<>(); datos.put("id", ficha.getId());
        datos.put("idPlantilla", ficha.getFichaMedica().getId()); datos.put("nombrePlantilla", ficha.getFichaMedica().getNombre());
        datos.put("respuestas", ficha.getRespuestas().stream().map(r -> {
            Map<String, Object> respuesta = new LinkedHashMap<>(); respuesta.put("idOpcion", r.getOpcion().getId());
            respuesta.put("valor", r.getValor()); respuesta.put("seleccionada", r.getSeleccionada()); return respuesta;
        }).toList()); return datos;
    }

    private String normalizarObservaciones(String observaciones) {
        return observaciones == null || observaciones.isBlank() ? "Sin observaciones" : observaciones.trim();
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
                tratamiento.getFechaCreacion(), tratamiento.getVersionClinica(), tratamiento.getEstadoRegistro(),
                tratamiento.getFechaUltimaRectificacion(), tratamiento.getSesiones().stream().map(this::convertirSesion).toList());
    }

    private RespuestaTratamiento.RespuestaSesion convertirSesion(SesionTratamiento s) {
        return new RespuestaTratamiento.RespuestaSesion(s.getId(), s.getNroSesion(), s.getObservaciones(), s.getFechaHora(),
                s.getFichaSeguimiento() == null ? null : s.getFichaSeguimiento().getId(),
                s.getFichaSeguimiento() == null ? null : s.getFichaSeguimiento().getNombre(), convertirFicha(s.getFichaPacienteSeguimiento()),
                s.getVersionClinica(), s.getEstadoRegistro(), s.getFechaUltimaRectificacion());
    }

    private RespuestaFichaClinica convertirFicha(FichaPaciente ficha) {
        if (ficha == null) return null;
        return new RespuestaFichaClinica(ficha.getFichaMedica().getId(), ficha.getFichaMedica().getNombre(),
                ficha.getRespuestas().stream().map(r -> new RespuestaFichaClinica.Respuesta(
                        r.getOpcion().getId(), r.getValor(), r.getSeleccionada())).toList());
    }
}
