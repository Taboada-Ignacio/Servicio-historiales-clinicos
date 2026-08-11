package com.historialclinico.archivo.modelo;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documentos_clinicos_versiones")
public class DocumentoClinicoVersion {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private DocumentoClinico documento;

    @Column(name = "numero_version", nullable = false)
    private int numeroVersion;
    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;
    @Column(nullable = false, length = 5)
    private String extension;
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
    @Column(name = "storage_key", nullable = false, length = 500, unique = true)
    private String storageKey;
    @Column(name = "integridad_hash", nullable = false, length = 64)
    private String integridadHash;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_version", nullable = false, length = 20)
    private EstadoVersionDocumento estadoVersion;
    @Column(name = "current_slot")
    private Short currentSlot;
    @Column(name = "motivo_cambio", length = 500)
    private String motivoCambio;
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    protected DocumentoClinicoVersion() {}

    public DocumentoClinicoVersion(UUID id, DocumentoClinico documento, int numeroVersion, String nombreOriginal,
            String extension, String mimeType, long sizeBytes, String storageKey, String integridadHash,
            String motivoCambio, Instant fechaCreacion) {
        this.id = id;
        this.documento = documento;
        this.numeroVersion = numeroVersion;
        this.nombreOriginal = nombreOriginal;
        this.extension = extension;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.integridadHash = integridadHash;
        this.estadoVersion = EstadoVersionDocumento.CURRENT;
        this.currentSlot = 1;
        this.motivoCambio = motivoCambio;
        this.fechaCreacion = fechaCreacion;
    }

    public void marcarHistorica() {
        estadoVersion = EstadoVersionDocumento.HISTORICAL;
        currentSlot = null;
    }

    public void marcarActual() {
        estadoVersion = EstadoVersionDocumento.CURRENT;
        currentSlot = 1;
    }

    public UUID getId() { return id; }
    public DocumentoClinico getDocumento() { return documento; }
    public int getNumeroVersion() { return numeroVersion; }
    public String getNombreOriginal() { return nombreOriginal; }
    public String getExtension() { return extension; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStorageKey() { return storageKey; }
    public String getIntegridadHash() { return integridadHash; }
    public EstadoVersionDocumento getEstadoVersion() { return estadoVersion; }
    public String getMotivoCambio() { return motivoCambio; }
    public Instant getFechaCreacion() { return fechaCreacion; }
}
