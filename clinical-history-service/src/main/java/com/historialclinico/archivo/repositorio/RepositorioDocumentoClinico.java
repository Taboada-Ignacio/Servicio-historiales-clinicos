package com.historialclinico.archivo.repositorio;

import com.historialclinico.archivo.modelo.DocumentoClinico;
import com.historialclinico.archivo.modelo.EstadoDocumentoClinico;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDocumentoClinico extends JpaRepository<DocumentoClinico, UUID> {
    List<DocumentoClinico> findAllByPacienteIdAndPacienteIdProfesionalAndEstadoOrderByFechaCreacionDesc(
            Long pacienteId, Long profesionalId, EstadoDocumentoClinico estado);

    Optional<DocumentoClinico> findByIdAndPacienteIdProfesional(UUID id, Long profesionalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DocumentoClinico d where d.id = :id and d.paciente.idProfesional = :profesionalId")
    Optional<DocumentoClinico> buscarParaActualizar(@Param("id") UUID id,
            @Param("profesionalId") Long profesionalId);
}
