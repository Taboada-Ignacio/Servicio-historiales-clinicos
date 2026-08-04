package com.historialclinico.paciente.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_profesional", nullable = false)
    private Long idProfesional;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(nullable = false, length = 12)
    private String dni;

    @Column(length = 30)
    private String telefono;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sexo sexo;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    private long version;

    @OneToMany(mappedBy = "paciente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FichaPaciente> fichasAsignadas = new ArrayList<>();

    protected Paciente() {
    }

    public Paciente(Long idProfesional, String nombre, String apellido, String dni, String telefono,
                    LocalDate fechaNacimiento, Sexo sexo) {
        this.idProfesional = idProfesional;
        actualizarDatos(nombre, apellido, dni, telefono, fechaNacimiento, sexo);
        this.fechaCreacion = this.fechaActualizacion;
    }

    public void actualizar(String nombre, String apellido, String dni, String telefono,
                           LocalDate fechaNacimiento, Sexo sexo) {
        actualizarDatos(nombre, apellido, dni, telefono, fechaNacimiento, sexo);
    }

    public void asignarFicha(FichaPaciente fichaAsignada) {
        this.fichasAsignadas.add(fichaAsignada);
        fichaAsignada.asociarA(this);
    }

    private void actualizarDatos(String nombre, String apellido, String dni, String telefono,
                                 LocalDate fechaNacimiento, Sexo sexo) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.fechaActualizacion = Instant.now();
    }

    public Long getId() { return id; }
    public Long getIdProfesional() { return idProfesional; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getDni() { return dni; }
    public String getTelefono() { return telefono; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public Sexo getSexo() { return sexo; }
    public Instant getFechaCreacion() { return fechaCreacion; }
    public Instant getFechaActualizacion() { return fechaActualizacion; }
    public long getVersion() { return version; }
    public List<FichaPaciente> getFichasAsignadas() { return fichasAsignadas; }
}
