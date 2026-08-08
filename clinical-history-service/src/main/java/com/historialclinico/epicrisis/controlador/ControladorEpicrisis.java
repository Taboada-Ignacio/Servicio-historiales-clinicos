package com.historialclinico.epicrisis.controlador;

import com.historialclinico.epicrisis.dto.RespuestaEpicrisis;
import com.historialclinico.epicrisis.dto.SolicitudEpicrisis;
import com.historialclinico.epicrisis.servicio.ServicioEpicrisis;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import com.historialclinico.auditoria.dto.InformeAuditoriaClinica;
import com.historialclinico.auditoria.dto.RespuestaAuditoriaRectificacion;
import com.historialclinico.epicrisis.dto.SolicitudRectificacionEpicrisis;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Validated
@RestController
@RequestMapping("/api/v1/profesionales/{idProfesional}/pacientes/{idPaciente}/epicrisis")
public class ControladorEpicrisis {

    private final ServicioEpicrisis servicio;

    public ControladorEpicrisis(ServicioEpicrisis servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaEpicrisis> registrar(
            @PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente,
            @Valid @RequestBody SolicitudEpicrisis solicitud
    ) {
        RespuestaEpicrisis respuesta = servicio.registrar(idProfesional, idPaciente, solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idEpicrisis}")
                .buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @GetMapping
    public List<RespuestaEpicrisis> buscarDelPaciente(
            @PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente
    ) {
        return servicio.buscarDelPaciente(idProfesional, idPaciente);
    }

    @PostMapping("/{idEpicrisis}/rectificaciones")
    public RespuestaEpicrisis rectificar(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idEpicrisis,
            @Valid @RequestBody SolicitudRectificacionEpicrisis solicitud) {
        return servicio.rectificar(idProfesional, idPaciente, idEpicrisis, solicitud);
    }

    @GetMapping("/{idEpicrisis}/auditoria")
    public List<RespuestaAuditoriaRectificacion> auditoria(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idEpicrisis) {
        return servicio.auditoria(idProfesional, idPaciente, idEpicrisis);
    }

    @GetMapping(value = "/{idEpicrisis}/informe-auditoria", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InformeAuditoriaClinica> informeAuditoria(@PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idPaciente, @PathVariable @Positive Long idEpicrisis) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=auditoria-epicrisis-" + idEpicrisis + ".json")
                .body(servicio.informeAuditoria(idProfesional, idPaciente, idEpicrisis));
    }
}
