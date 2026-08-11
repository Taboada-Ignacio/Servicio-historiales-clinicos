package com.historialclinico.archivo.repositorio;

import com.historialclinico.archivo.modelo.DocumentoClinicoVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioDocumentoClinicoVersion extends JpaRepository<DocumentoClinicoVersion, UUID> {
    List<DocumentoClinicoVersion> findAllByDocumentoIdOrderByNumeroVersionDesc(UUID documentoId);
    Optional<DocumentoClinicoVersion> findByIdAndDocumentoId(UUID id, UUID documentoId);

    @Query("select coalesce(sum(v.sizeBytes), 0) from DocumentoClinicoVersion v "
            + "where v.documento.paciente.id = :pacienteId")
    long sumarBytesDelPaciente(@Param("pacienteId") Long pacienteId);

    @Query("select (count(v) > 0) from DocumentoClinicoVersion v "
            + "where v.documento.paciente.id = :pacienteId and v.integridadHash = :hash")
    boolean existeIntegridadEnPaciente(@Param("pacienteId") Long pacienteId, @Param("hash") String hash);

    @Query("select coalesce(max(v.numeroVersion), 0) from DocumentoClinicoVersion v where v.documento.id = :documentoId")
    int maximoNumeroVersion(@Param("documentoId") UUID documentoId);
}
