package com.historialclinico.paciente.servicio;

import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import com.historialclinico.paciente.dto.RespuestaPaciente;
import com.historialclinico.paciente.dto.SolicitudPaciente;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.modelo.FichaPaciente;
import com.historialclinico.paciente.modelo.RespuestaCampo;
import com.historialclinico.paciente.modelo.OrigenFichaPaciente;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.fichamedica.modelo.CampoParaLlenar;
import com.historialclinico.fichamedica.modelo.FichaMedica;
import com.historialclinico.fichamedica.modelo.OpcionCampo;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
import com.historialclinico.fichamedica.repositorio.RepositorioFichaMedica;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ServicioPaciente {

    private final RepositorioPaciente repositorio;
    private final RepositorioFichaMedica repositorioFichas;

    public ServicioPaciente(RepositorioPaciente repositorio, RepositorioFichaMedica repositorioFichas) {
        this.repositorio = repositorio;
        this.repositorioFichas = repositorioFichas;
    }

    @Transactional
    public RespuestaPaciente crear(Long idProfesional, SolicitudPaciente solicitud) {
        String dni = solicitud.dni().trim();
        validarDniDisponible(idProfesional, dni, null);
        Paciente paciente = new Paciente(idProfesional, normalizarRequerido(solicitud.nombre()),
                normalizarRequerido(solicitud.apellido()), dni, normalizarOpcional(solicitud.telefono()),
                solicitud.fechaNacimiento(), solicitud.sexo());
        List<SolicitudPaciente.SolicitudFichaPaciente> fichas = solicitud.fichas() == null ? List.of() : solicitud.fichas();
        Set<Long> idsFichas = fichas.stream().map(SolicitudPaciente.SolicitudFichaPaciente::idFichaMedica)
                .collect(Collectors.toSet());
        if (idsFichas.size() != fichas.size()) {
            throw new ExcepcionReglaNegocio("No se puede asignar dos veces la misma ficha médica");
        }
        fichas.forEach(ficha -> paciente.asignarFicha(construirFichaAsignada(idProfesional, ficha)));
        return convertir(repositorio.save(paciente));
    }

    public List<RespuestaPaciente> buscarTodos(Long idProfesional) {
        return repositorio.findAllByIdProfesionalOrderByApellidoAscNombreAsc(idProfesional).stream()
                .map(this::convertir)
                .toList();
    }

    public RespuestaPaciente buscarPorId(Long idProfesional, Long idPaciente) {
        return convertir(buscarPacienteDelProfesional(idProfesional, idPaciente));
    }

    @Transactional
    public RespuestaPaciente actualizar(Long idProfesional, Long idPaciente, SolicitudPaciente solicitud) {
        Paciente paciente = buscarPacienteDelProfesional(idProfesional, idPaciente);
        String dni = solicitud.dni().trim();
        validarDniDisponible(idProfesional, dni, idPaciente);
        paciente.actualizar(normalizarRequerido(solicitud.nombre()), normalizarRequerido(solicitud.apellido()),
                dni, normalizarOpcional(solicitud.telefono()), solicitud.fechaNacimiento(), solicitud.sexo());
        if (solicitud.fichas() != null) actualizarFichasAsignadas(paciente, solicitud.fichas());
        return convertir(repositorio.save(paciente));
    }

    private void actualizarFichasAsignadas(Paciente paciente,
            List<SolicitudPaciente.SolicitudFichaPaciente> solicitudes) {
        Map<Long, FichaPaciente> existentes = paciente.getFichasAsignadas().stream()
                .filter(ficha -> ficha.getOrigen() == OrigenFichaPaciente.DIRECTA)
                .collect(Collectors.toMap(FichaPaciente::getId, Function.identity()));
        Set<Long> idsRecibidos = solicitudes.stream().map(SolicitudPaciente.SolicitudFichaPaciente::idFichaPaciente)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        long cantidadExistentesRecibidas = solicitudes.stream().filter(s -> s.idFichaPaciente() != null).count();
        if (idsRecibidos.size() != cantidadExistentesRecibidas || !existentes.keySet().containsAll(idsRecibidos))
            throw new ExcepcionReglaNegocio("Una de las fichas informadas no pertenece al paciente o está repetida");

        paciente.getFichasAsignadas().removeIf(ficha -> ficha.getOrigen() == OrigenFichaPaciente.DIRECTA
                && !idsRecibidos.contains(ficha.getId()));
        solicitudes.stream().filter(s -> s.idFichaPaciente() != null).forEach(solicitud -> {
            FichaPaciente fichaPaciente = existentes.get(solicitud.idFichaPaciente());
            if (!fichaPaciente.getIdPlantillaOrigen().equals(solicitud.idFichaMedica()))
                throw new ExcepcionReglaNegocio("La ficha médica informada no corresponde a la ficha del paciente");
            FichaMedica plantilla = repositorioFichas.buscarPorIdYProfesional(
                    solicitud.idFichaMedica(), paciente.getIdProfesional())
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica no encontrada"));
            Map<Long, SolicitudPaciente.SolicitudRespuesta> respuestas = mapearYValidarRespuestas(
                    plantilla, solicitud.respuestas());
            fichaPaciente.getRespuestas().forEach(respuesta -> {
                var nueva = respuestas.get(respuesta.getIdOpcionOrigen());
                String valor = respuesta.getTipoOpcion() == TipoOpcion.ENTRADA
                        && (nueva.valor() == null || nueva.valor().isBlank()) ? "No aplica" : normalizarOpcional(nueva.valor());
                respuesta.actualizar(valor, nueva.seleccionada());
            });
        });
        solicitudes.stream().filter(s -> s.idFichaPaciente() == null)
                .forEach(solicitud -> paciente.asignarFicha(construirFichaAsignada(paciente.getIdProfesional(), solicitud)));
    }

    @Transactional
    public void eliminar(Long idProfesional, Long idPaciente) {
        buscarPacienteDelProfesional(idProfesional, idPaciente);
        throw new ExcepcionReglaNegocio("El borrado físico de pacientes está deshabilitado para preservar su historia clínica");
    }

    private Paciente buscarPacienteDelProfesional(Long idProfesional, Long idPaciente) {
        return repositorio.findByIdAndIdProfesional(idPaciente, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Paciente no encontrado"));
    }

    private void validarDniDisponible(Long idProfesional, String dni, Long idPaciente) {
        boolean existe = idPaciente == null
                ? repositorio.existsByIdProfesionalAndDni(idProfesional, dni)
                : repositorio.existsByIdProfesionalAndDniAndIdNot(idProfesional, dni, idPaciente);
        if (existe) {
            throw new ExcepcionReglaNegocio("Ya existe un paciente con ese DNI para el profesional");
        }
    }

    private String normalizarRequerido(String valor) {
        return valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private FichaPaciente construirFichaAsignada(Long idProfesional, SolicitudPaciente.SolicitudFichaPaciente solicitud) {
        FichaMedica ficha = repositorioFichas.buscarPorIdYProfesional(solicitud.idFichaMedica(), idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica no encontrada"));
        Map<Long, SolicitudPaciente.SolicitudRespuesta> respuestas = mapearYValidarRespuestas(ficha, solicitud.respuestas());
        List<OpcionCampo> opciones = ficha.getDetalles().stream()
                .flatMap(detalle -> detalle.getCampos().stream())
                .flatMap(campo -> campo.getOpciones().stream())
                .toList();

        FichaPaciente fichaPaciente = new FichaPaciente(ficha);
        opciones.forEach(opcion -> {
            var respuesta = respuestas.get(opcion.getId());
            String valor = opcion.getTipo() == TipoOpcion.ENTRADA
                    && (respuesta.valor() == null || respuesta.valor().isBlank())
                    ? "No aplica"
                    : normalizarOpcional(respuesta.valor());
            fichaPaciente.agregarRespuesta(new RespuestaCampo(opcion, valor, respuesta.seleccionada()));
        });
        return fichaPaciente;
    }

    private Map<Long, SolicitudPaciente.SolicitudRespuesta> mapearYValidarRespuestas(FichaMedica ficha,
            List<SolicitudPaciente.SolicitudRespuesta> solicitudes) {
        Map<Long, SolicitudPaciente.SolicitudRespuesta> respuestas = solicitudes.stream()
                .collect(Collectors.toMap(SolicitudPaciente.SolicitudRespuesta::idOpcion, Function.identity(),
                        (primera, repetida) -> { throw new ExcepcionReglaNegocio("No se puede responder dos veces la misma opción"); }));
        List<OpcionCampo> opciones = ficha.getDetalles().stream()
                .flatMap(detalle -> detalle.getCampos().stream())
                .flatMap(campo -> campo.getOpciones().stream())
                .toList();
        Set<Long> idsValidos = opciones.stream().map(OpcionCampo::getId).collect(Collectors.toSet());
        if (!idsValidos.equals(respuestas.keySet())) {
            throw new ExcepcionReglaNegocio("Deben completarse todas las opciones de la ficha seleccionada");
        }
        ficha.getDetalles().stream().flatMap(detalle -> detalle.getCampos().stream())
                .forEach(campo -> validarCampo(campo, respuestas));
        return respuestas;
    }

    private void validarCampo(CampoParaLlenar campo, Map<Long, SolicitudPaciente.SolicitudRespuesta> respuestas) {
        List<OpcionCampo> seleccionables = campo.getOpciones().stream()
                .filter(opcion -> opcion.getTipo() == TipoOpcion.SELECCION).toList();
        for (OpcionCampo opcion : campo.getOpciones()) {
            var respuesta = respuestas.get(opcion.getId());
            if (opcion.getTipo() == TipoOpcion.SI_NO
                    && !("SI".equals(respuesta.valor()) || "NO".equals(respuesta.valor()))) {
                throw new ExcepcionReglaNegocio("Debe responderse Sí o No en " + campo.getTitulo());
            }
            if (opcion.getTipo() == TipoOpcion.SELECCION && respuesta.seleccionada() == null) {
                throw new ExcepcionReglaNegocio("Debe indicarse la selección de todas las opciones");
            }
        }
        long cantidadSeleccionada = seleccionables.stream()
                .filter(opcion -> Boolean.TRUE.equals(respuestas.get(opcion.getId()).seleccionada())).count();
        if (!campo.isPermiteSeleccionMultiple() && cantidadSeleccionada > 1) {
            throw new ExcepcionReglaNegocio("El campo " + campo.getTitulo() + " admite una sola selección");
        }
        seleccionables.stream().filter(opcion -> opcion.getGrupoExclusion() != null)
                .collect(Collectors.groupingBy(OpcionCampo::getGrupoExclusion))
                .forEach((grupo, opciones) -> {
                    long seleccionadas = opciones.stream()
                            .filter(opcion -> Boolean.TRUE.equals(respuestas.get(opcion.getId()).seleccionada())).count();
                    if (seleccionadas > 1) throw new ExcepcionReglaNegocio("Las opciones del grupo " + grupo + " son excluyentes");
                });
    }

    private RespuestaPaciente convertir(Paciente paciente) {
        return new RespuestaPaciente(paciente.getId(), paciente.getIdProfesional(), paciente.getNombre(),
                paciente.getApellido(), paciente.getDni(), paciente.getTelefono(), paciente.getFechaNacimiento(),
                paciente.getSexo(), paciente.getFechaCreacion(), paciente.getFechaActualizacion(), paciente.getVersion(),
                paciente.getFichasAsignadas().stream()
                        .filter(ficha -> ficha.getOrigen() == OrigenFichaPaciente.DIRECTA)
                        .map(ficha -> new RespuestaPaciente.RespuestaFichaPaciente(
                        ficha.getId(), ficha.getIdPlantillaOrigen(), ficha.getNombreFicha(),
                        ficha.getFechaAsignacion(), ficha.getRespuestas().stream().map(respuesta ->
                                new RespuestaPaciente.RespuestaFicha(respuesta.getId(), respuesta.getIdOpcionOrigen(),
                                        respuesta.getTituloDetalle(), respuesta.getTituloCampo(),
                                        respuesta.getTituloOpcion(), respuesta.getTipoOpcion().name(),
                                        respuesta.getValor(), respuesta.getSeleccionada())).toList())).toList());
    }
}
