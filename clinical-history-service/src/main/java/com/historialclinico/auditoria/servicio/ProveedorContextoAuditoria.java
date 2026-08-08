package com.historialclinico.auditoria.servicio;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;
import com.historialclinico.seguridad.ProveedorIdentidadProfesional;

@Component
public class ProveedorContextoAuditoria {
    private final boolean confiarProxy;
    private final ProveedorIdentidadProfesional proveedorIdentidad;
    public ProveedorContextoAuditoria(@Value("${app.auditoria.confiar-cabeceras-proxy:false}") boolean confiarProxy,
            ProveedorIdentidadProfesional proveedorIdentidad) {
        this.confiarProxy = confiarProxy; this.proveedorIdentidad = proveedorIdentidad;
    }
    public ContextoSolicitudAuditoria obtener(Long idProfesional) {
        var atributos = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = atributos.getRequest();
        String ip = request.getRemoteAddr();
        if (confiarProxy) {
            String reenviada = request.getHeader("X-Forwarded-For");
            if (reenviada != null && !reenviada.isBlank()) ip = reenviada.split(",")[0].trim();
        }
        String dispositivo = limitar(request.getHeader("X-Device-Id"), 150);
        String agente = limitar(request.getHeader("User-Agent"), 320);
        String equipo = "dispositivo=" + (dispositivo == null ? "no-informado" : dispositivo)
                + "; user-agent=" + (agente == null ? "no-informado" : agente);
        String sesion = request.getSession(false) == null ? null : request.getSession(false).getId();
        String solicitud = limitar(request.getHeader("X-Request-Id"), 128);
        if (solicitud == null) solicitud = UUID.randomUUID().toString();
        var identidad = proveedorIdentidad.obtener().orElse(null);
        String nombre = identidad == null ? limitar(request.getHeader("X-Professional-Name"), 200) : limitar(identidad.nombre(), 200);
        String matricula = identidad == null ? limitar(request.getHeader("X-Professional-License"), 100) : limitar(identidad.matricula(), 100);
        return new ContextoSolicitudAuditoria(idProfesional, nombre, matricula, limitar(ip, 64), equipo, sesion, solicitud);
    }
    private String limitar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.replaceAll("[\\r\\n]", " ").trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
