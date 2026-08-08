package com.historialclinico.auditoria.repositorio;

import com.historialclinico.auditoria.modelo.AuditoriaRectificacionClinica;
import com.historialclinico.auditoria.modelo.TipoRegistroClinico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositorioAuditoriaRectificacion extends JpaRepository<AuditoriaRectificacionClinica, Long> {
    Optional<AuditoriaRectificacionClinica> findFirstByTipoRegistroAndIdRegistroOrderByIdDesc(
            TipoRegistroClinico tipoRegistro, Long idRegistro);
    List<AuditoriaRectificacionClinica> findAllByIdProfesionalAndIdPacienteAndTipoRegistroAndIdRegistroOrderByIdAsc(
            Long idProfesional, Long idPaciente, TipoRegistroClinico tipoRegistro, Long idRegistro);
}

