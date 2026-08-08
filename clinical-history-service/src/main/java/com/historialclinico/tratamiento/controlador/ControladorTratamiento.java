package com.historialclinico.tratamiento.controlador;
import com.historialclinico.tratamiento.dto.*;
import com.historialclinico.tratamiento.servicio.ServicioTratamiento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.net.URI;
import java.util.List;
import com.historialclinico.auditoria.dto.InformeAuditoriaClinica;
import com.historialclinico.auditoria.dto.RespuestaAuditoriaRectificacion;
import com.historialclinico.auditoria.modelo.TipoRegistroClinico;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Validated @RestController
@RequestMapping("/api/v1/profesionales/{idProfesional}/pacientes/{idPaciente}/tratamientos")
public class ControladorTratamiento {
    private final ServicioTratamiento servicio;
    public ControladorTratamiento(ServicioTratamiento servicio) { this.servicio = servicio; }
    @PostMapping
    public ResponseEntity<RespuestaTratamiento> crear(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @Valid @RequestBody SolicitudTratamiento solicitud) {
        var respuesta = servicio.crear(idProfesional, idPaciente, solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }
    @GetMapping
    public List<RespuestaTratamiento> buscarDelPaciente(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente) { return servicio.buscarDelPaciente(idProfesional, idPaciente); }

    @GetMapping("/sin-terminar")
    public List<RespuestaTratamiento> buscarSinTerminar(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente) { return servicio.buscarSinTerminar(idProfesional, idPaciente); }

    @PostMapping("/{idTratamiento}/sesiones")
    public ResponseEntity<RespuestaTratamiento> registrarSesion(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento,
            @Valid @RequestBody SolicitudTratamiento.SolicitudSesion solicitud) {
        var respuesta = servicio.registrarSesion(idProfesional, idPaciente, idTratamiento, solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{nroSesion}")
                .buildAndExpand(respuesta.sesiones().size()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @PostMapping("/{idTratamiento}/rectificaciones")
    public RespuestaTratamiento rectificarTratamiento(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento,
            @Valid @RequestBody SolicitudRectificacionTratamiento solicitud) {
        return servicio.rectificarTratamiento(idProfesional, idPaciente, idTratamiento, solicitud);
    }

    @PostMapping("/{idTratamiento}/sesiones/{idSesion}/rectificaciones")
    public RespuestaTratamiento.RespuestaSesion rectificarSesion(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento,
            @PathVariable @Positive Long idSesion, @Valid @RequestBody SolicitudRectificacionSesion solicitud) {
        return servicio.rectificarSesion(idProfesional, idPaciente, idTratamiento, idSesion, solicitud);
    }

    @GetMapping("/{idTratamiento}/auditoria")
    public List<RespuestaAuditoriaRectificacion> auditoriaTratamiento(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento) {
        return servicio.auditoria(idProfesional, idPaciente, TipoRegistroClinico.TRATAMIENTO, idTratamiento, idTratamiento);
    }

    @GetMapping("/{idTratamiento}/sesiones/{idSesion}/auditoria")
    public List<RespuestaAuditoriaRectificacion> auditoriaSesion(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento,
            @PathVariable @Positive Long idSesion) {
        return servicio.auditoria(idProfesional, idPaciente, TipoRegistroClinico.SESION, idTratamiento, idSesion);
    }

    @GetMapping(value = "/{idTratamiento}/informe-auditoria", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InformeAuditoriaClinica> informeTratamiento(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento) {
        return descargar("tratamiento", idTratamiento, servicio.informeAuditoria(idProfesional, idPaciente,
                TipoRegistroClinico.TRATAMIENTO, idTratamiento, idTratamiento));
    }

    @GetMapping(value = "/{idTratamiento}/sesiones/{idSesion}/informe-auditoria", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InformeAuditoriaClinica> informeSesion(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idTratamiento,
            @PathVariable @Positive Long idSesion) {
        return descargar("sesion", idSesion, servicio.informeAuditoria(idProfesional, idPaciente,
                TipoRegistroClinico.SESION, idTratamiento, idSesion));
    }

    private ResponseEntity<InformeAuditoriaClinica> descargar(String tipo, Long id, InformeAuditoriaClinica informe) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=auditoria-" + tipo + "-" + id + ".json").body(informe);
    }
}
