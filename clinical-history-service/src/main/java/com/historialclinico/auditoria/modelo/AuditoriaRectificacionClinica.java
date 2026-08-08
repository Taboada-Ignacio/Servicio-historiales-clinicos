package com.historialclinico.auditoria.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auditoria_rectificaciones_clinicas")
public class AuditoriaRectificacionClinica {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_registro", nullable = false, length = 20)
    private TipoRegistroClinico tipoRegistro;
    @Column(name = "id_registro", nullable = false) private Long idRegistro;
    @Column(name = "id_paciente", nullable = false) private Long idPaciente;
    @Column(name = "id_profesional", nullable = false) private Long idProfesional;
    @Column(name = "nombre_profesional", length = 200) private String nombreProfesional;
    @Column(name = "matricula_profesional", length = 100) private String matriculaProfesional;
    @Column(name = "version_anterior", nullable = false) private int versionAnterior;
    @Column(name = "version_nueva", nullable = false) private int versionNueva;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_motivo", nullable = false, length = 40)
    private TipoMotivoRectificacion tipoMotivo;
    @Column(nullable = false, length = 500) private String motivo;
    @Column(nullable = false, length = 20) private String resultado;
    @Column(name = "fecha_hora", nullable = false) private Instant fechaHora;
    @Column(name = "conservar_hasta", nullable = false) private Instant conservarHasta;
    @Column(name = "ip_origen", nullable = false, length = 64) private String ipOrigen;
    @Column(nullable = false, length = 500) private String equipo;
    @Column(name = "id_sesion", length = 128) private String idSesion;
    @Column(name = "id_solicitud", nullable = false, length = 128) private String idSolicitud;
    @Column(name = "antes_cifrado", nullable = false) private byte[] antesCifrado;
    @Column(name = "antes_iv", nullable = false) private byte[] antesIv;
    @Column(name = "despues_cifrado", nullable = false) private byte[] despuesCifrado;
    @Column(name = "despues_iv", nullable = false) private byte[] despuesIv;
    @Column(name = "hash_antes", nullable = false, length = 64) private String hashAntes;
    @Column(name = "hash_despues", nullable = false, length = 64) private String hashDespues;
    @Column(name = "hash_anterior_cadena", length = 64) private String hashAnteriorCadena;
    @Column(name = "hash_cadena", nullable = false, length = 64) private String hashCadena;

    protected AuditoriaRectificacionClinica() {}

    public AuditoriaRectificacionClinica(TipoRegistroClinico tipoRegistro, Long idRegistro, Long idPaciente,
            Long idProfesional, String nombreProfesional, String matriculaProfesional, int versionAnterior,
            int versionNueva, TipoMotivoRectificacion tipoMotivo, String motivo, Instant fechaHora,
            Instant conservarHasta, String ipOrigen, String equipo, String idSesion, String idSolicitud,
            byte[] antesCifrado, byte[] antesIv, byte[] despuesCifrado, byte[] despuesIv,
            String hashAntes, String hashDespues, String hashAnteriorCadena, String hashCadena) {
        this.tipoRegistro = tipoRegistro; this.idRegistro = idRegistro; this.idPaciente = idPaciente;
        this.idProfesional = idProfesional; this.nombreProfesional = nombreProfesional;
        this.matriculaProfesional = matriculaProfesional; this.versionAnterior = versionAnterior;
        this.versionNueva = versionNueva; this.tipoMotivo = tipoMotivo; this.motivo = motivo;
        this.resultado = "EXITOSA"; this.fechaHora = fechaHora; this.conservarHasta = conservarHasta;
        this.ipOrigen = ipOrigen; this.equipo = equipo; this.idSesion = idSesion; this.idSolicitud = idSolicitud;
        this.antesCifrado = antesCifrado; this.antesIv = antesIv; this.despuesCifrado = despuesCifrado;
        this.despuesIv = despuesIv; this.hashAntes = hashAntes; this.hashDespues = hashDespues;
        this.hashAnteriorCadena = hashAnteriorCadena; this.hashCadena = hashCadena;
    }

    @PreUpdate @PreRemove
    private void impedirAlteracion() {
        throw new IllegalStateException("La auditoría clínica es inmutable");
    }

    public Long getId() { return id; }
    public TipoRegistroClinico getTipoRegistro() { return tipoRegistro; }
    public Long getIdRegistro() { return idRegistro; }
    public Long getIdPaciente() { return idPaciente; }
    public Long getIdProfesional() { return idProfesional; }
    public String getNombreProfesional() { return nombreProfesional; }
    public String getMatriculaProfesional() { return matriculaProfesional; }
    public int getVersionAnterior() { return versionAnterior; }
    public int getVersionNueva() { return versionNueva; }
    public TipoMotivoRectificacion getTipoMotivo() { return tipoMotivo; }
    public String getMotivo() { return motivo; }
    public String getResultado() { return resultado; }
    public Instant getFechaHora() { return fechaHora; }
    public Instant getConservarHasta() { return conservarHasta; }
    public String getIpOrigen() { return ipOrigen; }
    public String getEquipo() { return equipo; }
    public String getIdSesion() { return idSesion; }
    public String getIdSolicitud() { return idSolicitud; }
    public byte[] getAntesCifrado() { return antesCifrado.clone(); }
    public byte[] getAntesIv() { return antesIv.clone(); }
    public byte[] getDespuesCifrado() { return despuesCifrado.clone(); }
    public byte[] getDespuesIv() { return despuesIv.clone(); }
    public String getHashAntes() { return hashAntes; }
    public String getHashDespues() { return hashDespues; }
    public String getHashAnteriorCadena() { return hashAnteriorCadena; }
    public String getHashCadena() { return hashCadena; }
}

