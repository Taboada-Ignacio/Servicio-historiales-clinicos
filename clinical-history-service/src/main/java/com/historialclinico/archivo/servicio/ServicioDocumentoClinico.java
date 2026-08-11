package com.historialclinico.archivo.servicio;

import com.historialclinico.archivo.dto.*;
import com.historialclinico.archivo.malware.FileMalwareScanner;
import com.historialclinico.archivo.modelo.*;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinico;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinicoVersion;
import com.historialclinico.archivo.storage.ClinicalFileStorage;
import com.historialclinico.auditoria.modelo.AuditLog;
import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.servicio.ServicioAuditLog;
import com.historialclinico.auditoria.servicio.ServicioCifradoAuditoria;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.excepcion.ExcepcionArchivoInvalido;
import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.seguridad.IdentidadProfesional;
import com.historialclinico.seguridad.ProveedorIdentidadProfesional;
import com.historialclinico.tratamiento.repositorio.RepositorioSesionTratamiento;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ServicioDocumentoClinico {
    private static final Logger LOG = LoggerFactory.getLogger(ServicioDocumentoClinico.class);
    private static final String WARNING_CUOTA = "El paciente supera el almacenamiento recomendado de 500 MB.";
    private static final String ERROR_DUPLICADO = "Este archivo ya fue cargado para el paciente.";

    private final ProveedorIdentidadProfesional proveedorIdentidad;
    private final RepositorioPaciente pacientes;
    private final RepositorioTratamiento tratamientos;
    private final RepositorioSesionTratamiento sesiones;
    private final RepositorioEpicrisis epicrisis;
    private final RepositorioDocumentoClinico documentos;
    private final RepositorioDocumentoClinicoVersion versiones;
    private final ValidadorArchivoClinico validador;
    private final GeneradorVistaPreviaDocx generadorVistaPreviaDocx;
    private final FileMalwareScanner malwareScanner;
    private final ServicioCifradoAuditoria integridad;
    private final ClinicalFileStorage storage;
    private final ServicioAuditLog auditLog;
    private final long cuotaRecomendada;
    private final int retencionAnios;

    public ServicioDocumentoClinico(ProveedorIdentidadProfesional proveedorIdentidad,
            RepositorioPaciente pacientes, RepositorioTratamiento tratamientos,
            RepositorioSesionTratamiento sesiones, RepositorioEpicrisis epicrisis,
            RepositorioDocumentoClinico documentos, RepositorioDocumentoClinicoVersion versiones,
            ValidadorArchivoClinico validador, GeneradorVistaPreviaDocx generadorVistaPreviaDocx,
            FileMalwareScanner malwareScanner,
            ServicioCifradoAuditoria integridad, ClinicalFileStorage storage, ServicioAuditLog auditLog,
            @Value("${app.archivos.cuota-recomendada-bytes:524288000}") long cuotaRecomendada,
            @Value("${app.archivos.retencion-anios-minima:10}") int retencionAnios) {
        if (cuotaRecomendada <= 0) throw new IllegalStateException("La cuota recomendada debe ser positiva");
        if (retencionAnios < 10)
            throw new IllegalStateException("La retención de archivos clínicos no puede configurarse por debajo de 10 años");
        this.proveedorIdentidad = proveedorIdentidad;
        this.pacientes = pacientes;
        this.tratamientos = tratamientos;
        this.sesiones = sesiones;
        this.epicrisis = epicrisis;
        this.documentos = documentos;
        this.versiones = versiones;
        this.validador = validador;
        this.generadorVistaPreviaDocx = generadorVistaPreviaDocx;
        this.malwareScanner = malwareScanner;
        this.integridad = integridad;
        this.storage = storage;
        this.auditLog = auditLog;
        this.cuotaRecomendada = cuotaRecomendada;
        this.retencionAnios = retencionAnios;
    }

    @Transactional
    public RespuestaDocumentoClinico crear(ContextoDocumentoClinico contexto, Long contextoId,
            CategoriaDocumentoClinico categoria, String descripcion, MultipartFile archivo) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        UUID documentoId = UUID.randomUUID();
        ContextoResuelto resuelto = resolverContextoBloqueado(profesional.id(), contexto, contextoId);
        try {
            ArchivoClinicoValidado validado = validador.validar(archivo);
            malwareScanner.analizar(validado.contenido());
            String hash = integridad.hash(validado.contenido());
            boolean duplicado = versiones.existeIntegridadEnPaciente(resuelto.paciente().getId(), hash);
            if (duplicado) throw new ExcepcionReglaNegocio(ERROR_DUPLICADO);
            long total = versiones.sumarBytesDelPaciente(resuelto.paciente().getId());
            String warningCuota = total + validado.sizeBytes() >= cuotaRecomendada ? WARNING_CUOTA : null;

            UUID versionId = UUID.randomUUID();
            String storageKey = storageKey(resuelto.paciente().getId(), documentoId, versionId);
            storage.almacenar(storageKey, validado.contenido(), validado.mimeType());
            registrarCompensacion(storageKey);

            Instant ahora = ahora();
            DocumentoClinico documento = new DocumentoClinico(documentoId, resuelto.paciente(), contexto,
                    contextoId, requerirCategoria(categoria), normalizarDescripcion(descripcion), ahora);
            documentos.saveAndFlush(documento);
            DocumentoClinicoVersion version = new DocumentoClinicoVersion(versionId, documento, 1,
                    validado.nombreOriginal(), validado.extension(), validado.mimeType(), validado.sizeBytes(),
                    storageKey, hash, null, ahora);
            versiones.saveAndFlush(version);
            documento.asignarVersionActual(version, ahora);
            documentos.saveAndFlush(documento);
            auditarTrasCommit(AuditLog.FILE_UPLOAD, profesional.id(), documento, null, null, null);
            return convertir(documento, warningCuota, null);
        } catch (RuntimeException ex) {
            auditarFallo(AuditLog.FILE_UPLOAD, profesional.id(), documentoId, resuelto, null, null, null);
            throw ex;
        }
    }

    public List<RespuestaDocumentoClinico> listarDelPaciente(Long pacienteId) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        exigirPacientePropio(profesional.id(), pacienteId);
        return documentos.findAllByPacienteIdAndPacienteIdProfesionalAndEstadoOrderByFechaCreacionDesc(
                        pacienteId, profesional.id(), EstadoDocumentoClinico.ACTIVE)
                .stream().map(documento -> convertir(documento, null, null)).toList();
    }

    public RespuestaDocumentoClinico buscar(UUID documentoId) {
        DocumentoClinico documento = buscarPropio(documentoId, proveedorIdentidad.requerir().id());
        return convertir(documento, null, null);
    }

    public ArchivoClinicoDescargado descargar(UUID documentoId) {
        DocumentoClinico documento = buscarPropio(documentoId, proveedorIdentidad.requerir().id());
        if (documento.getEstado() != EstadoDocumentoClinico.ACTIVE)
            throw new ExcepcionRecursoNoEncontrado("Archivo clínico no encontrado");
        DocumentoClinicoVersion version = requerirVersionActual(documento);
        byte[] contenido = leerYVerificarIntegridad(version);
        return new ArchivoClinicoDescargado(contenido, version.getMimeType(), version.getNombreOriginal());
    }

    public ArchivoClinicoDescargado previsualizar(UUID documentoId) {
        ArchivoClinicoDescargado archivo = descargar(documentoId);
        if (!archivo.mimeType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
            return archivo;
        byte[] html = generadorVistaPreviaDocx.generar(archivo.contenido(), archivo.nombreOriginal());
        return new ArchivoClinicoDescargado(html, "text/html;charset=UTF-8", archivo.nombreOriginal());
    }

    @Transactional
    public RespuestaDocumentoClinico nuevaVersion(UUID documentoId, MultipartFile archivo, String motivo) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        DocumentoClinico documento = buscarPropioBloqueado(documentoId, profesional.id());
        ContextoResuelto resuelto = new ContextoResuelto(documento.getPaciente(), documento.getContexto(),
                documento.getContextoId());
        try {
            if (documento.getEstado() != EstadoDocumentoClinico.ACTIVE)
                throw new ExcepcionReglaNegocio("No se puede versionar un documento eliminado");
            String motivoNormalizado = requerirMotivo(motivo);
            pacientes.buscarPropioParaActualizar(documento.getPaciente().getId(), profesional.id())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
            ArchivoClinicoValidado validado = validador.validar(archivo);
            malwareScanner.analizar(validado.contenido());
            String hash = integridad.hash(validado.contenido());
            boolean duplicado = versiones.existeIntegridadEnPaciente(documento.getPaciente().getId(), hash);
            if (duplicado) throw new ExcepcionReglaNegocio(ERROR_DUPLICADO);
            long total = versiones.sumarBytesDelPaciente(documento.getPaciente().getId());
            String warningCuota = total + validado.sizeBytes() >= cuotaRecomendada ? WARNING_CUOTA : null;

            UUID versionId = UUID.randomUUID();
            String storageKey = storageKey(documento.getPaciente().getId(), documentoId, versionId);
            storage.almacenar(storageKey, validado.contenido(), validado.mimeType());
            registrarCompensacion(storageKey);

            DocumentoClinicoVersion anterior = requerirVersionActual(documento);
            int numeroVersion = versiones.maximoNumeroVersion(documentoId) + 1;
            anterior.marcarHistorica();
            versiones.saveAndFlush(anterior);
            Instant ahora = ahora();
            DocumentoClinicoVersion nueva = new DocumentoClinicoVersion(versionId, documento, numeroVersion,
                    validado.nombreOriginal(), validado.extension(), validado.mimeType(), validado.sizeBytes(),
                    storageKey, hash, motivoNormalizado, ahora);
            versiones.saveAndFlush(nueva);
            documento.asignarVersionActual(nueva, ahora);
            documentos.saveAndFlush(documento);
            auditarTrasCommit(AuditLog.FILE_UPDATE, profesional.id(), documento, motivoNormalizado,
                    anterior.getNumeroVersion(), numeroVersion);
            return convertir(documento, warningCuota, null);
        } catch (RuntimeException ex) {
            auditarFallo(AuditLog.FILE_UPDATE, profesional.id(), documentoId, resuelto, motivo,
                    documento.getVersionActual() == null ? null : documento.getVersionActual().getNumeroVersion(), null);
            throw ex;
        }
    }

    @Transactional
    public void eliminar(UUID documentoId, String motivo) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        DocumentoClinico documento = buscarPropioBloqueado(documentoId, profesional.id());
        ContextoResuelto resuelto = contexto(documento);
        try {
            String motivoNormalizado = requerirMotivo(motivo);
            documento.eliminar(motivoNormalizado, ahora(), retencionAnios);
            documentos.saveAndFlush(documento);
            auditarTrasCommit(AuditLog.FILE_DELETE, profesional.id(), documento, motivoNormalizado, null, null);
        } catch (IllegalStateException ex) {
            auditarFallo(AuditLog.FILE_DELETE, profesional.id(), documentoId, resuelto, motivo, null, null);
            throw new ExcepcionReglaNegocio(ex.getMessage());
        } catch (RuntimeException ex) {
            auditarFallo(AuditLog.FILE_DELETE, profesional.id(), documentoId, resuelto, motivo, null, null);
            throw ex;
        }
    }

    @Transactional
    public RespuestaDocumentoClinico restaurar(UUID documentoId, String motivo) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        DocumentoClinico documento = buscarPropioBloqueado(documentoId, profesional.id());
        ContextoResuelto resuelto = contexto(documento);
        String motivoAuditoria = normalizarMotivoOpcional(motivo);
        try {
            if (documento.getEstado() != EstadoDocumentoClinico.DELETED)
                throw new ExcepcionReglaNegocio("Sólo puede restaurarse un documento eliminado");
            requerirVersionActual(documento);
            versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documentoId)
                    .forEach(this::leerYVerificarIntegridad);
            documento.restaurar(ahora());
            documentos.saveAndFlush(documento);
            auditarTrasCommit(AuditLog.FILE_RESTORE, profesional.id(), documento, motivoAuditoria, null, null);
            return convertir(documento, null, null);
        } catch (IllegalStateException ex) {
            auditarFallo(AuditLog.FILE_RESTORE, profesional.id(), documentoId, resuelto, motivoAuditoria, null, null);
            throw new ExcepcionReglaNegocio(ex.getMessage());
        } catch (RuntimeException ex) {
            auditarFallo(AuditLog.FILE_RESTORE, profesional.id(), documentoId, resuelto, motivoAuditoria, null, null);
            throw ex;
        }
    }

    @Transactional
    public RespuestaDocumentoClinico restaurarVersion(UUID documentoId, UUID versionId, String motivo) {
        IdentidadProfesional profesional = proveedorIdentidad.requerir();
        DocumentoClinico documento = buscarPropioBloqueado(documentoId, profesional.id());
        ContextoResuelto resuelto = contexto(documento);
        try {
            if (documento.getEstado() != EstadoDocumentoClinico.ACTIVE)
                throw new ExcepcionReglaNegocio("Debe restaurar el documento antes de restaurar una versión");
            String motivoNormalizado = requerirMotivo(motivo);
            DocumentoClinicoVersion restaurada = versiones.findByIdAndDocumentoId(versionId, documentoId)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Versión clínica no encontrada"));
            if (restaurada.getEstadoVersion() != EstadoVersionDocumento.HISTORICAL)
                throw new ExcepcionReglaNegocio("Sólo puede restaurarse una versión histórica");
            leerYVerificarIntegridad(restaurada);
            DocumentoClinicoVersion anterior = requerirVersionActual(documento);
            anterior.marcarHistorica();
            versiones.saveAndFlush(anterior);
            restaurada.marcarActual();
            versiones.saveAndFlush(restaurada);
            documento.asignarVersionActual(restaurada, ahora());
            documentos.saveAndFlush(documento);
            auditarTrasCommit(AuditLog.FILE_VERSION_RESTORE, profesional.id(), documento, motivoNormalizado,
                    anterior.getNumeroVersion(), restaurada.getNumeroVersion());
            return convertir(documento, null, null);
        } catch (RuntimeException ex) {
            auditarFallo(AuditLog.FILE_VERSION_RESTORE, profesional.id(), documentoId, resuelto, motivo,
                    documento.getVersionActual() == null ? null : documento.getVersionActual().getNumeroVersion(), null);
            throw ex;
        }
    }

    private ContextoResuelto resolverContextoBloqueado(Long profesionalId, ContextoDocumentoClinico contexto,
            Long contextoId) {
        if (contexto == null || contextoId == null || contextoId <= 0)
            throw new ExcepcionArchivoInvalido("El contexto principal del archivo no es válido");
        Long pacienteId = switch (contexto) {
            case PACIENTE -> contextoId;
            case TRATAMIENTO -> tratamientos.findByIdAndPacienteIdProfesional(contextoId, profesionalId)
                    .map(t -> t.getPaciente().getId())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso al tratamiento solicitado"));
            case SESION -> sesiones.findByIdAndTratamientoPacienteIdProfesional(contextoId, profesionalId)
                    .map(s -> s.getTratamiento().getPaciente().getId())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso a la sesión solicitada"));
            case EPICRISIS -> epicrisis.findByIdAndPacienteIdProfesional(contextoId, profesionalId)
                    .map(e -> e.getPaciente().getId())
                    .orElseThrow(() -> new AccessDeniedException("No tiene acceso a la epicrisis solicitada"));
        };
        Paciente paciente = pacientes.buscarPropioParaActualizar(pacienteId, profesionalId)
                .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
        return new ContextoResuelto(paciente, contexto, contextoId);
    }

    private Paciente exigirPacientePropio(Long profesionalId, Long pacienteId) {
        return pacientes.findByIdAndIdProfesional(pacienteId, profesionalId)
                .orElseThrow(() -> new AccessDeniedException("No tiene acceso al paciente solicitado"));
    }

    private DocumentoClinico buscarPropio(UUID documentoId, Long profesionalId) {
        return documentos.findByIdAndPacienteIdProfesional(documentoId, profesionalId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Archivo clínico no encontrado"));
    }

    private DocumentoClinico buscarPropioBloqueado(UUID documentoId, Long profesionalId) {
        return documentos.buscarParaActualizar(documentoId, profesionalId)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Archivo clínico no encontrado"));
    }

    private byte[] leerYVerificarIntegridad(DocumentoClinicoVersion version) {
        if (!storage.existe(version.getStorageKey()))
            throw new ExcepcionReglaNegocio("El archivo requerido ya no está disponible en Object Storage");
        byte[] contenido = storage.leer(version.getStorageKey());
        String hashActual = integridad.hash(contenido);
        if (!MessageDigest.isEqual(hashActual.getBytes(StandardCharsets.US_ASCII),
                version.getIntegridadHash().getBytes(StandardCharsets.US_ASCII)))
            throw new ExcepcionReglaNegocio("El archivo no superó la verificación de integridad");
        return contenido;
    }

    private DocumentoClinicoVersion requerirVersionActual(DocumentoClinico documento) {
        if (documento.getVersionActual() == null
                || documento.getVersionActual().getEstadoVersion() != EstadoVersionDocumento.CURRENT)
            throw new ExcepcionReglaNegocio("El documento no tiene una única versión actual consistente");
        return documento.getVersionActual();
    }

    private RespuestaDocumentoClinico convertir(DocumentoClinico documento, String warningStorage,
            String warningDuplicate) {
        DocumentoClinicoVersion actual = requerirVersionActual(documento);
        List<RespuestaVersionDocumento> historial = versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(
                        documento.getId()).stream()
                .map(v -> new RespuestaVersionDocumento(v.getId(), v.getNumeroVersion(), v.getNombreOriginal(),
                        v.getExtension(), v.getMimeType(), v.getSizeBytes(), v.getEstadoVersion(),
                        v.getMotivoCambio(), v.getFechaCreacion())).toList();
        return new RespuestaDocumentoClinico(documento.getId(), documento.getPaciente().getId(),
                documento.getContexto(), documento.getContextoId(), documento.getCategoria(),
                documento.getDescripcion(), actual.getNombreOriginal(), actual.getMimeType(), actual.getSizeBytes(),
                actual.getNumeroVersion(), documento.getEstado(), warningStorage, warningDuplicate,
                documento.getFechaCreacion(), documento.getFechaActualizacion(), historial);
    }

    private CategoriaDocumentoClinico requerirCategoria(CategoriaDocumentoClinico categoria) {
        if (categoria == null) throw new ExcepcionArchivoInvalido("La categoría es obligatoria");
        return categoria;
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) return null;
        String valor = descripcion.trim();
        if (valor.length() > 1000) throw new ExcepcionArchivoInvalido("La descripción no puede superar 1000 caracteres");
        return valor;
    }

    private String requerirMotivo(String motivo) {
        if (motivo == null || motivo.isBlank()) throw new ExcepcionArchivoInvalido("El motivo es obligatorio");
        String valor = motivo.trim();
        if (valor.length() > 500) throw new ExcepcionArchivoInvalido("El motivo no puede superar 500 caracteres");
        return valor;
    }

    private String normalizarMotivoOpcional(String motivo) {
        if (motivo == null || motivo.isBlank()) return "Restauración solicitada por el profesional";
        return requerirMotivo(motivo);
    }

    private String storageKey(Long pacienteId, UUID documentoId, UUID versionId) {
        return "patients/" + pacienteId + "/documents/" + documentoId + "/versions/" + versionId;
    }

    private Instant ahora() { return Instant.now().truncatedTo(ChronoUnit.MICROS); }

    private void registrarCompensacion(String storageKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            intentarEliminarStorage(storageKey);
            throw new IllegalStateException("La persistencia del archivo requiere una transacción activa");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) intentarEliminarStorage(storageKey);
            }
        });
    }

    private void intentarEliminarStorage(String storageKey) {
        try { storage.eliminar(storageKey); }
        catch (RuntimeException ex) { LOG.error("Falló la compensación del objeto clínico con key {}", storageKey, ex); }
    }

    private void auditarTrasCommit(String accion, Long profesionalId, DocumentoClinico documento, String motivo,
            Integer versionAnterior, Integer versionRestaurada) {
        Runnable tarea = () -> auditarSeguro(accion, profesionalId, documento.getPaciente().getId(), documento.getId(),
                documento.getContexto(), documento.getContextoId(), motivo, versionAnterior, versionRestaurada,
                ResultadoAuditLog.SUCCESS);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { tarea.run(); }
            });
        } else tarea.run();
    }

    private void auditarFallo(String accion, Long profesionalId, UUID documentoId, ContextoResuelto contexto,
            String motivo, Integer versionAnterior, Integer versionRestaurada) {
        auditarSeguro(accion, profesionalId, contexto.paciente().getId(), documentoId, contexto.contexto(),
                contexto.contextoId(), normalizarParaAuditoria(motivo), versionAnterior, versionRestaurada,
                ResultadoAuditLog.FAILED);
    }

    private void auditarSeguro(String accion, Long profesionalId, Long pacienteId, UUID documentoId,
            ContextoDocumentoClinico contexto, Long contextoId, String motivo, Integer versionAnterior,
            Integer versionRestaurada, ResultadoAuditLog resultado) {
        try {
            auditLog.registrarArchivo(accion, profesionalId, pacienteId, documentoId, contexto, contextoId,
                    motivo, versionAnterior, versionRestaurada, resultado);
        } catch (RuntimeException ex) {
            LOG.error("No fue posible registrar el evento {} del documento {}", accion, documentoId, ex);
        }
    }

    private String normalizarParaAuditoria(String motivo) {
        if (motivo == null || motivo.isBlank()) return null;
        String valor = motivo.trim();
        return valor.length() <= 500 ? valor : valor.substring(0, 500);
    }

    private ContextoResuelto contexto(DocumentoClinico documento) {
        return new ContextoResuelto(documento.getPaciente(), documento.getContexto(), documento.getContextoId());
    }

    private record ContextoResuelto(Paciente paciente, ContextoDocumentoClinico contexto, Long contextoId) {}
}
