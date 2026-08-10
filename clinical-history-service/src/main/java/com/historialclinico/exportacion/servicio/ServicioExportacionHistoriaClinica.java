package com.historialclinico.exportacion.servicio;

import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.servicio.ServicioAuditLog;
import com.historialclinico.exportacion.dto.ArchivoHistoriaClinica;
import com.historialclinico.exportacion.dto.PacienteExportable;
import com.historialclinico.exportacion.dto.SolicitudExportacionHistoriaClinica;
import com.historialclinico.exportacion.exportador.HistoriaClinicaExporter;
import com.historialclinico.exportacion.modelo.ExportacionHistoriaClinica;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.repositorio.RepositorioExportacionHistoriaClinica;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.seguridad.ProveedorIdentidadProfesional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HexFormat;
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
    private final ServicioAuditLog auditLog;
    private final Map<FormatoExportacion, HistoriaClinicaExporter> exportadores;

    public ServicioExportacionHistoriaClinica(ProveedorIdentidadProfesional proveedorIdentidad,
            RepositorioPaciente repositorioPacientes,
            RepositorioExportacionHistoriaClinica repositorioExportaciones,
            ConstructorHistoriaClinica constructorHistoria, ServicioAuditLog auditLog,
            List<HistoriaClinicaExporter> exportadores) {
        this.proveedorIdentidad = proveedorIdentidad;
        this.repositorioPacientes = repositorioPacientes;
        this.repositorioExportaciones = repositorioExportaciones;
        this.constructorHistoria = constructorHistoria;
        this.auditLog = auditLog;
        this.exportadores = new EnumMap<>(FormatoExportacion.class);
        exportadores.forEach(exportador -> this.exportadores.put(exportador.formato(), exportador));
        if (this.exportadores.size() != FormatoExportacion.values().length)
            throw new IllegalStateException("Debe existir un exportador por cada formato soportado");
    }

    @Transactional
    public ArchivoHistoriaClinica exportar(Long pacienteId, SolicitudExportacionHistoriaClinica solicitud) {
        var profesional = proveedorIdentidad.requerir();
        try {
            var paciente = repositorioPacientes.findByIdAndIdProfesional(pacienteId, profesional.id())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
            Instant fecha = Instant.now();
            byte[] contenido = exportadores.get(solicitud.formato())
                    .exportar(constructorHistoria.construir(paciente, fecha));
            String nombre = nombreArchivo(paciente.getApellido(), paciente.getNombre(), fecha, solicitud.formato());
            String hash = sha256(contenido);
            String detalle = solicitud.detalleMotivo() == null || solicitud.detalleMotivo().isBlank()
                    ? null : solicitud.detalleMotivo().trim();
            repositorioExportaciones.saveAndFlush(new ExportacionHistoriaClinica(pacienteId, profesional.id(),
                    solicitud.motivo(), detalle, solicitud.formato(), fecha, nombre, hash));
            auditLog.registrarExportacion(profesional.id(), pacienteId, solicitud.formato(), ResultadoAuditLog.SUCCESS);
            return new ArchivoHistoriaClinica(contenido, solicitud.formato().getTipoContenido(), nombre);
        } catch (RuntimeException ex) {
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

    private void intentarAuditarFallo(Long profesionalId, Long pacienteId, FormatoExportacion formato) {
        try { auditLog.registrarExportacion(profesionalId, pacienteId, formato, ResultadoAuditLog.FAILED); }
        catch (RuntimeException ignored) {
            // La excepción original conserva la causa funcional; el sistema no registra contenido clínico en logs.
        }
    }

    private String nombreArchivo(String apellido, String nombre, Instant fecha, FormatoExportacion formato) {
        String base = Normalizer.normalize(apellido + "-" + nombre, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "paciente";
        return "historia-clinica-" + base + "-" + FECHA_ARCHIVO.format(fecha) + "." + formato.getExtension();
    }

    private String sha256(byte[] contenido) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 no está disponible", ex); }
    }
}
