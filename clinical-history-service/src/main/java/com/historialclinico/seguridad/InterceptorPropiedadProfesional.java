package com.historialclinico.seguridad;

import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

public class InterceptorPropiedadProfesional implements HandlerInterceptor {
    private final ProveedorIdentidadProfesional proveedor;
    public InterceptorPropiedadProfesional(ProveedorIdentidadProfesional proveedor) { this.proveedor = proveedor; }

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> variables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (variables == null || !variables.containsKey("idProfesional")) return true;
        proveedor.obtener().ifPresent(identidad -> {
            Long solicitado;
            try { solicitado = Long.valueOf(variables.get("idProfesional")); }
            catch (NumberFormatException ex) { throw new ExcepcionRecursoNoEncontrado("Recurso clínico no encontrado"); }
            if (!identidad.id().equals(solicitado))
                throw new ExcepcionRecursoNoEncontrado("Recurso clínico no encontrado");
        });
        return true;
    }
}

