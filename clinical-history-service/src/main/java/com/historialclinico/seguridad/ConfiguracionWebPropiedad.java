package com.historialclinico.seguridad;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfiguracionWebPropiedad implements WebMvcConfigurer {
    private final ProveedorIdentidadProfesional proveedor;
    public ConfiguracionWebPropiedad(ProveedorIdentidadProfesional proveedor) { this.proveedor = proveedor; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InterceptorPropiedadProfesional(proveedor)).addPathPatterns("/api/**");
    }
}

