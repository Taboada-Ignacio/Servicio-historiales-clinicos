package com.historialclinico.paciente.dto;

import com.historialclinico.paciente.modelo.Sexo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

public record SolicitudPaciente(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Size(max = 100) String apellido,
        @NotBlank @Pattern(regexp = "\\d{6,12}", message = "debe contener entre 6 y 12 dígitos") String dni,
        @Size(max = 30) @Pattern(regexp = "\\d*", message = "solo puede contener números") String telefono,
        @NotNull @Past LocalDate fechaNacimiento,
        @NotNull Sexo sexo,
        List<@Valid SolicitudFichaPaciente> fichas
) {
    public record SolicitudFichaPaciente(
            @Positive Long idFichaPaciente,
            @NotNull @Positive Long idFichaMedica,
            @NotEmpty List<@Valid SolicitudRespuesta> respuestas
    ) {}

    public record SolicitudRespuesta(
            @NotNull @Positive Long idOpcion,
            @Size(max = 1000) String valor,
            Boolean seleccionada
    ) {}
}
