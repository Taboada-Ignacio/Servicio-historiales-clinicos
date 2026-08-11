package com.historialclinico.auditoria.modelo;

import com.historialclinico.exportacion.modelo.FormatoExportacion;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import com.historialclinico.archivo.modelo.ContextoDocumentoClinico;

@Entity
@Table(name = "audit_log")
public class AuditLog {
    public static final String EXPORT_CLINICAL_HISTORY = "EXPORT_CLINICAL_HISTORY";
    public static final String FILE_UPLOAD = "FILE_UPLOAD";
    public static final String FILE_UPDATE = "FILE_UPDATE";
    public static final String FILE_DELETE = "FILE_DELETE";
    public static final String FILE_RESTORE = "FILE_RESTORE";
    public static final String FILE_VERSION_RESTORE = "FILE_VERSION_RESTORE";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50) private String action;
    @Column(name = "id_profesional", nullable = false) private Long profesionalId;
    @Column(name = "id_paciente", nullable = false) private Long pacienteId;
    @Enumerated(EnumType.STRING) @Column(length = 10) private FormatoExportacion formato;
    @Column(name = "fecha_hora", nullable = false) private Instant fechaHora;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ResultadoAuditLog resultado;
    @Column(name = "documento_id") private UUID documentoId;
    @Enumerated(EnumType.STRING) @Column(length = 20) private ContextoDocumentoClinico contexto;
    @Column(name = "contexto_id") private Long contextoId;
    @Column(length = 500) private String motivo;
    @Column(name = "version_anterior") private Integer versionAnterior;
    @Column(name = "version_restaurada") private Integer versionRestaurada;

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

    public AuditLog(String action, Long profesionalId, Long pacienteId, UUID documentoId,
            ContextoDocumentoClinico contexto, Long contextoId, String motivo, Instant fechaHora,
            Integer versionAnterior, Integer versionRestaurada, ResultadoAuditLog resultado) {
        this.action = action;
        this.profesionalId = profesionalId;
        this.pacienteId = pacienteId;
        this.documentoId = documentoId;
        this.contexto = contexto;
        this.contextoId = contextoId;
        this.motivo = motivo;
        this.versionAnterior = versionAnterior;
        this.versionRestaurada = versionRestaurada;
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
    public UUID getDocumentoId() { return documentoId; }
    public ContextoDocumentoClinico getContexto() { return contexto; }
    public Long getContextoId() { return contextoId; }
    public String getMotivo() { return motivo; }
    public Integer getVersionAnterior() { return versionAnterior; }
    public Integer getVersionRestaurada() { return versionRestaurada; }
}
