package com.historialclinico.exportacion.modelo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "exportaciones_historia_clinica")
public class ExportacionHistoriaClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_paciente", nullable = false) private Long pacienteId;
    @Column(name = "id_profesional", nullable = false) private Long profesionalId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private MotivoExportacion motivo;
    @Column(name = "detalle_motivo", length = 500) private String detalleMotivo;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_exportacion", nullable = false, length = 40)
    private TipoExportacion tipoExportacion;
    @Enumerated(EnumType.STRING) @Column(name = "formato_historia_clinica", nullable = false, length = 10)
    private FormatoExportacion formatoHistoriaClinica;
    @Enumerated(EnumType.STRING) @Column(name = "formato_archivo_final", nullable = false, length = 10)
    private FormatoArchivoFinal formatoArchivoFinal;
    @Column(name = "fecha_hora_exportacion", nullable = false) private Instant fechaHoraExportacion;
    @Column(name = "nombre_archivo", nullable = false, length = 255) private String nombreArchivo;
    @Column(name = "hash_archivo", nullable = false, length = 64) private String hashArchivo;

    protected ExportacionHistoriaClinica() {}

    public ExportacionHistoriaClinica(Long pacienteId, Long profesionalId, MotivoExportacion motivo,
            String detalleMotivo, FormatoExportacion formato, Instant fechaHoraExportacion,
            String nombreArchivo, String hashArchivo) {
        this(pacienteId, profesionalId, motivo, detalleMotivo, TipoExportacion.HISTORIA_CLINICA,
                formato, FormatoArchivoFinal.desde(formato), fechaHoraExportacion, nombreArchivo, hashArchivo);
    }

    public ExportacionHistoriaClinica(Long pacienteId, Long profesionalId, MotivoExportacion motivo,
            String detalleMotivo, TipoExportacion tipoExportacion, FormatoExportacion formatoHistoriaClinica,
            FormatoArchivoFinal formatoArchivoFinal, Instant fechaHoraExportacion,
            String nombreArchivo, String hashArchivo) {
        this.pacienteId = pacienteId;
        this.profesionalId = profesionalId;
        this.motivo = motivo;
        this.detalleMotivo = detalleMotivo;
        this.tipoExportacion = tipoExportacion;
        this.formatoHistoriaClinica = formatoHistoriaClinica;
        this.formatoArchivoFinal = formatoArchivoFinal;
        this.fechaHoraExportacion = fechaHoraExportacion;
        this.nombreArchivo = nombreArchivo;
        this.hashArchivo = hashArchivo;
    }

    @PreUpdate @PreRemove
    private void impedirAlteracion() { throw new IllegalStateException("El registro de exportación es inmutable"); }

    public Long getId() { return id; }
    public Long getPacienteId() { return pacienteId; }
    public Long getProfesionalId() { return profesionalId; }
    public MotivoExportacion getMotivo() { return motivo; }
    public String getDetalleMotivo() { return detalleMotivo; }
    public TipoExportacion getTipoExportacion() { return tipoExportacion; }
    public FormatoExportacion getFormatoHistoriaClinica() { return formatoHistoriaClinica; }
    public FormatoArchivoFinal getFormatoArchivoFinal() { return formatoArchivoFinal; }
    public FormatoExportacion getFormato() { return formatoHistoriaClinica; }
    public Instant getFechaHoraExportacion() { return fechaHoraExportacion; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getHashArchivo() { return hashArchivo; }
}
