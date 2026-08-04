import { useEffect, useState } from 'react'
import { apiFichasMedicas } from './api/fichasMedicas.js'

const modulos = [
  { icono: 'FM', titulo: 'Fichas médicas', descripcion: 'Diseñá y administrá plantillas clínicas.', disponible: true },
  { icono: 'PA', titulo: 'Pacientes', descripcion: 'Información personal y datos de contacto.' },
  { icono: 'HC', titulo: 'Historias clínicas', descripcion: 'Evoluciones y antecedentes por paciente.' },
  { icono: 'TR', titulo: 'Tratamientos', descripcion: 'Sesiones, avances y observaciones.' },
  { icono: 'EP', titulo: 'Epicrisis', descripcion: 'Síntesis de episodios clínicos.' },
]

const nuevaOpcion = (orden = 0) => ({ titulo: '', tipo: 'SELECCION', descripcion: '', orden, grupoExclusion: '' })
const nuevoCampo = (orden = 0) => ({ titulo: '', descripcion: '', orden, permiteSeleccionMultiple: false, opciones: [nuevaOpcion()] })
const nuevoDetalle = (orden = 0) => ({ titulo: '', descripcion: '', orden, campos: [nuevoCampo()] })
const fichaVacia = () => ({ nombre: '', descripcion: '', detalles: [nuevoDetalle()] })

export default function App() {
  const [vista, setVista] = useState(window.location.pathname.includes('fichas-medicas') ? 'fichas' : 'inicio')

  const navegar = (destino) => {
    const ruta = destino === 'fichas' ? '/fichas-medicas' : '/'
    window.history.pushState({}, '', ruta)
    setVista(destino)
  }

  useEffect(() => {
    const volver = () => setVista(window.location.pathname.includes('fichas-medicas') ? 'fichas' : 'inicio')
    window.addEventListener('popstate', volver)
    return () => window.removeEventListener('popstate', volver)
  }, [])

  return (
    <div className="aplicacion">
      <header className="barra">
        <button className="marca" onClick={() => navegar('inicio')} aria-label="Ir al inicio">
          <span className="marca-icono">+</span>
          <span><strong>Clínica</strong><small>Gestión profesional</small></span>
        </button>
        <span className="entorno">Entorno de desarrollo</span>
      </header>
      {vista === 'inicio' ? <Inicio onAbrirFichas={() => navegar('fichas')} /> : <GestionFichas onVolver={() => navegar('inicio')} />}
    </div>
  )
}

function Inicio({ onAbrirFichas }) {
  return (
    <main>
      <section className="hero">
        <p className="sobrelinea">Panel clínico</p>
        <h1>Todo lo necesario para acompañar a tus pacientes.</h1>
        <p>Administrá la información clínica desde un espacio simple, ordenado y pensado para el trabajo cotidiano.</p>
      </section>
      <section className="contenido">
        <div className="titulo-seccion">
          <div><p className="sobrelinea">Módulos</p><h2>¿Qué querés gestionar?</h2></div>
          <span>1 de {modulos.length} disponible</span>
        </div>
        <div className="grilla-modulos">
          {modulos.map((modulo) => (
            <button key={modulo.titulo} className={`tarjeta-modulo ${modulo.disponible ? 'activa' : ''}`}
              onClick={modulo.disponible ? onAbrirFichas : undefined} disabled={!modulo.disponible}>
              <span className="icono-modulo">{modulo.icono}</span>
              <span><strong>{modulo.titulo}</strong><small>{modulo.descripcion}</small></span>
              <span className="estado-modulo">{modulo.disponible ? 'Abrir →' : 'Próximamente'}</span>
            </button>
          ))}
        </div>
      </section>
    </main>
  )
}

