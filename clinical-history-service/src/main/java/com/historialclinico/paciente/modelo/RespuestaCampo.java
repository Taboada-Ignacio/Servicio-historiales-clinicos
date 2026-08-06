package com.historialclinico.paciente.modelo;

import com.historialclinico.fichamedica.modelo.OpcionCampo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "respuestas_campo")
public class RespuestaCampo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ficha_paciente", nullable = false)
    private FichaPaciente fichaPaciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_opcion_campo", nullable = false)
    private OpcionCampo opcion;

    @Column(length = 1000)
    private String valor;

    private Boolean seleccionada;

    protected RespuestaCampo() {}

    public RespuestaCampo(OpcionCampo opcion, String valor, Boolean seleccionada) {
        this.opcion = opcion;
        this.valor = valor;
        this.seleccionada = seleccionada;
    }

    void asociarA(FichaPaciente fichaPaciente) { this.fichaPaciente = fichaPaciente; }

    public void actualizar(String valor, Boolean seleccionada) {
        this.valor = valor;
        this.seleccionada = seleccionada;
    }

    public Long getId() { return id; }
    public OpcionCampo getOpcion() { return opcion; }
    public String getValor() { return valor; }
    public Boolean getSeleccionada() { return seleccionada; }
}
