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
}