function GestionFichas({ onVolver }) {
  const [modo, setModo] = useState(null)
  const [idProfesional, setIdProfesional] = useState('1')
  const [fichas, setFichas] = useState([])
  const [fichaPrevia, setFichaPrevia] = useState(null)
  const [editando, setEditando] = useState(null)
  const [ficha, setFicha] = useState(fichaVacia)
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)

  const cargar = async () => {
    if (!idProfesional) return
    setCargando(true); setMensaje(null)
    try { setFichas(await apiFichasMedicas.listar(idProfesional)) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const guardar = async (evento) => {
    evento.preventDefault(); setCargando(true); setMensaje(null)
    try {
      if (editando) await apiFichasMedicas.actualizar(idProfesional, editando, ficha)
      else await apiFichasMedicas.crear(idProfesional, ficha)
      setMensaje({ tipo: 'exito', texto: editando ? 'Ficha actualizada correctamente.' : 'Ficha creada correctamente.' })
      setEditando(null); setFicha(fichaVacia())
      if (editando) { setModo('consultar'); await cargar() }
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const editar = (seleccionada) => {
    setEditando(seleccionada.id)
    setFicha({ nombre: seleccionada.nombre, descripcion: seleccionada.descripcion || '', detalles: seleccionada.detalles })
    setModo('editar')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const eliminar = async (seleccionada) => {
    if (!window.confirm(`¿Eliminar la ficha “${seleccionada.nombre}”?`)) return
    try { await apiFichasMedicas.eliminar(idProfesional, seleccionada.id); await cargar() }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
  }

  const abrirConsulta = () => {
    setModo('consultar')
    cargar()
  }

  const volverAlMenu = () => {
    setModo(null)
    setEditando(null)
    setFicha(fichaVacia())
    setFichaPrevia(null)
    setMensaje(null)
  }

  const verVistaPrevia = (seleccionada) => {
    setFichaPrevia(seleccionada)
    setModo('vista-previa')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <main className="contenido pagina-fichas">
      <button className="volver" onClick={modo === 'vista-previa' ? () => setModo('consultar') : modo ? volverAlMenu : onVolver}>← {modo === 'vista-previa' ? 'Volver a fichas consultadas' : modo ? 'Volver a opciones' : 'Volver al inicio'}</button>
      <div className="cabecera-pagina">
        <div><p className="sobrelinea">Configuración clínica</p><h1>Fichas médicas</h1><p>Creá hasta cinco plantillas por profesional.</p></div>
        <label className="profesional">ID del profesional<input type="number" min="1" value={idProfesional} onChange={(e) => setIdProfesional(e.target.value)} />{modo === 'consultar' && <button type="button" onClick={cargar}>Consultar</button>}</label>
      </div>
      {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
      {!modo && <section className="opciones-ficha">
        <button className="opcion-ficha principal" onClick={() => setModo('crear')}>
          <span className="paso">01</span><span className="simbolo-opcion">＋</span>
          <span><strong>Crear nueva ficha médica</strong><small>Diseñá una plantilla con secciones, campos y opciones.</small></span><b>Comenzar →</b>
        </button>
        <button className="opcion-ficha" onClick={abrirConsulta}>
          <span className="paso">02</span><span className="simbolo-opcion">⌕</span>
          <span><strong>Consultar fichas médicas</strong><small>Revisá, editá o eliminá las plantillas existentes.</small></span><b>Consultar →</b>
        </button>
      </section>}
      {(modo === 'crear' || modo === 'editar') && <div className="contenedor-editor">
        <EditorFicha ficha={ficha} setFicha={setFicha} editando={editando} cargando={cargando} onGuardar={guardar}
          onCancelar={volverAlMenu} />
      </div>}
      {modo === 'consultar' && <section className="panel listado listado-completo">
          <div className="encabezado-panel"><div><h2>Plantillas creadas</h2><p>{fichas.length} de 5 fichas</p></div><button className="boton-secundario" onClick={cargar}>Actualizar</button></div>
          {cargando && !fichas.length ? <p className="estado-vacio">Cargando fichas…</p> : fichas.length === 0 ? <p className="estado-vacio">Este profesional todavía no tiene fichas.</p> : fichas.map((item) => (
            <article className="ficha-lista" key={item.id}>
              <span className="numero-ficha">{String(item.id).padStart(2, '0')}</span>
              <div><h3>{item.nombre}</h3><p>{item.descripcion || 'Sin descripción'}</p><small>{item.detalles.length} secciones · versión {item.version}</small></div>
              <div className="acciones"><button className="vista" onClick={() => verVistaPrevia(item)}>Vista previa</button><button onClick={() => editar(item)}>Editar</button><button className="peligro" onClick={() => eliminar(item)}>Eliminar</button></div>
            </article>
          ))}
      </section>}
      {modo === 'vista-previa' && fichaPrevia && <VistaPreviaFicha ficha={fichaPrevia} onEditar={() => editar(fichaPrevia)} />}
    </main>
  )
}

function VistaPreviaFicha({ ficha, onEditar }) {
  return (
    <section className="panel vista-previa-ficha" aria-label={`Vista previa de ${ficha.nombre}`}>
      <header className="cabecera-vista">
        <div><p className="sobrelinea">Vista previa · solo lectura</p><h2>{ficha.nombre}</h2><p>{ficha.descripcion || 'Sin descripción'}</p></div>
        <div className="acciones-vista"><span>{ficha.detalles.length} secciones</span><button type="button" onClick={onEditar}>Editar plantilla</button></div>
      </header>
      <div className="cuerpo-vista">
        {ficha.detalles.map((detalle, indiceDetalle) => (
          <article className="fila-seccion" key={detalle.id || indiceDetalle}>
            <div className="nombre-seccion"><span>{String(indiceDetalle + 1).padStart(2, '0')}</span><div><h3>{detalle.titulo}</h3>{detalle.descripcion && <p>{detalle.descripcion}</p>}</div></div>
            <div className="campos-compactos">
              {detalle.campos.map((campo, indiceCampo) => (
                <div className="campo-previo" key={campo.id || indiceCampo}>
                  <div className="titulo-campo-previo"><strong>{campo.titulo}</strong>{campo.permiteSeleccionMultiple && <span>Múltiple</span>}</div>
                  {campo.descripcion && <p>{campo.descripcion}</p>}
                  <div className="respuestas-previas">
                    {campo.opciones.map((opcion, indiceOpcion) => (
                      <OpcionPrevia opcion={opcion} key={opcion.id || indiceOpcion} />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

function OpcionPrevia({ opcion }) {
  if (opcion.tipo === 'SI_NO') return <span className="respuesta-si-no"><label><input type="checkbox" disabled /> Sí</label><label><input type="checkbox" disabled /> No</label></span>
  if (opcion.tipo === 'ENTRADA') return <span className="respuesta-entrada">{opcion.titulo && <b>{opcion.titulo}</b>}<i>Espacio para completar</i></span>
  return <label className="respuesta-seleccion respuesta-seleccion-casilla"><input type="checkbox" disabled /><span>{opcion.titulo}</span></label>
}

function EditorFicha({ ficha, setFicha, editando, cargando, onGuardar, onCancelar }) {
  const cambiarDetalle = (indice, clave, valor) => setFicha({ ...ficha, detalles: ficha.detalles.map((d, i) => i === indice ? { ...d, [clave]: valor } : d) })
  const cambiarCampo = (di, ci, clave, valor) => cambiarDetalle(di, 'campos', ficha.detalles[di].campos.map((c, i) => i === ci ? { ...c, [clave]: valor } : c))
  const cambiarOpcion = (di, ci, oi, clave, valor) => cambiarCampo(di, ci, 'opciones', ficha.detalles[di].campos[ci].opciones.map((o, i) => i === oi ? { ...o, [clave]: valor } : o))
  const cambiarTipoOpcion = (di, ci, oi, tipo) => {
    cambiarCampo(di, ci, 'opciones', ficha.detalles[di].campos[ci].opciones.map((opcion, i) => i === oi ? { ...opcion, tipo, titulo: tipo === 'SI_NO' ? '' : opcion.titulo, grupoExclusion: tipo === 'SELECCION' ? opcion.grupoExclusion : '' } : opcion))
  }
  const quitarDetalle = (di) => setFicha({ ...ficha, detalles: ficha.detalles.filter((_, i) => i !== di).map((d, i) => ({ ...d, orden: i })) })
  const quitarCampo = (di, ci) => cambiarDetalle(di, 'campos', ficha.detalles[di].campos.filter((_, i) => i !== ci).map((c, i) => ({ ...c, orden: i })))
  const quitarOpcion = (di, ci, oi) => cambiarCampo(di, ci, 'opciones', ficha.detalles[di].campos[ci].opciones.filter((_, i) => i !== oi).map((o, i) => ({ ...o, orden: i })))

  return (
    <form className="panel editor" onSubmit={onGuardar}>
      <div className="encabezado-panel"><div><h2>{editando ? 'Editar plantilla' : 'Nueva plantilla'}</h2><p>Definí la estructura que completarás para cada paciente.</p></div>{editando && <button type="button" className="boton-secundario" onClick={onCancelar}>Cancelar</button>}</div>
      <label>Nombre de la ficha<input required maxLength="120" value={ficha.nombre} onChange={(e) => setFicha({ ...ficha, nombre: e.target.value })} placeholder="Ej. Historia clínica general" /></label>
      <label>Descripción <span>(opcional)</span><textarea maxLength="500" value={ficha.descripcion} onChange={(e) => setFicha({ ...ficha, descripcion: e.target.value })} placeholder="¿Para qué se utilizará esta ficha?" /></label>
      {ficha.detalles.map((detalle, di) => (
        <div className="detalle" key={di}>
          <div className="fila-titulo"><span>Sección {di + 1}</span>{ficha.detalles.length > 1 && <button type="button" onClick={() => quitarDetalle(di)}>Quitar sección</button>}</div>
          <input required value={detalle.titulo} onChange={(e) => cambiarDetalle(di, 'titulo', e.target.value)} placeholder="Título de la sección" aria-label={`Título de sección ${di + 1}`} />
          <input value={detalle.descripcion || ''} onChange={(e) => cambiarDetalle(di, 'descripcion', e.target.value)} placeholder="Descripción opcional" aria-label={`Descripción de sección ${di + 1}`} />
          {detalle.campos.map((campo, ci) => (
            <div className="campo" key={ci}>
              <div className="fila-titulo"><strong>Campo {ci + 1}</strong>{detalle.campos.length > 1 && <button type="button" onClick={() => quitarCampo(di, ci)}>Quitar</button>}</div>
              <input required value={campo.titulo} onChange={(e) => cambiarCampo(di, ci, 'titulo', e.target.value)} placeholder="Ej. Tabaquismo" aria-label={`Título de campo ${ci + 1}`} />
              <input value={campo.descripcion || ''} onChange={(e) => cambiarCampo(di, ci, 'descripcion', e.target.value)} placeholder="Descripción opcional" aria-label={`Descripción de campo ${ci + 1}`} />
              <label className="seleccion-multiple">
                <input type="checkbox" checked={Boolean(campo.permiteSeleccionMultiple)} onChange={(e) => cambiarCampo(di, ci, 'permiteSeleccionMultiple', e.target.checked)} />
                <span><strong>Permitir selección múltiple</strong><small>Solo afecta opciones de tipo “Seleccionar”. No modifica “Escribir” ni “Sí/No”.</small></span>
              </label>
              <div className="opciones">
                {campo.opciones.map((opcion, oi) => (
                  <div className="opcion" key={oi}>
                    {opcion.tipo === 'SI_NO' ? <span className="vista-si-no"><strong>Respuesta automática</strong><small>□ Sí&nbsp;&nbsp;&nbsp;□ No</small></span> : <input required={!(campo.opciones.length === 1 && opcion.tipo === 'ENTRADA')} value={opcion.titulo || ''} onChange={(e) => cambiarOpcion(di, ci, oi, 'titulo', e.target.value)} placeholder={campo.opciones.length === 1 && opcion.tipo === 'ENTRADA' ? 'Etiqueta opcional' : 'Título de opción'} aria-label={`Título de opción ${oi + 1}`} />}
                    <select value={opcion.tipo} onChange={(e) => cambiarTipoOpcion(di, ci, oi, e.target.value)} aria-label={`Tipo de opción ${oi + 1}`}><option value="SELECCION">Seleccionar</option><option value="ENTRADA">Escribir</option><option value="SI_NO" disabled={opcion.tipo !== 'SI_NO' && campo.opciones.some((otra, indice) => indice !== oi && otra.tipo === 'SI_NO')}>Sí / No</option></select>
                    {opcion.tipo === 'SELECCION' ? <input value={opcion.grupoExclusion || ''} onChange={(e) => cambiarOpcion(di, ci, oi, 'grupoExclusion', e.target.value)} placeholder="Grupo excluyente (opcional)" aria-label={`Grupo de exclusión ${oi + 1}`} /> : <span className="sin-grupo">No aplica grupo</span>}
                    {campo.opciones.length > 1 && <button type="button" onClick={() => quitarOpcion(di, ci, oi)} aria-label="Quitar opción">×</button>}
                  </div>
                ))}
                <button type="button" className="agregar" onClick={() => cambiarCampo(di, ci, 'opciones', [...campo.opciones, nuevaOpcion(campo.opciones.length)])}>+ Agregar opción</button>
              </div>
            </div>
          ))}
          <button type="button" className="agregar" onClick={() => cambiarDetalle(di, 'campos', [...detalle.campos, nuevoCampo(detalle.campos.length)])}>+ Agregar campo</button>
        </div>
      ))}
      <button type="button" className="agregar agregar-seccion" onClick={() => setFicha({ ...ficha, detalles: [...ficha.detalles, nuevoDetalle(ficha.detalles.length)] })}>+ Agregar sección</button>
      <button className="boton-principal" disabled={cargando}>{cargando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear ficha médica'}</button>
    </form>
  )
}
