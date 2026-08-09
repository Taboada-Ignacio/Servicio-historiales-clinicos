package com.historialclinico.fichamedica.servicio;

import com.historialclinico.fichamedica.dto.SolicitudFichaMedica;
import com.historialclinico.fichamedica.dto.RespuestaFichaMedica;
import com.historialclinico.fichamedica.modelo.FichaMedica;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
import com.historialclinico.fichamedica.repositorio.RepositorioFichaMedica;
import com.historialclinico.excepcion.ExcepcionReglaNegocio;
import com.historialclinico.excepcion.ExcepcionRecursoNoEncontrado;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ServicioFichaMedica {

    static final int MAXIMO_FICHAS_POR_PROFESIONAL = 5;

    private final RepositorioFichaMedica repositorio;

    public ServicioFichaMedica(RepositorioFichaMedica repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public RespuestaFichaMedica crear(Long idProfesional, SolicitudFichaMedica solicitud) {
        validarTitulosDeOpciones(solicitud);
        if (repositorio.contarPorProfesional(idProfesional) >= MAXIMO_FICHAS_POR_PROFESIONAL) {
            throw new ExcepcionReglaNegocio("El profesional no puede tener más de cinco fichas médicas");
        }
        FichaMedica ficha = MapeadorFichaMedica.convertirAEntidad(idProfesional, solicitud);
        return MapeadorFichaMedica.convertirARespuesta(repositorio.save(ficha));
    }

    public List<RespuestaFichaMedica> buscarTodos(Long idProfesional) {
        return repositorio.buscarTodasPorProfesional(idProfesional).stream()
                .map(MapeadorFichaMedica::convertirARespuesta)
                .toList();
    }

    public RespuestaFichaMedica buscarPorId(Long idProfesional, Long idFicha) {
        return MapeadorFichaMedica.convertirARespuesta(buscarFichaDelProfesional(idProfesional, idFicha));
    }

    @Transactional
    public RespuestaFichaMedica actualizar(Long idProfesional, Long idFicha, SolicitudFichaMedica solicitud) {
        validarTitulosDeOpciones(solicitud);
        FichaMedica ficha = buscarFichaDelProfesional(idProfesional, idFicha);
        ficha.actualizar(solicitud.nombre().trim(), normalizar(solicitud.descripcion()));
        ficha.reemplazarDetalles(MapeadorFichaMedica.convertirADetalles(solicitud.detalles()));
        return MapeadorFichaMedica.convertirARespuesta(repositorio.save(ficha));
    }

    @Transactional
    public void eliminar(Long idProfesional, Long idFicha) {
        FichaMedica ficha = buscarFichaDelProfesional(idProfesional, idFicha);
        try {
            // Fuerza el DELETE dentro de este método para poder convertir una referencia clínica
            // existente en una respuesta de negocio, en lugar de dejar que falle al confirmar la transacción.
            repositorio.delete(ficha);
            repositorio.flush();
        } catch (DataIntegrityViolationException excepcion) {
            throw new ExcepcionReglaNegocio(
                    "La ficha médica no puede eliminarse porque ya fue utilizada en la historia clínica");
        }
    }

    private FichaMedica buscarFichaDelProfesional(Long idProfesional, Long idFicha) {
        return repositorio.buscarPorIdYProfesional(idFicha, idProfesional)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ficha médica no encontrada"));
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validarTitulosDeOpciones(SolicitudFichaMedica solicitud) {
        solicitud.detalles().stream()
                .flatMap(detalle -> detalle.campos().stream())
                .forEach(campo -> {
                    long cantidadSiNo = campo.opciones().stream()
                            .filter(opcion -> opcion.tipo() == TipoOpcion.SI_NO)
                            .count();
                    if (cantidadSiNo > 1) {
                        throw new ExcepcionReglaNegocio(
                                "Un campo puede contener como máximo una opción de tipo SI_NO"
                        );
                    }
                    boolean entradaUnica = campo.opciones().size() == 1
                            && campo.opciones().getFirst().tipo() == TipoOpcion.ENTRADA;
                    boolean existeTituloVacioNoPermitido = campo.opciones().stream()
                            .anyMatch(opcion -> (opcion.titulo() == null || opcion.titulo().isBlank())
                                    && opcion.tipo() != TipoOpcion.SI_NO
                                    && !(entradaUnica && opcion.tipo() == TipoOpcion.ENTRADA));
                    if (existeTituloVacioNoPermitido) {
                        throw new ExcepcionReglaNegocio(
                                "El título solo puede omitirse en SI_NO o en un campo con una única opción ENTRADA"
                        );
                    }
                });
    }
}
