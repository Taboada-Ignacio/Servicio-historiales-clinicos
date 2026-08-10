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
    private final boolean modoLocal;
    private final IdentidadProfesional identidadLocal;

    public ProveedorIdentidadProfesional(@Value("${app.seguridad.claim-id-profesional:professional_id}") String claimId,
            @Value("${app.seguridad.claim-nombre-profesional:professional_name}") String claimNombre,
            @Value("${app.seguridad.claim-matricula-profesional:professional_license}") String claimMatricula,
            @Value("${app.seguridad.modo:local}") String modo,
            @Value("${app.seguridad.profesional-local-id:1}") Long idLocal,
            @Value("${app.seguridad.profesional-local-nombre:Profesional local}") String nombreLocal,
            @Value("${app.seguridad.profesional-local-matricula:LOCAL}") String matriculaLocal) {
        this.claimId = claimId; this.claimNombre = claimNombre; this.claimMatricula = claimMatricula;
        this.modoLocal = "local".equalsIgnoreCase(modo);
        this.identidadLocal = new IdentidadProfesional(idLocal, nombreLocal, matriculaLocal);
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

    public IdentidadProfesional requerir() {
        return obtener().orElseGet(() -> {
            if (modoLocal) return identidadLocal;
            throw new AccessDeniedException("Se requiere un profesional autenticado");
        });
    }

    private String texto(Object valor) { return valor == null || String.valueOf(valor).isBlank() ? null : String.valueOf(valor); }
}
