package com.historialclinico.fichamedica.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fichas_medicas")
public class FichaMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_profesional", nullable = false)
    private Long idProfesional;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    private long version;

    @OneToMany(mappedBy = "fichaMedica", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("orden ASC")
    private List<DetalleFicha> detalles = new ArrayList<>();

    protected FichaMedica() {
    }

    public FichaMedica(Long idProfesional, String nombre, String descripcion) {
        this.idProfesional = idProfesional;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaCreacion = Instant.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public void actualizar(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaActualizacion = Instant.now();
    }

    public void reemplazarDetalles(List<DetalleFicha> nuevosDetalles) {
        detalles.clear();
        nuevosDetalles.forEach(this::agregarDetalle);
        this.fechaActualizacion = Instant.now();
    }

    public void agregarDetalle(DetalleFicha detalle) {
        detalles.add(detalle);
        detalle.asociarA(this);
    }

    public Long getId() { return id; }
    public Long getIdProfesional() { return idProfesional; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
    public List<DetalleFicha> getDetalles() { return detalles; }
}
