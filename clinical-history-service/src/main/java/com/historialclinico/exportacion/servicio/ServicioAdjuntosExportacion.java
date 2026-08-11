package com.historialclinico.exportacion.servicio;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.historialclinico.archivo.modelo.DocumentoClinicoVersion;
import com.historialclinico.archivo.modelo.EstadoDocumentoClinico;
import com.historialclinico.archivo.modelo.EstadoVersionDocumento;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinico;
import com.historialclinico.archivo.storage.ClinicalFileStorage;
import com.historialclinico.auditoria.servicio.ServicioCifradoAuditoria;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import com.historialclinico.excepcion.ExcepcionServicioArchivosNoDisponible;
import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.tratamiento.repositorio.RepositorioSesionTratamiento;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ServicioAdjuntosExportacion {
    private final RepositorioDocumentoClinico documentos;
    private final RepositorioTratamiento tratamientos;
    private final RepositorioSesionTratamiento sesiones;
    private final RepositorioEpicrisis epicrisis;
    private final ClinicalFileStorage storage;
    private final ServicioCifradoAuditoria integridad;
    private final ObjectMapper objectMapper;

    public ServicioAdjuntosExportacion(RepositorioDocumentoClinico documentos,
            RepositorioTratamiento tratamientos, RepositorioSesionTratamiento sesiones,
            RepositorioEpicrisis epicrisis, ClinicalFileStorage storage,
            ServicioCifradoAuditoria integridad, ObjectMapper objectMapper) {
        this.documentos = documentos;
        this.tratamientos = tratamientos;
        this.sesiones = sesiones;
        this.epicrisis = epicrisis;
        this.storage = storage;
        this.integridad = integridad;
        this.objectMapper = objectMapper;
    }

    public List<AdjuntoExportable> buscarActivos(Long pacienteId, Long profesionalId) {
        return documentos.findAllByPacienteIdAndPacienteIdProfesionalAndEstadoOrderByFechaCreacionDesc(
                        pacienteId, profesionalId, EstadoDocumentoClinico.ACTIVE).stream()
                .map(documento -> {
                    validarContexto(documento.getContexto().name(), documento.getContextoId(),
                            pacienteId, profesionalId);
                    DocumentoClinicoVersion version = documento.getVersionActual();
                    if (version == null || version.getEstadoVersion() != EstadoVersionDocumento.CURRENT)
                        throw new ExcepcionReglaNegocio(
                                "Un archivo adjunto no tiene una versión CURRENT consistente");
                    var referencia = new HistoriaClinicaDocumento.ArchivoAdjunto(documento.getId(),
                            version.getNombreOriginal(), documento.getCategoria().name(), documento.getContexto().name(),
                            documento.getContextoId(), version.getMimeType(), version.getSizeBytes(),
                            version.getIntegridadHash(), documento.getDescripcion());
                    return new AdjuntoExportable(referencia, version.getStorageKey());
                }).toList();
    }

    public PaqueteExportacion generarZip(Long pacienteId, FormatoExportacion formatoHistoria,
            byte[] historiaClinica, Instant fecha, List<AdjuntoExportable> adjuntos) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("historia-clinica-completa-", ".zip");
            try (var salida = Files.newOutputStream(temporal); var zip = new ZipOutputStream(salida)) {
                agregarBytes(zip, "Historia_Clinica." + formatoHistoria.getExtension(), historiaClinica, fecha);
                agregarBytes(zip, "manifest.json", manifest(pacienteId, formatoHistoria, fecha, adjuntos), fecha);
                Set<String> nombresUsados = new HashSet<>();
                for (AdjuntoExportable adjunto : adjuntos) {
                    String nombre = nombreUnico(adjunto.referencia().nombreOriginal(), nombresUsados);
                    String ruta = "Adjuntos/" + nombre;
                    validarRutaZip(ruta);
                    if (!storage.existe(adjunto.storageKey()))
                        throw new ExcepcionReglaNegocio(
                                "Un archivo requerido ya no está disponible en Object Storage");
                    ZipEntry entrada = new ZipEntry(ruta);
                    entrada.setTime(fecha.toEpochMilli());
                    zip.putNextEntry(entrada);
                    String hash;
                    try (var contenido = storage.abrirLectura(adjunto.storageKey())) {
                        hash = integridad.copiarYHash(contenido, zip);
                    }
                    zip.closeEntry();
                    if (!MessageDigest.isEqual(hash.getBytes(StandardCharsets.US_ASCII),
                            adjunto.referencia().integridad().getBytes(StandardCharsets.US_ASCII)))
                        throw new ExcepcionReglaNegocio(
                                "Un archivo adjunto no superó la verificación de integridad");
                }
            }
            String hashFinal;
            try (var contenido = Files.newInputStream(temporal)) { hashFinal = integridad.hash(contenido); }
            return new PaqueteExportacion(temporal, Files.size(temporal), hashFinal);
        } catch (ExcepcionReglaNegocio | ExcepcionServicioArchivosNoDisponible ex) {
            eliminar(temporal);
            throw ex;
        } catch (IOException ex) {
            eliminar(temporal);
            throw new ExcepcionServicioArchivosNoDisponible(
                    "No fue posible generar el paquete con archivos adjuntos", ex);
        } catch (RuntimeException ex) {
            eliminar(temporal);
            throw ex;
        }
    }

    private byte[] manifest(Long pacienteId, FormatoExportacion formato, Instant fecha,
            List<AdjuntoExportable> adjuntos) throws IOException {
        var archivos = adjuntos.stream().map(adjunto -> {
            var referencia = adjunto.referencia();
            return new ArchivoManifest(referencia.documentoId(), referencia.nombreOriginal(), referencia.categoria(),
                    referencia.contexto(), referencia.contextoId(), referencia.mimeType(), referencia.sizeBytes(),
                    referencia.integridad());
        }).toList();
        return objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(new ManifestExportacion(fecha, pacienteId, formato, archivos));
    }

    private void validarContexto(String contexto, Long contextoId, Long pacienteId, Long profesionalId) {
        boolean valido = switch (contexto) {
            case "PACIENTE" -> contextoId.equals(pacienteId);
            case "TRATAMIENTO" -> tratamientos.existsByIdAndPacienteIdAndPacienteIdProfesional(
                    contextoId, pacienteId, profesionalId);
            case "SESION" -> sesiones.findByIdAndTratamientoPacienteIdProfesional(contextoId, profesionalId)
                    .map(sesion -> sesion.getTratamiento().getPaciente().getId().equals(pacienteId)).orElse(false);
            case "EPICRISIS" -> epicrisis.existsByIdAndPacienteIdAndPacienteIdProfesional(
                    contextoId, pacienteId, profesionalId);
            default -> false;
        };
        if (!valido) throw new ExcepcionReglaNegocio(
                "Un archivo adjunto no corresponde al paciente exportado");
    }

    private void agregarBytes(ZipOutputStream zip, String nombre, byte[] contenido, Instant fecha) throws IOException {
        validarRutaZip(nombre);
        ZipEntry entrada = new ZipEntry(nombre);
        entrada.setTime(fecha.toEpochMilli());
        zip.putNextEntry(entrada);
        zip.write(contenido);
        zip.closeEntry();
    }

    private String nombreUnico(String original, Set<String> usados) {
        String normalizado = Normalizer.normalize(original == null ? "archivo" : original, Normalizer.Form.NFC)
                .replace('\\', '/');
        normalizado = normalizado.substring(normalizado.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}<>:\"/\\\\|?*]", "_").trim()
                .replaceAll("\\.{2,}", ".");
        while (normalizado.startsWith(".")) normalizado = normalizado.substring(1);
        if (normalizado.isBlank()) normalizado = "archivo";
        String base = normalizado;
        String extension = "";
        int punto = normalizado.lastIndexOf('.');
        if (punto > 0) {
            base = normalizado.substring(0, punto);
            extension = normalizado.substring(punto);
        }
        String candidato = base + extension;
        int numero = 2;
        while (!usados.add(candidato.toLowerCase(Locale.ROOT)))
            candidato = base + "_" + numero++ + extension;
        return candidato;
    }

    private void validarRutaZip(String ruta) {
        if (ruta.startsWith("/") || ruta.startsWith("\\") || ruta.contains("../") || ruta.contains("..\\"))
            throw new ExcepcionReglaNegocio("Se rechazó un nombre de archivo inseguro para el ZIP");
    }

    private void eliminar(Path archivo) {
        if (archivo == null) return;
        try { Files.deleteIfExists(archivo); }
        catch (IOException ignored) {
            // El sistema operativo completará la limpieza del temporal si el borrado inmediato falla.
        }
    }

    private record ManifestExportacion(Instant generatedAt, Long patientId,
                                        FormatoExportacion clinicalHistoryFormat,
                                        List<ArchivoManifest> files) {}
    private record ArchivoManifest(UUID documentId, String fileName, String category, String context,
                                   Long contextId, String mimeType, long sizeBytes, String integrity) {}
}
