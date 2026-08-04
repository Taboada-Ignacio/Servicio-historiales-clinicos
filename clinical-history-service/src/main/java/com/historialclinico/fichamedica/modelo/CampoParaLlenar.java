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
@Table(name = "campos_para_llenar")
public class CampoParaLlenar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_ficha", nullable = false)
    private DetalleFicha detalle;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private int orden;

    @Column(name = "permite_seleccion_multiple", nullable = false)
    private boolean permiteSeleccionMultiple;

    @OneToMany(mappedBy = "campo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<OpcionCampo> opciones = new ArrayList<>();

    protected CampoParaLlenar() {
    }

    public CampoParaLlenar(String titulo, String descripcion, int orden, boolean permiteSeleccionMultiple) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.orden = orden;
        this.permiteSeleccionMultiple = permiteSeleccionMultiple;
    }

    void asociarA(DetalleFicha detalle) { this.detalle = detalle; }

    public void agregarOpcion(OpcionCampo opcion) {
        opciones.add(opcion);
        opcion.asociarA(this);
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public int getOrden() { return orden; }
    public boolean isPermiteSeleccionMultiple() { return permiteSeleccionMultiple; }
    public List<OpcionCampo> getOpciones() { return opciones; }
}
