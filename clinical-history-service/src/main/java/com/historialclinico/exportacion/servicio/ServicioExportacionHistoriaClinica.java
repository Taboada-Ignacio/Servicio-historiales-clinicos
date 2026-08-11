package com.historialclinico.exportacion.servicio;

import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.servicio.ServicioAuditLog;
import com.historialclinico.auditoria.servicio.ServicioCifradoAuditoria;
import com.historialclinico.exportacion.dto.ArchivoHistoriaClinica;
import com.historialclinico.exportacion.dto.PacienteExportable;
import com.historialclinico.exportacion.dto.RespuestaExportacionHistoriaClinica;
import com.historialclinico.exportacion.dto.SolicitudExportacionHistoriaClinica;
import com.historialclinico.exportacion.exportador.HistoriaClinicaExporter;
import com.historialclinico.exportacion.modelo.ExportacionHistoriaClinica;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.modelo.FormatoArchivoFinal;
import com.historialclinico.exportacion.modelo.TipoExportacion;
import com.historialclinico.exportacion.repositorio.RepositorioExportacionHistoriaClinica;
import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.seguridad.ProveedorIdentidadProfesional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicioExportacionHistoriaClinica {
    private static final DateTimeFormatter FECHA_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));
    private final ProveedorIdentidadProfesional proveedorIdentidad;
    private final RepositorioPaciente repositorioPacientes;
    private final RepositorioExportacionHistoriaClinica repositorioExportaciones;
    private final ConstructorHistoriaClinica constructorHistoria;
    private final ServicioAdjuntosExportacion servicioAdjuntos;
    private final ServicioCifradoAuditoria integridad;
    private final ServicioAuditLog auditLog;
    private final Map<FormatoExportacion, HistoriaClinicaExporter> exportadores;

    public ServicioExportacionHistoriaClinica(ProveedorIdentidadProfesional proveedorIdentidad,
            RepositorioPaciente repositorioPacientes,
            RepositorioExportacionHistoriaClinica repositorioExportaciones,
            ConstructorHistoriaClinica constructorHistoria, ServicioAdjuntosExportacion servicioAdjuntos,
            ServicioCifradoAuditoria integridad, ServicioAuditLog auditLog,
            List<HistoriaClinicaExporter> exportadores) {
        this.proveedorIdentidad = proveedorIdentidad;
        this.repositorioPacientes = repositorioPacientes;
        this.repositorioExportaciones = repositorioExportaciones;
        this.constructorHistoria = constructorHistoria;
        this.servicioAdjuntos = servicioAdjuntos;
        this.integridad = integridad;
        this.auditLog = auditLog;
        this.exportadores = new EnumMap<>(FormatoExportacion.class);
        exportadores.forEach(exportador -> this.exportadores.put(exportador.formato(), exportador));
        if (this.exportadores.size() != FormatoExportacion.values().length)
            throw new IllegalStateException("Debe existir un exportador por cada formato soportado");
    }

    @Transactional
    public ArchivoHistoriaClinica exportar(Long pacienteId, SolicitudExportacionHistoriaClinica solicitud) {
        var profesional = proveedorIdentidad.requerir();
        ArchivoHistoriaClinica archivo = null;
        try {
            var paciente = repositorioPacientes.findByIdAndIdProfesional(pacienteId, profesional.id())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
            Instant fecha = Instant.now();
            List<AdjuntoExportable> adjuntos = servicioAdjuntos.buscarActivos(pacienteId, profesional.id());
            var referencias = adjuntos.stream().map(AdjuntoExportable::referencia).toList();
            byte[] documentoPrincipal = exportadores.get(solicitud.formato())
                    .exportar(constructorHistoria.construir(paciente, fecha, referencias));
            FormatoArchivoFinal formatoFinal;
            String hash;
            String nombre;
            if (solicitud.tipoExportacion() == TipoExportacion.HISTORIA_CLINICA_CON_ADJUNTOS) {
                PaqueteExportacion paquete = servicioAdjuntos.generarZip(pacienteId, solicitud.formato(),
                        documentoPrincipal, fecha, adjuntos);
                formatoFinal = FormatoArchivoFinal.ZIP;
                nombre = nombreArchivo(paciente.getApellido(), paciente.getNombre(), fecha, formatoFinal, true);
                hash = paquete.integridad();
                archivo = ArchivoHistoriaClinica.temporal(paquete.archivo(), paquete.longitud(),
                        formatoFinal.getTipoContenido(), nombre);
            } else {
                formatoFinal = FormatoArchivoFinal.desde(solicitud.formato());
                nombre = nombreArchivo(paciente.getApellido(), paciente.getNombre(), fecha, formatoFinal, false);
                hash = integridad.hash(documentoPrincipal);
                archivo = ArchivoHistoriaClinica.enMemoria(documentoPrincipal, formatoFinal.getTipoContenido(), nombre);
            }
            String detalle = solicitud.detalleMotivo() == null || solicitud.detalleMotivo().isBlank()
                    ? null : solicitud.detalleMotivo().trim();
            repositorioExportaciones.saveAndFlush(new ExportacionHistoriaClinica(pacienteId, profesional.id(),
                    solicitud.motivo(), detalle, solicitud.tipoExportacion(), solicitud.formato(), formatoFinal,
                    fecha, nombre, hash));
            auditLog.registrarExportacion(profesional.id(), pacienteId, solicitud.formato(), ResultadoAuditLog.SUCCESS);
            return archivo;
        } catch (RuntimeException ex) {
            if (archivo != null) archivo.limpiar();
            intentarAuditarFallo(profesional.id(), pacienteId, solicitud.formato());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PacienteExportable> buscarPacientesPropios() {
        Long profesionalId = proveedorIdentidad.requerir().id();
        return repositorioPacientes.findAllByIdProfesionalOrderByApellidoAscNombreAsc(profesionalId).stream()
                .map(paciente -> new PacienteExportable(paciente.getId(), paciente.getNombre(),
                        paciente.getApellido(), paciente.getDni()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RespuestaExportacionHistoriaClinica> listarExportaciones(Long pacienteId) {
        Long profesionalId = proveedorIdentidad.requerir().id();
        validarAccesoPaciente(pacienteId, profesionalId);
        return repositorioExportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(profesionalId, pacienteId)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public RespuestaExportacionHistoriaClinica obtenerExportacion(Long pacienteId, Long exportacionId) {
        Long profesionalId = proveedorIdentidad.requerir().id();
        validarAccesoPaciente(pacienteId, profesionalId);
        return repositorioExportaciones.findByIdAndProfesionalIdAndPacienteId(
                        exportacionId, profesionalId, pacienteId)
                .map(this::respuesta)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Exportación de historia clínica no encontrada"));
    }

    private void validarAccesoPaciente(Long pacienteId, Long profesionalId) {
        repositorioPacientes.findByIdAndIdProfesional(pacienteId, profesionalId)
                .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
    }

    private RespuestaExportacionHistoriaClinica respuesta(ExportacionHistoriaClinica exportacion) {
        return new RespuestaExportacionHistoriaClinica(exportacion.getId(),
                exportacion.getMotivo(), exportacion.getDetalleMotivo(),
                exportacion.getFormato(), exportacion.getTipoExportacion(), exportacion.getFormatoHistoriaClinica(),
                exportacion.getFormatoArchivoFinal(), exportacion.getFechaHoraExportacion(),
                exportacion.getNombreArchivo());
    }

    private void intentarAuditarFallo(Long profesionalId, Long pacienteId, FormatoExportacion formato) {
        try { auditLog.registrarExportacion(profesionalId, pacienteId, formato, ResultadoAuditLog.FAILED); }
        catch (RuntimeException ignored) {
            // La excepción original conserva la causa funcional; el sistema no registra contenido clínico en logs.
        }
    }

    private String nombreArchivo(String apellido, String nombre, Instant fecha, FormatoArchivoFinal formato,
            boolean completa) {
        String base = Normalizer.normalize(apellido + "-" + nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "paciente";
        return "historia-clinica-" + (completa ? "completa-" : "") + base + "-"
                + FECHA_ARCHIVO.format(fecha) + "." + formato.getExtension();
    }
}
