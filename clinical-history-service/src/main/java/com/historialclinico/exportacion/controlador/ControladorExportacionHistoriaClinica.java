package com.historialclinico.exportacion.controlador;

import com.historialclinico.exportacion.dto.SolicitudExportacionHistoriaClinica;
import com.historialclinico.exportacion.dto.PacienteExportable;
import com.historialclinico.exportacion.dto.RespuestaExportacionHistoriaClinica;
import com.historialclinico.exportacion.servicio.ServicioExportacionHistoriaClinica;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@Validated
public class ControladorExportacionHistoriaClinica {
    private final ServicioExportacionHistoriaClinica servicio;

    public ControladorExportacionHistoriaClinica(ServicioExportacionHistoriaClinica servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<PacienteExportable> buscarPacientesPropios() {
        return servicio.buscarPacientesPropios();
    }

    @GetMapping("/{pacienteId}/historia-clinica/exportaciones")
    public List<RespuestaExportacionHistoriaClinica> listarExportaciones(
            @PathVariable @Positive Long pacienteId) {
        return servicio.listarExportaciones(pacienteId);
    }

    @GetMapping("/{pacienteId}/historia-clinica/exportaciones/{exportacionId}")
    public RespuestaExportacionHistoriaClinica obtenerExportacion(
            @PathVariable @Positive Long pacienteId,
            @PathVariable @Positive Long exportacionId) {
        return servicio.obtenerExportacion(pacienteId, exportacionId);
    }

    @PostMapping("/{pacienteId}/historia-clinica/exportar")
    public ResponseEntity<Resource> exportar(@PathVariable @Positive Long pacienteId,
            @Valid @RequestBody SolicitudExportacionHistoriaClinica solicitud) throws java.io.IOException {
        var archivo = servicio.exportar(pacienteId, solicitud);
        Resource cuerpo;
        try { cuerpo = new InputStreamResource(archivo.abrirLectura()); }
        catch (java.io.IOException ex) { archivo.limpiar(); throw ex; }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(archivo.tipoContenido()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombreArchivo(), StandardCharsets.UTF_8).build().toString())
                .contentLength(archivo.longitud())
                .body(cuerpo);
    }
}
