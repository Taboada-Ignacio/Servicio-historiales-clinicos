package com.historialclinico.fichamedica.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "opciones_campo")
public class OpcionCampo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_campo_para_llenar", nullable = false)
    private CampoParaLlenar campo;

    @Column(length = 150)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_opcion", nullable = false, length = 20)
    private TipoOpcion tipo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @Column(name = "grupo_exclusion", length = 80)
    private String grupoExclusion;

    protected OpcionCampo() {
    }

    public OpcionCampo(String titulo, TipoOpcion tipo, String descripcion, int orden, String grupoExclusion) {
        this.titulo = titulo;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.orden = orden;
        this.grupoExclusion = grupoExclusion;
    }

    void asociarA(CampoParaLlenar campo) { this.campo = campo; }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public TipoOpcion getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public int getOrden() { return orden; }
    public String getGrupoExclusion() { return grupoExclusion; }
}
