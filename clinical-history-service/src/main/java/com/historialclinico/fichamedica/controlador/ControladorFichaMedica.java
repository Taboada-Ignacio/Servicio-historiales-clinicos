package com.historialclinico.fichamedica.controlador;

import com.historialclinico.fichamedica.dto.SolicitudFichaMedica;
import com.historialclinico.fichamedica.dto.RespuestaFichaMedica;
import com.historialclinico.fichamedica.servicio.ServicioFichaMedica;
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
@RequestMapping("/api/v1/profesionales/{idProfesional}/fichas-medicas")
public class ControladorFichaMedica {

    private final ServicioFichaMedica servicio;

    public ControladorFichaMedica(ServicioFichaMedica servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaFichaMedica> crear(
            @PathVariable @Positive Long idProfesional,
            @Valid @RequestBody SolicitudFichaMedica solicitud
    ) {
        RespuestaFichaMedica respuesta = servicio.crear(idProfesional, solicitud);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idFicha}")
                .buildAndExpand(respuesta.id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(respuesta);
    }

    @GetMapping
    public List<RespuestaFichaMedica> buscarTodos(@PathVariable @Positive Long idProfesional) {
        return servicio.buscarTodos(idProfesional);
    }

    @GetMapping("/{idFicha}")
    public RespuestaFichaMedica buscarPorId(
            @PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idFicha
    ) {
        return servicio.buscarPorId(idProfesional, idFicha);
    }

    @PutMapping("/{idFicha}")
    public RespuestaFichaMedica actualizar(
            @PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idFicha,
            @Valid @RequestBody SolicitudFichaMedica solicitud
    ) {
        return servicio.actualizar(idProfesional, idFicha, solicitud);
    }

    @DeleteMapping("/{idFicha}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @PathVariable @Positive Long idProfesional,
            @PathVariable @Positive Long idFicha
    ) {
        servicio.eliminar(idProfesional, idFicha);
    }
}
