package com.historialclinico.paciente.controlador;

import com.historialclinico.paciente.dto.RespuestaPaciente;
import com.historialclinico.paciente.dto.SolicitudPaciente;
import com.historialclinico.paciente.servicio.ServicioPaciente;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/profesionales/{idProfesional}/pacientes")
public class ControladorPaciente {

    private final ServicioPaciente servicio;

    public ControladorPaciente(ServicioPaciente servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaPaciente> crear(@PathVariable @Positive Long idProfesional,
                                                    @Valid @RequestBody SolicitudPaciente solicitud) {
        RespuestaPaciente respuesta = servicio.crear(idProfesional, solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest().path("/{idPaciente}")
                .buildAndExpand(respuesta.id()).toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @GetMapping
    public List<RespuestaPaciente> buscarTodos(@PathVariable @Positive Long idProfesional) {
        return servicio.buscarTodos(idProfesional);
    }

    @GetMapping("/{idPaciente}")
    public RespuestaPaciente buscarPorId(@PathVariable @Positive Long idProfesional,
                                         @PathVariable @Positive Long idPaciente) {
        return servicio.buscarPorId(idProfesional, idPaciente);
    }

    @PutMapping("/{idPaciente}")
    public RespuestaPaciente actualizar(@PathVariable @Positive Long idProfesional,
                                        @PathVariable @Positive Long idPaciente,
                                        @Valid @RequestBody SolicitudPaciente solicitud) {
        return servicio.actualizar(idProfesional, idPaciente, solicitud);
    }

    @DeleteMapping("/{idPaciente}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable @Positive Long idProfesional,
                         @PathVariable @Positive Long idPaciente) {
        servicio.eliminar(idProfesional, idPaciente);
    }
}
