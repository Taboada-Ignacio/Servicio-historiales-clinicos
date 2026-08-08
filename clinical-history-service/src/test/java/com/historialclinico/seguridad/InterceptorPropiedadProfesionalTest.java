package com.historialclinico.seguridad;

import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterceptorPropiedadProfesionalTest {
    private final ProveedorIdentidadProfesional proveedor = mock(ProveedorIdentidadProfesional.class);
    private final InterceptorPropiedadProfesional interceptor = new InterceptorPropiedadProfesional(proveedor);

    @Test
    void permiteSolamenteLaRutaDelProfesionalAutenticado() {
        when(proveedor.obtener()).thenReturn(Optional.of(new IdentidadProfesional(10L, "Profesional", "MP 10")));
        var propia = solicitudConProfesional("10");
        var ajena = solicitudConProfesional("11");

        assertDoesNotThrow(() -> interceptor.preHandle(propia, new MockHttpServletResponse(), new Object()));
        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> interceptor.preHandle(ajena, new MockHttpServletResponse(), new Object()));
    }

    private MockHttpServletRequest solicitudConProfesional(String id) {
        var request = new MockHttpServletRequest();
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("idProfesional", id));
        return request;
    }
}
