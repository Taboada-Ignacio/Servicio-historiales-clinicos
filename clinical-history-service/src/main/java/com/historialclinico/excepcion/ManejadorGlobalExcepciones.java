package com.historialclinico.excepcion;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<ErrorApi> manejarNoEncontrado(ExcepcionRecursoNoEncontrado excepcion, HttpServletRequest solicitud) {
        return construir(HttpStatus.NOT_FOUND, excepcion.getMessage(), solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler(ExcepcionReglaNegocio.class)
    public ResponseEntity<ErrorApi> manejarReglaNegocio(ExcepcionReglaNegocio excepcion, HttpServletRequest solicitud) {
        return construir(HttpStatus.CONFLICT, excepcion.getMessage(), solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException excepcion, HttpServletRequest solicitud) {
        List<ErrorApi.ViolacionCampo> violaciones = excepcion.getBindingResult().getFieldErrors().stream()
                .map(errorCampo -> new ErrorApi.ViolacionCampo(errorCampo.getField(), errorCampo.getDefaultMessage()))
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", solicitud.getRequestURI(), violaciones);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorApi> manejarViolacionRestriccion(ConstraintViolationException excepcion, HttpServletRequest solicitud) {
        List<ErrorApi.ViolacionCampo> violaciones = excepcion.getConstraintViolations().stream()
                .map(errorCampo -> new ErrorApi.ViolacionCampo(errorCampo.getPropertyPath().toString(), errorCampo.getMessage()))
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", solicitud.getRequestURI(), violaciones);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorApi> manejarSolicitudIlegible(HttpMessageNotReadableException excepcion,
            HttpServletRequest solicitud) {
        return construir(HttpStatus.BAD_REQUEST, "La solicitud contiene un valor inválido",
                solicitud.getRequestURI(), List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorApi> manejarAccesoDenegado(AccessDeniedException excepcion,
            HttpServletRequest solicitud) {
        return construir(HttpStatus.FORBIDDEN, excepcion.getMessage(), solicitud.getRequestURI(), List.of());
    }

    private ResponseEntity<ErrorApi> construir(
            HttpStatus estado,
            String mensaje,
            String ruta,
            List<ErrorApi.ViolacionCampo> violaciones
    ) {
        ErrorApi errorApi = new ErrorApi(
                Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, ruta, violaciones
        );
        return ResponseEntity.status(estado).body(errorApi);
    }
}
