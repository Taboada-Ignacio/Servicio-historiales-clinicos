package com.historialclinico.auditoria.modelo;

import com.historialclinico.exportacion.modelo.FormatoExportacion;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    public static final String EXPORT_CLINICAL_HISTORY = "EXPORT_CLINICAL_HISTORY";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "id_profesional", nullable = false) private Long profesionalId;
    @Column(name = "id_paciente", nullable = false) private Long pacienteId;
    @Enumerated(EnumType.STRING) @Column(length = 10) private FormatoExportacion formato;
    @Column(name = "fecha_hora", nullable = false) private Instant fechaHora;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ResultadoAuditLog resultado;

    protected AuditLog() {}

    public AuditLog(String action, Long profesionalId, Long pacienteId, FormatoExportacion formato,
            Instant fechaHora, ResultadoAuditLog resultado) {
        this.action = action;
        this.profesionalId = profesionalId;
        this.pacienteId = pacienteId;
        this.formato = formato;
        this.fechaHora = fechaHora;
        this.resultado = resultado;
    }

    @PreUpdate @PreRemove
    private void impedirAlteracion() { throw new IllegalStateException("El AuditLog es inmutable"); }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public Long getProfesionalId() { return profesionalId; }
    public Long getPacienteId() { return pacienteId; }
    public FormatoExportacion getFormato() { return formato; }
    public Instant getFechaHora() { return fechaHora; }
    public ResultadoAuditLog getResultado() { return resultado; }
}
