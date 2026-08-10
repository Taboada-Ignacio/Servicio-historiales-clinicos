package com.historialclinico.auditoria.servicio;

import com.historialclinico.auditoria.modelo.AuditLog;
import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.repositorio.RepositorioAuditLog;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ServicioAuditLog {
    private final RepositorioAuditLog repositorio;

    public ServicioAuditLog(RepositorioAuditLog repositorio) { this.repositorio = repositorio; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExportacion(Long profesionalId, Long pacienteId, FormatoExportacion formato,
            ResultadoAuditLog resultado) {
        repositorio.save(new AuditLog(AuditLog.EXPORT_CLINICAL_HISTORY, profesionalId, pacienteId,
                formato, Instant.now(), resultado));
    }
}
