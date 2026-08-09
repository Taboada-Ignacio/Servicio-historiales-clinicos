package com.historialclinico.paciente.modelo;

import com.historialclinico.fichamedica.modelo.OpcionCampo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import com.historialclinico.fichamedica.modelo.TipoOpcion;
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
@Table(name = "respuestas_campo")
public class RespuestaCampo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_ficha_paciente", nullable = false)
    private FichaPaciente fichaPaciente;

    @Column(name = "id_opcion_origen", nullable = false) private Long idOpcionOrigen;
    @Column(name = "titulo_detalle", nullable = false, length = 150) private String tituloDetalle;
    @Column(name = "descripcion_detalle", length = 500) private String descripcionDetalle;
    @Column(name = "orden_detalle", nullable = false) private int ordenDetalle;
    @Column(name = "titulo_campo", nullable = false, length = 150) private String tituloCampo;
    @Column(name = "descripcion_campo", length = 500) private String descripcionCampo;
    @Column(name = "orden_campo", nullable = false) private int ordenCampo;
    @Column(name = "permite_seleccion_multiple", nullable = false) private boolean permiteSeleccionMultiple;
    @Column(name = "titulo_opcion", length = 150) private String tituloOpcion;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_opcion", nullable = false, length = 20) private TipoOpcion tipoOpcion;
    @Column(name = "descripcion_opcion", length = 500) private String descripcionOpcion;
    @Column(name = "orden_opcion", nullable = false) private int ordenOpcion;
    @Column(name = "grupo_exclusion", length = 80) private String grupoExclusion;

    @Column(length = 1000)
    private String valor;

    private Boolean seleccionada;

    protected RespuestaCampo() {}

    public RespuestaCampo(OpcionCampo opcion, String valor, Boolean seleccionada) {
        this.idOpcionOrigen = opcion.getId();
        this.tituloDetalle = opcion.getCampo().getDetalle().getTitulo();
        this.descripcionDetalle = opcion.getCampo().getDetalle().getDescripcion();
        this.ordenDetalle = opcion.getCampo().getDetalle().getOrden();
        this.tituloCampo = opcion.getCampo().getTitulo();
        this.descripcionCampo = opcion.getCampo().getDescripcion();
        this.ordenCampo = opcion.getCampo().getOrden();
        this.permiteSeleccionMultiple = opcion.getCampo().isPermiteSeleccionMultiple();
        this.tituloOpcion = opcion.getTitulo(); this.tipoOpcion = opcion.getTipo();
        this.descripcionOpcion = opcion.getDescripcion(); this.ordenOpcion = opcion.getOrden();
        this.grupoExclusion = opcion.getGrupoExclusion();
        this.valor = valor;
        this.seleccionada = seleccionada;
    }

    void asociarA(FichaPaciente fichaPaciente) { this.fichaPaciente = fichaPaciente; }

    public void actualizar(String valor, Boolean seleccionada) {
        this.valor = valor;
        this.seleccionada = seleccionada;
    }

    public Long getId() { return id; }
    public Long getIdOpcionOrigen() { return idOpcionOrigen; }
    public String getTituloDetalle() { return tituloDetalle; }
    public String getDescripcionDetalle() { return descripcionDetalle; }
    public int getOrdenDetalle() { return ordenDetalle; }
    public String getTituloCampo() { return tituloCampo; }
    public String getDescripcionCampo() { return descripcionCampo; }
    public int getOrdenCampo() { return ordenCampo; }
    public boolean isPermiteSeleccionMultiple() { return permiteSeleccionMultiple; }
    public String getTituloOpcion() { return tituloOpcion; }
    public TipoOpcion getTipoOpcion() { return tipoOpcion; }
    public String getDescripcionOpcion() { return descripcionOpcion; }
    public int getOrdenOpcion() { return ordenOpcion; }
    public String getGrupoExclusion() { return grupoExclusion; }
    public String getValor() { return valor; }
    public Boolean getSeleccionada() { return seleccionada; }
}
