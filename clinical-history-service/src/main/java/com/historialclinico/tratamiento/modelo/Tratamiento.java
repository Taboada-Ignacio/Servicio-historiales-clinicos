package com.historialclinico.tratamiento.modelo;

import com.historialclinico.paciente.modelo.Paciente;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.historialclinico.auditoria.modelo.EstadoRegistroClinico;

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
    @Column(name = "version_clinica", nullable = false)
    private int versionClinica = 1;
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_registro", nullable = false, length = 20)
    private EstadoRegistroClinico estadoRegistro = EstadoRegistroClinico.VIGENTE;
    @Column(name = "fecha_ultima_rectificacion")
    private Instant fechaUltimaRectificacion;

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
    public int getVersionClinica() { return versionClinica; }
    public EstadoRegistroClinico getEstadoRegistro() { return estadoRegistro; }
    public Instant getFechaUltimaRectificacion() { return fechaUltimaRectificacion; }
    public void rectificar(String nombre, String descripcion, int cantidadSesionesTotal, boolean anular) {
        int realizadas = this.cantidadSesionesTotal - this.cantidadSesionesFaltantes;
        if (cantidadSesionesTotal < realizadas) {
            throw new IllegalArgumentException("La cantidad total no puede ser menor que las sesiones ya realizadas");
        }
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadSesionesTotal = cantidadSesionesTotal;
        this.cantidadSesionesFaltantes = cantidadSesionesTotal - realizadas;
        this.versionClinica++;
        this.estadoRegistro = anular ? EstadoRegistroClinico.ANULADO : EstadoRegistroClinico.RECTIFICADO;
        this.fechaUltimaRectificacion = Instant.now();
    }
}
