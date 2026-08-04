package com.historialclinico.fichamedica.servicio;

import com.historialclinico.fichamedica.dto.SolicitudFichaMedica;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
import com.historialclinico.fichamedica.repositorio.RepositorioFichaMedica;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicioFichaMedicaTest {

    private final RepositorioFichaMedica repositorio = mock(RepositorioFichaMedica.class);
    private final ServicioFichaMedica servicio = new ServicioFichaMedica(repositorio);

    @Test
    void rechazaSextaFichaMedicaDelProfesional() {
        when(repositorio.contarPorProfesional(10L)).thenReturn(5L);

        assertThatThrownBy(() -> servicio.crear(10L, solicitudValida()))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessage("El profesional no puede tener más de cinco fichas médicas");
    }

    private SolicitudFichaMedica solicitudValida() {
        var opcion = new SolicitudFichaMedica.SolicitudOpcion("Sí", TipoOpcion.SELECCION, null, 0, "smoking");
        var campo = new SolicitudFichaMedica.SolicitudCampo("Tabaquismo", null, 0, false, List.of(opcion));
        var detalle = new SolicitudFichaMedica.SolicitudDetalle(
                "Antecedentes personales no patológicos", null, 0, List.of(campo)
        );
        return new SolicitudFichaMedica("Historia clínica general", null, List.of(detalle));
    }
}
