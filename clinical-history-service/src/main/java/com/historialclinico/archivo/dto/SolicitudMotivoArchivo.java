package com.historialclinico.archivo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudMotivoArchivo(
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 500, message = "El motivo no puede superar 500 caracteres")
        String motivo
) {}
