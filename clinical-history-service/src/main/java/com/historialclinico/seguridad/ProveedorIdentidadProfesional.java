package com.historialclinico.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

@Component
public class ProveedorIdentidadProfesional {
    private final String claimId;
    private final String claimNombre;
    private final String claimMatricula;

    public ProveedorIdentidadProfesional(@Value("${app.seguridad.claim-id-profesional:professional_id}") String claimId,
            @Value("${app.seguridad.claim-nombre-profesional:professional_name}") String claimNombre,
            @Value("${app.seguridad.claim-matricula-profesional:professional_license}") String claimMatricula) {
        this.claimId = claimId; this.claimNombre = claimNombre; this.claimMatricula = claimMatricula;
    }

    public Optional<IdentidadProfesional> obtener() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated() || !(autenticacion.getPrincipal() instanceof Jwt jwt))
            return Optional.empty();
        Object valor = jwt.getClaims().get(claimId);
        if (valor == null) throw new AccessDeniedException("El token no identifica al profesional");
        Long id;
        try { id = Long.valueOf(String.valueOf(valor)); }
        catch (RuntimeException ex) { throw new AccessDeniedException("El token no contiene un identificador profesional válido", ex); }
        String nombre = texto(jwt.getClaims().get(claimNombre));
        String matricula = texto(jwt.getClaims().get(claimMatricula));
        if (nombre == null || matricula == null)
            throw new AccessDeniedException("El token debe identificar el nombre y la matrícula profesional");
        return Optional.of(new IdentidadProfesional(id, nombre, matricula));
    }
    private String texto(Object valor) { return valor == null || String.valueOf(valor).isBlank() ? null : String.valueOf(valor); }
}
