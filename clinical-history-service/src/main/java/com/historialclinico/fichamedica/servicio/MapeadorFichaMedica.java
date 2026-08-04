package com.historialclinico.fichamedica.servicio;

import com.historialclinico.fichamedica.dto.SolicitudFichaMedica;
import com.historialclinico.fichamedica.dto.RespuestaFichaMedica;
import com.historialclinico.fichamedica.modelo.OpcionCampo;
import com.historialclinico.fichamedica.modelo.CampoParaLlenar;
import com.historialclinico.fichamedica.modelo.DetalleFicha;
import com.historialclinico.fichamedica.modelo.FichaMedica;

import java.util.List;

final class MapeadorFichaMedica {

    private MapeadorFichaMedica() {
    }

    static FichaMedica convertirAEntidad(Long idProfesional, SolicitudFichaMedica solicitud) {
        FichaMedica ficha = new FichaMedica(idProfesional, solicitud.nombre().trim(), recortarANulo(solicitud.descripcion()));
        convertirADetalles(solicitud.detalles()).forEach(ficha::agregarDetalle);
        return ficha;
    }

    static List<DetalleFicha> convertirADetalles(List<SolicitudFichaMedica.SolicitudDetalle> solicitudes) {
        return solicitudes.stream().map(solicitudDetalle -> {
            DetalleFicha detalle = new DetalleFicha(
                    solicitudDetalle.titulo().trim(),
                    recortarANulo(solicitudDetalle.descripcion()),
                    solicitudDetalle.orden()
            );
            solicitudDetalle.campos().stream().map(solicitudCampo -> {
                CampoParaLlenar campo = new CampoParaLlenar(
                        solicitudCampo.titulo().trim(),
                        recortarANulo(solicitudCampo.descripcion()),
                        solicitudCampo.orden(),
                        solicitudCampo.permiteSeleccionMultiple()
                );
                solicitudCampo.opciones().stream()
                        .map(opcion -> new OpcionCampo(
                                recortarANulo(opcion.titulo()),
                                opcion.tipo(),
                                recortarANulo(opcion.descripcion()),
                                opcion.orden(),
                                recortarANulo(opcion.grupoExclusion())
                        ))
                        .forEach(campo::agregarOpcion);
                return campo;
            }).forEach(detalle::agregarCampo);
            return detalle;
        }).toList();
    }

    static RespuestaFichaMedica convertirARespuesta(FichaMedica ficha) {
        return new RespuestaFichaMedica(
                ficha.getId(), ficha.getIdProfesional(), ficha.getNombre(), ficha.getDescripcion(),
                ficha.getFechaCreacion(), ficha.getFechaActualizacion(), ficha.getVersion(),
                ficha.getDetalles().stream().map(detalle -> new RespuestaFichaMedica.RespuestaDetalle(
                        detalle.getId(), detalle.getTitulo(), detalle.getDescripcion(), detalle.getOrden(),
                        detalle.getCampos().stream().map(campo -> new RespuestaFichaMedica.RespuestaCampo(
                                campo.getId(), campo.getTitulo(), campo.getDescripcion(), campo.getOrden(),
                                campo.isPermiteSeleccionMultiple(),
                                campo.getOpciones().stream().map(opcion -> new RespuestaFichaMedica.RespuestaOpcion(
                                        opcion.getId(), opcion.getTitulo(), opcion.getTipo(), opcion.getDescripcion(),
                                        opcion.getOrden(), opcion.getGrupoExclusion()
                                )).toList()
                        )).toList()
                )).toList()
        );
    }

    private static String recortarANulo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
