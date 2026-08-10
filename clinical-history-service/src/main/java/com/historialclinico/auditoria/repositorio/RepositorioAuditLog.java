package com.historialclinico.auditoria.repositorio;

import com.historialclinico.auditoria.modelo.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositorioAuditLog extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
            String action, Long profesionalId, Long pacienteId);
}
