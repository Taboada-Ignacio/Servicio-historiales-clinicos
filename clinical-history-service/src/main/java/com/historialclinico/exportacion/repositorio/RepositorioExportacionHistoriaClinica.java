package com.historialclinico.exportacion.repositorio;

import com.historialclinico.exportacion.modelo.ExportacionHistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositorioExportacionHistoriaClinica extends JpaRepository<ExportacionHistoriaClinica, Long> {
    List<ExportacionHistoriaClinica> findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(
            Long profesionalId, Long pacienteId);

    Optional<ExportacionHistoriaClinica> findByIdAndProfesionalIdAndPacienteId(
            Long id, Long profesionalId, Long pacienteId);
}
