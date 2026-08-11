package com.historialclinico.archivo.modelo;

import com.historialclinico.paciente.modelo.Paciente;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "documentos_clinicos")
public class DocumentoClinico {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContextoDocumentoClinico contexto;

    @Column(name = "contexto_id", nullable = false)
    private Long contextoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CategoriaDocumentoClinico categoria;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDocumentoClinico estado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DocumentoClinicoVersion versionActual;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Column(name = "fecha_eliminacion")
    private Instant fechaEliminacion;

    @Column(name = "conservar_hasta")
    private Instant conservarHasta;

    @Column(name = "motivo_eliminacion", length = 500)
    private String motivoEliminacion;

    @Version
    @Column(name = "version_lock", nullable = false)
    private long versionLock;

    protected DocumentoClinico() {}

    public DocumentoClinico(UUID id, Paciente paciente, ContextoDocumentoClinico contexto, Long contextoId,
            CategoriaDocumentoClinico categoria, String descripcion, Instant fecha) {
        this.id = id;
        this.paciente = paciente;
        this.contexto = contexto;
        this.contextoId = contextoId;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.estado = EstadoDocumentoClinico.ACTIVE;
        this.fechaCreacion = fecha;
        this.fechaActualizacion = fecha;
    }

    public void asignarVersionActual(DocumentoClinicoVersion version, Instant fecha) {
        if (!id.equals(version.getDocumento().getId()) || version.getEstadoVersion() != EstadoVersionDocumento.CURRENT)
            throw new IllegalArgumentException("La versión actual debe pertenecer al documento y estar CURRENT");
        this.versionActual = version;
        this.fechaActualizacion = fecha;
    }

    public void eliminar(String motivo, Instant fecha, int retencionAnios) {
        if (estado == EstadoDocumentoClinico.DELETED) throw new IllegalStateException("El documento ya está eliminado");
        estado = EstadoDocumentoClinico.DELETED;
        motivoEliminacion = motivo;
        fechaEliminacion = fecha;
        conservarHasta = fecha.atZone(java.time.ZoneOffset.UTC).plusYears(retencionAnios).toInstant()
                .truncatedTo(ChronoUnit.MICROS);
        fechaActualizacion = fecha;
    }

    public void restaurar(Instant fecha) {
        if (estado == EstadoDocumentoClinico.ACTIVE) throw new IllegalStateException("El documento ya está activo");
        estado = EstadoDocumentoClinico.ACTIVE;
        fechaEliminacion = null;
        conservarHasta = null;
        motivoEliminacion = null;
        fechaActualizacion = fecha;
    }

    public UUID getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public ContextoDocumentoClinico getContexto() { return contexto; }
    public Long getContextoId() { return contextoId; }
    public CategoriaDocumentoClinico getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public EstadoDocumentoClinico getEstado() { return estado; }
    public DocumentoClinicoVersion getVersionActual() { return versionActual; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public Instant getFechaEliminacion() { return fechaEliminacion; }
    public Instant getConservarHasta() { return conservarHasta; }
    public String getMotivoEliminacion() { return motivoEliminacion; }
}
