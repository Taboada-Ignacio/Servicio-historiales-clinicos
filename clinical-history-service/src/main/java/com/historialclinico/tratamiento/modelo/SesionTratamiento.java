package com.historialclinico.tratamiento.modelo;

import com.historialclinico.fichamedica.modelo.FichaMedica;
import com.historialclinico.paciente.modelo.FichaPaciente;
import jakarta.persistence.*;
import java.time.Instant;
import com.historialclinico.auditoria.modelo.EstadoRegistroClinico;

@Entity
@Table(name = "sesiones_tratamiento")
public class SesionTratamiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tratamiento", nullable = false)
    private Tratamiento tratamiento;
    @Column(name = "nro_sesion", nullable = false)
    private int nroSesion;
    @Column(nullable = false, length = 1000)
    private String observaciones;
    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_ficha_seguimiento")
    private FichaMedica fichaSeguimiento;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "id_ficha_paciente_seguimiento")
    private FichaPaciente fichaPacienteSeguimiento;
    @Column(name = "version_clinica", nullable = false)
    private int versionClinica = 1;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_registro", nullable = false, length = 20)
    private EstadoRegistroClinico estadoRegistro = EstadoRegistroClinico.VIGENTE;
    @Column(name = "fecha_ultima_rectificacion")
    private Instant fechaUltimaRectificacion;

    protected SesionTratamiento() {}
    public SesionTratamiento(String observaciones, FichaMedica fichaSeguimiento, FichaPaciente fichaPacienteSeguimiento) {
        this.observaciones = observaciones; this.fichaSeguimiento = fichaSeguimiento;
        this.fichaPacienteSeguimiento = fichaPacienteSeguimiento; this.fechaHora = Instant.now();
    }
    void asociarA(Tratamiento tratamiento, int nroSesion) { this.tratamiento = tratamiento; this.nroSesion = nroSesion; }
    public Long getId() { return id; }
    public int getNroSesion() { return nroSesion; }
    public String getObservaciones() { return observaciones; }
    public Instant getFechaHora() { return fechaHora; }
    public FichaMedica getFichaSeguimiento() { return fichaSeguimiento; }
    public FichaPaciente getFichaPacienteSeguimiento() { return fichaPacienteSeguimiento; }
    public Tratamiento getTratamiento() { return tratamiento; }
    public int getVersionClinica() { return versionClinica; }
    public EstadoRegistroClinico getEstadoRegistro() { return estadoRegistro; }
    public Instant getFechaUltimaRectificacion() { return fechaUltimaRectificacion; }
    public void rectificar(String observaciones, FichaMedica fichaSeguimiento,
                           FichaPaciente fichaPacienteSeguimiento, boolean anular) {
        this.observaciones = observaciones;
        this.fichaSeguimiento = fichaSeguimiento;
        this.fichaPacienteSeguimiento = fichaPacienteSeguimiento;
        this.versionClinica++;
        this.estadoRegistro = anular ? EstadoRegistroClinico.ANULADO : EstadoRegistroClinico.RECTIFICADO;
        this.fechaUltimaRectificacion = Instant.now();
    }
}
