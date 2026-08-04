package com.historialclinico.paciente.modelo;

import com.historialclinico.fichamedica.modelo.FichaMedica;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_paciente")
public class FichaPaciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ficha_medica", nullable = false)
    private FichaMedica fichaMedica;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private Instant fechaAsignacion;

    @OneToMany(mappedBy = "fichaPaciente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaCampo> respuestas = new ArrayList<>();

    protected FichaPaciente() {}

    public FichaPaciente(FichaMedica fichaMedica) {
        this.fichaMedica = fichaMedica;
        this.fechaAsignacion = Instant.now();
    }

    void asociarA(Paciente paciente) { this.paciente = paciente; }

    public void agregarRespuesta(RespuestaCampo respuesta) {
        respuestas.add(respuesta);
        respuesta.asociarA(this);
    }

    public Long getId() { return id; }
    public FichaMedica getFichaMedica() { return fichaMedica; }
    public Instant getFechaAsignacion() { return fechaAsignacion; }
    public List<RespuestaCampo> getRespuestas() { return respuestas; }
}
