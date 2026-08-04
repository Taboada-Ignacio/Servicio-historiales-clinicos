package com.historialclinico.fichamedica.modelo;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "detalles_ficha")
public class DetalleFicha {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ficha_medica", nullable = false)
    private FichaMedica fichaMedica;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @OneToMany(mappedBy = "detalle", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<CampoParaLlenar> campos = new ArrayList<>();

    protected DetalleFicha() {
    }

    public DetalleFicha(String titulo, String descripcion, int orden) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.orden = orden;
    }

    void asociarA(FichaMedica fichaMedica) { this.fichaMedica = fichaMedica; }

    public void agregarCampo(CampoParaLlenar campo) {
        campos.add(campo);
        campo.asociarA(this);
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public int getOrden() { return orden; }
    public List<CampoParaLlenar> getCampos() { return campos; }
}
