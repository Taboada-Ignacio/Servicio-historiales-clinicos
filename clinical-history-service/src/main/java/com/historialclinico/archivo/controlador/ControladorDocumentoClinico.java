package com.historialclinico.archivo.controlador;

import com.historialclinico.archivo.dto.RespuestaDocumentoClinico;
import com.historialclinico.archivo.dto.SolicitudMotivoArchivo;
import com.historialclinico.archivo.modelo.CategoriaDocumentoClinico;
import com.historialclinico.archivo.modelo.ContextoDocumentoClinico;
import com.historialclinico.archivo.servicio.ServicioDocumentoClinico;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@Validated
public class ControladorDocumentoClinico {
    private final ServicioDocumentoClinico servicio;

    public ControladorDocumentoClinico(ServicioDocumentoClinico servicio) { this.servicio = servicio; }

    @PostMapping(path = "/api/pacientes/{pacienteId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaDocumentoClinico> adjuntarAPaciente(@PathVariable @Positive Long pacienteId,
            @RequestParam CategoriaDocumentoClinico categoria,
            @RequestParam(required = false) @Size(max = 1000) String descripcion,
            @RequestPart MultipartFile archivo) {
        return creado(servicio.crear(ContextoDocumentoClinico.PACIENTE, pacienteId, categoria, descripcion, archivo));
    }

    @PostMapping(path = "/api/tratamientos/{tratamientoId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaDocumentoClinico> adjuntarATratamiento(
            @PathVariable @Positive Long tratamientoId, @RequestParam CategoriaDocumentoClinico categoria,
            @RequestParam(required = false) @Size(max = 1000) String descripcion,
            @RequestPart MultipartFile archivo) {
        return creado(servicio.crear(ContextoDocumentoClinico.TRATAMIENTO, tratamientoId, categoria,
                descripcion, archivo));
    }

    @PostMapping(path = "/api/sesiones/{sesionId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaDocumentoClinico> adjuntarASesion(@PathVariable @Positive Long sesionId,
            @RequestParam CategoriaDocumentoClinico categoria,
            @RequestParam(required = false) @Size(max = 1000) String descripcion,
            @RequestPart MultipartFile archivo) {
        return creado(servicio.crear(ContextoDocumentoClinico.SESION, sesionId, categoria, descripcion, archivo));
    }

    @PostMapping(path = "/api/epicrisis/{epicrisisId}/archivos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RespuestaDocumentoClinico> adjuntarAEpicrisis(@PathVariable @Positive Long epicrisisId,
            @RequestParam CategoriaDocumentoClinico categoria,
            @RequestParam(required = false) @Size(max = 1000) String descripcion,
            @RequestPart MultipartFile archivo) {
        return creado(servicio.crear(ContextoDocumentoClinico.EPICRISIS, epicrisisId, categoria,
                descripcion, archivo));
    }

    @GetMapping("/api/pacientes/{pacienteId}/archivos")
    public List<RespuestaDocumentoClinico> listar(@PathVariable @Positive Long pacienteId) {
        return servicio.listarDelPaciente(pacienteId);
    }

    @GetMapping("/api/archivos/{archivoId}")
    public RespuestaDocumentoClinico buscar(@PathVariable UUID archivoId) {
        return servicio.buscar(archivoId);
    }

    @GetMapping("/api/archivos/{archivoId}/download")
    public ResponseEntity<byte[]> descargar(@PathVariable UUID archivoId) {
        var archivo = servicio.descargar(archivoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.mimeType()))
                .contentLength(archivo.contenido().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }

    @GetMapping("/api/archivos/{archivoId}/preview")
    public ResponseEntity<byte[]> previsualizar(@PathVariable UUID archivoId) {
        var archivo = servicio.previsualizar(archivoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.mimeType()))
                .contentLength(archivo.contenido().length)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(archivo.nombreOriginal(), StandardCharsets.UTF_8).build().toString())
                .body(archivo.contenido());
    }

    @PostMapping(path = "/api/archivos/{archivoId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RespuestaDocumentoClinico nuevaVersion(@PathVariable UUID archivoId,
            @RequestParam @Size(max = 500) String motivo, @RequestPart MultipartFile archivo) {
        return servicio.nuevaVersion(archivoId, archivo, motivo);
    }

    @DeleteMapping("/api/archivos/{archivoId}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID archivoId,
            @Valid @RequestBody SolicitudMotivoArchivo solicitud) {
        servicio.eliminar(archivoId, solicitud.motivo());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/archivos/{archivoId}/restore")
    public RespuestaDocumentoClinico restaurar(@PathVariable UUID archivoId,
            @RequestBody(required = false) SolicitudMotivoArchivo solicitud) {
        return servicio.restaurar(archivoId, solicitud == null ? null : solicitud.motivo());
    }

    @PostMapping("/api/archivos/{archivoId}/versions/{versionId}/restore")
    public RespuestaDocumentoClinico restaurarVersion(@PathVariable UUID archivoId, @PathVariable UUID versionId,
            @Valid @RequestBody SolicitudMotivoArchivo solicitud) {
        return servicio.restaurarVersion(archivoId, versionId, solicitud.motivo());
    }

    private ResponseEntity<RespuestaDocumentoClinico> creado(RespuestaDocumentoClinico respuesta) {
        return ResponseEntity.created(URI.create("/api/archivos/" + respuesta.id())).body(respuesta);
    }
}
