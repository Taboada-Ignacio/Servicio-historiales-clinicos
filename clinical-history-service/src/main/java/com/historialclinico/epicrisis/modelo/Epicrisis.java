package com.historialclinico.epicrisis.modelo;

import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.fichamedica.modelo.FichaMedica;
import com.historialclinico.paciente.modelo.FichaPaciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "epicrisis")
public class Epicrisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ficha_seguimiento")
    private FichaMedica fichaSeguimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ficha_paciente_seguimiento")
    private FichaPaciente fichaPacienteSeguimiento;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Column(nullable = false, length = 1000)
    private String observaciones;

    protected Epicrisis() {}

    public Epicrisis(Paciente paciente, FichaMedica fichaSeguimiento, FichaPaciente fichaPacienteSeguimiento,
                     String observaciones) {
        this.paciente = paciente;
        this.fichaSeguimiento = fichaSeguimiento;
        this.fichaPacienteSeguimiento = fichaPacienteSeguimiento;
        this.observaciones = observaciones;
        this.fechaHora = Instant.now();
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public FichaMedica getFichaSeguimiento() { return fichaSeguimiento; }
    public FichaPaciente getFichaPacienteSeguimiento() { return fichaPacienteSeguimiento; }
    public Instant getFechaHora() { return fechaHora; }
    public String getObservaciones() { return observaciones; }
}
