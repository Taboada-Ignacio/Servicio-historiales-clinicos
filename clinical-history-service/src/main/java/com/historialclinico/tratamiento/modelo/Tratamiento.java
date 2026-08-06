package com.historialclinico.tratamiento.modelo;

import com.historialclinico.paciente.modelo.Paciente;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tratamientos")
public class Tratamiento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;
    @Column(nullable = false, length = 150)
    private String nombre;
    @Column(length = 1000)
    private String descripcion;
    @Column(name = "cantidad_sesiones_total", nullable = false)
    private int cantidadSesionesTotal;
    @Column(name = "cantidad_sesiones_faltantes", nullable = false)
    private int cantidadSesionesFaltantes;
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
    @OneToMany(mappedBy = "tratamiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("nroSesion ASC")
    private List<SesionTratamiento> sesiones = new ArrayList<>();

    protected Tratamiento() {}
    public Tratamiento(Paciente paciente, String nombre, String descripcion, int cantidadSesionesTotal) {
        this.paciente = paciente; this.nombre = nombre; this.descripcion = descripcion;
        this.cantidadSesionesTotal = cantidadSesionesTotal;
        this.cantidadSesionesFaltantes = cantidadSesionesTotal;
        this.fechaCreacion = Instant.now();
    }
    public void agregarSesion(SesionTratamiento sesion) {
        if (cantidadSesionesFaltantes == 0) throw new IllegalStateException("El tratamiento no tiene sesiones pendientes");
        sesiones.add(sesion); sesion.asociarA(this, cantidadSesionesTotal - cantidadSesionesFaltantes + 1);
        cantidadSesionesFaltantes--;
    }
    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getCantidadSesionesTotal() { return cantidadSesionesTotal; }
    public int getCantidadSesionesFaltantes() { return cantidadSesionesFaltantes; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public List<SesionTratamiento> getSesiones() { return sesiones; }
}
