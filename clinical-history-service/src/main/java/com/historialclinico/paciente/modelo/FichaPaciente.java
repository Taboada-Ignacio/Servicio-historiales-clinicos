package com.historialclinico.paciente.modelo;

import com.historialclinico.fichamedica.modelo.FichaMedica;
import jakarta.persistence.CascadeType;
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

    @Column(name = "id_plantilla_origen", nullable = false)
    private Long idPlantillaOrigen;

    @Column(name = "nombre_ficha", nullable = false, length = 120)
    private String nombreFicha;

    @Column(name = "descripcion_ficha", length = 500)
    private String descripcionFicha;

    @Column(name = "version_plantilla", nullable = false)
    private long versionPlantilla;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private Instant fechaAsignacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenFichaPaciente origen;

    @OneToMany(mappedBy = "fichaPaciente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RespuestaCampo> respuestas = new ArrayList<>();

    protected FichaPaciente() {}

    public FichaPaciente(FichaMedica fichaMedica) {
        this(fichaMedica, OrigenFichaPaciente.DIRECTA);
    }

    public FichaPaciente(FichaMedica fichaMedica, OrigenFichaPaciente origen) {
        this.idPlantillaOrigen = fichaMedica.getId();
        this.nombreFicha = fichaMedica.getNombre();
        this.descripcionFicha = fichaMedica.getDescripcion();
        this.versionPlantilla = fichaMedica.getVersion();
        this.origen = origen;
        this.fechaAsignacion = Instant.now();
    }

    void asociarA(Paciente paciente) { this.paciente = paciente; }

    public void agregarRespuesta(RespuestaCampo respuesta) {
        respuestas.add(respuesta);
        respuesta.asociarA(this);
    }

    public Long getId() { return id; }
    public Long getIdPlantillaOrigen() { return idPlantillaOrigen; }
    public String getNombreFicha() { return nombreFicha; }
    public String getDescripcionFicha() { return descripcionFicha; }
    public long getVersionPlantilla() { return versionPlantilla; }
    public Instant getFechaAsignacion() { return fechaAsignacion; }
    public OrigenFichaPaciente getOrigen() { return origen; }
    public List<RespuestaCampo> getRespuestas() { return respuestas; }
}
