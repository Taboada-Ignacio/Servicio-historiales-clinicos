import { useEffect, useState } from 'react'
import { apiFichasMedicas } from './api/fichasMedicas.js'
import { apiPacientes } from './api/pacientes.js'
import { apiEpicrisis } from './api/epicrisis.js'

const modulos = [
  { icono: 'FM', titulo: 'Fichas médicas', descripcion: 'Diseñá y administrá plantillas clínicas.', disponible: true },
  { icono: 'PA', titulo: 'Pacientes', descripcion: 'Información personal y datos de contacto.', disponible: true, destino: 'pacientes' },
  { icono: 'HC', titulo: 'Historias clínicas', descripcion: 'Evoluciones y antecedentes por paciente.' },
  { icono: 'TR', titulo: 'Tratamientos', descripcion: 'Sesiones, avances y observaciones.' },
  { icono: 'EP', titulo: 'Epicrisis', descripcion: 'Síntesis de episodios clínicos.', disponible: true, destino: 'epicrisis' },
]

const nuevaOpcion = (orden = 0) => ({ titulo: '', tipo: 'SELECCION', descripcion: '', orden, grupoExclusion: '' })
const nuevoCampo = (orden = 0) => ({ titulo: '', descripcion: '', orden, permiteSeleccionMultiple: false, opciones: [nuevaOpcion()] })
const nuevoDetalle = (orden = 0) => ({ titulo: '', descripcion: '', orden, campos: [nuevoCampo()] })
const fichaVacia = () => ({ nombre: '', descripcion: '', detalles: [nuevoDetalle()] })
const pacienteVacio = () => ({ nombre: '', apellido: '', dni: '', telefono: '', fechaNacimiento: '', sexo: '' })

const vistaDesdeRuta = () => {
  if (window.location.pathname.includes('fichas-medicas')) return 'fichas'
  if (window.location.pathname.includes('pacientes')) return 'pacientes'
  if (window.location.pathname.includes('epicrisis')) return 'epicrisis'
  return 'inicio'
}

export default function App() {
  const [vista, setVista] = useState(vistaDesdeRuta)

  const navegar = (destino) => {
    const rutas = { fichas: '/fichas-medicas', pacientes: '/pacientes', epicrisis: '/epicrisis', inicio: '/' }
    const ruta = rutas[destino]
    window.history.pushState({}, '', ruta)
    setVista(destino)
  }

  useEffect(() => {
    const volver = () => setVista(vistaDesdeRuta())
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
      {vista === 'inicio' && <Inicio onAbrirModulo={navegar} />}
      {vista === 'fichas' && <GestionFichas onVolver={() => navegar('inicio')} />}
      {vista === 'pacientes' && <GestionPacientes onVolver={() => navegar('inicio')} />}
      {vista === 'epicrisis' && <GestionEpicrisis onVolver={() => navegar('inicio')} />}
    </div>
  )
}

function GestionEpicrisis({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [idProfesional, setIdProfesional] = useState('1')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [observaciones, setObservaciones] = useState('')
  const [fichaSeguimiento, setFichaSeguimiento] = useState(null)
  const [respuestasSeguimiento, setRespuestasSeguimiento] = useState({})
  const [fichasDisponiblesSeguimiento, setFichasDisponiblesSeguimiento] = useState([])
  const [idFichaModal, setIdFichaModal] = useState('')
  const [modalFichaAbierto, setModalFichaAbierto] = useState(false)
  const [cargandoFichas, setCargandoFichas] = useState(false)
  const [cargando, setCargando] = useState(false)
  const [buscado, setBuscado] = useState(false)
  const [mensaje, setMensaje] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const filtrados = pacientes.filter((paciente) =>
    normalizar(`${paciente.apellido} ${paciente.nombre}`).includes(normalizar(busqueda)))

  const buscarPacientes = async () => {
    if (!idProfesional || !busqueda.trim()) return setMensaje({ tipo: 'error', texto: 'Ingresá apellido o nombre para buscar.' })
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiPacientes.listar(idProfesional)); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const confirmarPaciente = () => {
    if (!seleccionado) return
    if (!window.confirm(`¿Confirmás a ${seleccionado.apellido}, ${seleccionado.nombre} para registrar la epicrisis?`)) return
    setMensaje(null); setPantalla('registrar'); window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const abrirModalFicha = async () => {
    setModalFichaAbierto(true); setIdFichaModal(fichaSeguimiento ? String(fichaSeguimiento.id) : '')
    setCargandoFichas(true)
    try { setFichasDisponiblesSeguimiento(await apiFichasMedicas.listar(idProfesional)) }
    catch (error) { setModalFichaAbierto(false); setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargandoFichas(false) }
  }

  const agregarFichaSeguimiento = () => {
    const ficha = fichasDisponiblesSeguimiento.find((item) => String(item.id) === idFichaModal)
    if (!ficha) return
    const respuestas = {}
    ficha.detalles.flatMap((detalle) => detalle.campos).flatMap((campo) => campo.opciones).forEach((opcion) => {
      respuestas[opcion.id] = { idOpcion: opcion.id, valor: null, seleccionada: opcion.tipo === 'SELECCION' ? false : null }
    })
    setRespuestasSeguimiento(respuestas); setFichaSeguimiento(ficha); setModalFichaAbierto(false)
  }

  const registrar = async (evento) => {
    evento.preventDefault()
    if (!seleccionado) return setMensaje({ tipo: 'error', texto: 'Seleccioná un paciente.' })
    const confirmar = window.confirm(`¿Confirmás el registro de la epicrisis para ${seleccionado.apellido}, ${seleccionado.nombre}?`)
    if (!confirmar) return
    setCargando(true); setMensaje(null)
    try {
      await apiEpicrisis.registrar(idProfesional, seleccionado.id, {
        observaciones,
        idFichaSeguimiento: fichaSeguimiento?.id || null,
        respuestasFichaSeguimiento: fichaSeguimiento ? Object.values(respuestasSeguimiento) : null,
      })
      setObservaciones('')
      setPantalla('exito')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  if (pantalla === 'exito') return <main className="contenido pagina-fichas pagina-epicrisis">
    <section className="panel resultado-epicrisis-exitoso" role="status">
      <span className="icono-exito-epicrisis">✓</span>
      <p className="sobrelinea">Registro completado</p>
      <h1>Epicrisis registrada con éxito</h1>
      <p>La epicrisis de {seleccionado.apellido}, {seleccionado.nombre} fue guardada correctamente.</p>
      <button type="button" className="boton-principal" onClick={onVolver}>Volver al panel principal</button>
    </section>
  </main>

  if (pantalla === 'datos') return <main className="contenido pagina-fichas pagina-epicrisis">
    <button className="volver" onClick={() => { setPantalla('registrar'); window.scrollTo({ top: 0 }) }}>← Volver al registro de epicrisis</button>
    <VistaCompletaPaciente paciente={seleccionado} />
  </main>

  const volver = pantalla === 'registrar' ? () => { setPantalla('buscar'); setMensaje(null) } : onVolver

  return <main className="contenido pagina-fichas pagina-epicrisis">
    <button className="volver" onClick={volver}>← {pantalla === 'registrar' ? 'Volver a buscar pacientes' : 'Volver al inicio'}</button>
    <div className="cabecera-pagina">
      <div><p className="sobrelinea">Registro clínico</p><h1>Epicrisis</h1><p>{pantalla === 'buscar' ? 'Buscá y seleccioná al paciente.' : 'Registrá la síntesis del episodio clínico.'}</p></div>
      {pantalla === 'buscar' && <label className="profesional">ID del profesional<input type="number" min="1" required value={idProfesional} onChange={(e) => { setIdProfesional(e.target.value); setPacientes([]); setSeleccionado(null); setBuscado(false) }} /></label>}
    </div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis">
      <div className="titulo-paso"><h2>Buscar paciente</h2><p>Ingresá apellido o nombre para buscar coincidencias.</p></div>
      <div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscarPacientes() } }} placeholder="Ej. Pérez - Ana" /></label><button type="button" className="boton-principal" onClick={buscarPacientes} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div>
          <div className="resultados-pacientes-epicrisis">
            {!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !filtrados.length ? <p>No se encontraron pacientes que coincidan con “{busqueda}”.</p> : filtrados.map((paciente) => <label className={seleccionado?.id === paciente.id ? 'seleccionado' : ''} key={paciente.id}>
              <input type="radio" name="paciente-epicrisis" checked={seleccionado?.id === paciente.id} onChange={() => setSeleccionado(paciente)} />
              <span><strong>{paciente.apellido}, {paciente.nombre}</strong><small>DNI {paciente.dni}</small></span>
            </label>)}
          </div>
      <div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button type="button" className="boton-principal" disabled={!seleccionado} onClick={confirmarPaciente}>Confirmar paciente</button></div>
    </section>}
    {pantalla === 'registrar' && <form className="formulario-registro-epicrisis" onSubmit={registrar}>
      <section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div><button type="button" className="boton-secundario" onClick={() => { setPantalla('datos'); window.scrollTo({ top: 0 }) }}>Consultar todos los datos del paciente</button></section>
      <section className="panel seccion-ficha-seguimiento"><div><p className="sobrelinea">Seguimiento opcional</p><h2>Ficha para seguimiento</h2><p>Asociá una plantilla médica y completá sus datos para este paciente.</p></div>{fichaSeguimiento ? <div className="ficha-seguimiento-seleccionada"><span><strong>{fichaSeguimiento.nombre}</strong><small>{fichaSeguimiento.descripcion || `${fichaSeguimiento.detalles.length} secciones`}</small></span><button type="button" onClick={abrirModalFicha}>Cambiar</button><button type="button" className="quitar-ficha-seguimiento" onClick={() => { setFichaSeguimiento(null); setRespuestasSeguimiento({}) }}>Quitar</button></div> : <button type="button" className="boton-secundario" onClick={abrirModalFicha}>Agregar ficha de seguimiento</button>}</section>
      {fichaSeguimiento && <section className="panel ficha-seguimiento-completar">
        <AsignacionFicha fichas={[fichaSeguimiento]} cargando={false} idsSeleccionadas={[String(fichaSeguimiento.id)]}
          respuestas={respuestasSeguimiento} setRespuestas={setRespuestasSeguimiento} ocultarSelector
          onSeleccionar={() => {}} onActualizar={abrirModalFicha} />
      </section>}
      <section className="panel seccion-observaciones-epicrisis"><div className="titulo-paso"><h2>Observaciones</h2><p>Ingresá la síntesis clínica correspondiente a esta epicrisis.</p></div><label className="observaciones-epicrisis">Síntesis clínica<textarea autoFocus required maxLength="1000" rows="12" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Ingresá las observaciones de la epicrisis…" /><small>{observaciones.length} / 1000 caracteres</small></label><div className="acciones-epicrisis"><p>Se solicitará confirmación antes de guardar.</p><button className="boton-principal" disabled={cargando || !observaciones.trim()}>{cargando ? 'Registrando…' : 'Registrar epicrisis'}</button></div></section>
    </form>}
    {modalFichaAbierto && <div className="fondo-modal-ficha" role="presentation" onMouseDown={(e) => { if (e.target === e.currentTarget) setModalFichaAbierto(false) }}>
      <section className="modal-ficha-seguimiento" role="dialog" aria-modal="true" aria-labelledby="titulo-modal-ficha">
        <header><div><p className="sobrelinea">Plantillas disponibles</p><h2 id="titulo-modal-ficha">Agregar ficha de seguimiento</h2><p>Seleccioná una ficha médica del profesional.</p></div><button type="button" onClick={() => setModalFichaAbierto(false)} aria-label="Cerrar ventana">×</button></header>
        <div className="lista-modal-fichas">{cargandoFichas ? <p className="estado-modal-fichas">Cargando fichas…</p> : !fichasDisponiblesSeguimiento.length ? <p className="estado-modal-fichas">El profesional no tiene fichas médicas disponibles.</p> : fichasDisponiblesSeguimiento.map((ficha) => <label className={idFichaModal === String(ficha.id) ? 'seleccionada' : ''} key={ficha.id}><input type="radio" name="ficha-seguimiento" value={ficha.id} checked={idFichaModal === String(ficha.id)} onChange={() => setIdFichaModal(String(ficha.id))} /><span><strong>{ficha.nombre}</strong><small>{ficha.descripcion || `${ficha.detalles.length} secciones`}</small></span></label>)}</div>
        <footer><button type="button" className="boton-secundario" onClick={() => setModalFichaAbierto(false)}>Cancelar</button><button type="button" className="boton-principal" disabled={!idFichaModal || cargandoFichas} onClick={agregarFichaSeguimiento}>Agregar</button></footer>
      </section>
    </div>}
  </main>
}

function Inicio({ onAbrirModulo }) {
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
          <span>{modulos.filter((modulo) => modulo.disponible).length} de {modulos.length} disponibles</span>
        </div>
        <div className="grilla-modulos">
          {modulos.map((modulo) => (
            <button key={modulo.titulo} className={`tarjeta-modulo ${modulo.disponible ? 'activa' : ''}`}
              onClick={modulo.disponible ? () => onAbrirModulo(modulo.destino || 'fichas') : undefined} disabled={!modulo.disponible}>
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
      else setModo(null)
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

function GestionPacientes({ onVolver }) {
  const [modo, setModo] = useState(null)
  const [idProfesional, setIdProfesional] = useState('1')
  const [pacientes, setPacientes] = useState([])
  const [paciente, setPaciente] = useState(pacienteVacio)
  const [editando, setEditando] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [cargandoFichas, setCargandoFichas] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [fichasDisponibles, setFichasDisponibles] = useState([])
  const [idsFichasSeleccionadas, setIdsFichasSeleccionadas] = useState([])
  const [respuestasFicha, setRespuestasFicha] = useState({})
  const [fichasEdicion, setFichasEdicion] = useState([])
  const [idPlantillaNueva, setIdPlantillaNueva] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [pacienteSeleccionado, setPacienteSeleccionado] = useState(null)

  const normalizarBusqueda = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const pacientesFiltrados = pacientes.filter((item) =>
    normalizarBusqueda(`${item.apellido} - ${item.nombre}`).includes(normalizarBusqueda(busqueda)))

  const cargar = async () => {
    if (!idProfesional) return
    setCargando(true); setMensaje(null)
    try { setPacientes(await apiPacientes.listar(idProfesional)) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const guardar = async (evento) => {
    evento.preventDefault()
    const fichasSeleccionadas = fichasDisponibles.filter((ficha) => idsFichasSeleccionadas.includes(String(ficha.id)))
    if (!editando) {
      const detalleConfirmacion = fichasSeleccionadas.length
        ? ` con ${fichasSeleccionadas.length} ficha${fichasSeleccionadas.length === 1 ? '' : 's'} médica${fichasSeleccionadas.length === 1 ? '' : 's'} asignada${fichasSeleccionadas.length === 1 ? '' : 's'}`
        : ' sin fichas médicas asignadas'
      if (!window.confirm(`¿Confirmás el registro de ${paciente.nombre} ${paciente.apellido}${detalleConfirmacion}?`)) return
    }
    setCargando(true); setMensaje(null)
    try {
      const solicitud = { ...paciente }
      if (editando) solicitud.fichas = fichasEdicion.map((item) => ({
        idFichaPaciente: item.idFichaPaciente,
        idFichaMedica: item.plantilla.id,
        respuestas: Object.values(item.respuestas),
      }))
      else if (fichasSeleccionadas.length) solicitud.fichas = fichasSeleccionadas.map((ficha) => {
        const idsOpciones = new Set(ficha.detalles.flatMap((detalle) => detalle.campos.flatMap((campo) => campo.opciones.map((opcion) => String(opcion.id)))))
        return { idFichaMedica: ficha.id, respuestas: Object.entries(respuestasFicha)
          .filter(([idOpcion]) => idsOpciones.has(idOpcion)).map(([, respuesta]) => respuesta) }
      })
      if (editando) await apiPacientes.actualizar(idProfesional, editando, solicitud)
      else await apiPacientes.crear(idProfesional, solicitud)
      const textoExito = editando ? 'Paciente actualizado correctamente.' : 'Paciente creado correctamente.'
      setEditando(null); setPaciente(pacienteVacio())
      if (editando) { setModo('consultar'); await cargar() }
      else setModo(null)
      setMensaje({ tipo: 'exito', texto: textoExito })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const editar = async (seleccionado) => {
    setPaciente({
      nombre: seleccionado.nombre,
      apellido: seleccionado.apellido,
      dni: seleccionado.dni,
      telefono: seleccionado.telefono || '',
      fechaNacimiento: seleccionado.fechaNacimiento,
      sexo: seleccionado.sexo,
    })
    setEditando(seleccionado.id)
    setModo('editar')
    setMensaje(null)
    setCargandoFichas(true)
    try {
      const plantillas = await apiFichasMedicas.listar(idProfesional)
      setFichasDisponibles(plantillas)
      setFichasEdicion((seleccionado.fichas || []).map((fichaPaciente) => {
        const plantilla = plantillas.find((item) => item.id === fichaPaciente.idFichaMedica)
        const respuestas = Object.fromEntries(fichaPaciente.respuestas.map((respuesta) => [respuesta.idOpcion, {
          idOpcion: respuesta.idOpcion,
          valor: respuesta.tipo === 'ENTRADA' && respuesta.valor === 'No aplica' ? '' : respuesta.valor,
          seleccionada: respuesta.seleccionada,
        }]))
        return { clave: `existente-${fichaPaciente.id}`, idFichaPaciente: fichaPaciente.id, plantilla, respuestas }
      }).filter((item) => item.plantilla))
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargandoFichas(false) }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const volverAlMenu = () => {
    setModo(null)
    setEditando(null)
    setPaciente(pacienteVacio())
    setMensaje(null)
    setIdsFichasSeleccionadas([])
    setRespuestasFicha({})
    setFichasEdicion([])
    setIdPlantillaNueva('')
    setPacienteSeleccionado(null)
  }

  const abrirConsulta = () => {
    setModo('consultar')
    setBusqueda('')
    cargar()
  }

  const verPaciente = (seleccionado) => {
    setPacienteSeleccionado(seleccionado)
    setModo('vista-paciente')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cargarFichasDisponibles = async () => {
    if (!idProfesional) return
    setCargandoFichas(true); setMensaje(null)
    try { setFichasDisponibles(await apiFichasMedicas.listar(idProfesional)) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargandoFichas(false) }
  }

  const abrirRegistro = () => {
    setModo('crear')
    setPaciente(pacienteVacio())
    setIdsFichasSeleccionadas([])
    setRespuestasFicha({})
    cargarFichasDisponibles()
  }

  const seleccionarFicha = (idFicha, seleccionada) => {
    const ficha = fichasDisponibles.find((item) => String(item.id) === idFicha)
    if (!ficha) return
    if (!seleccionada) {
      const idsOpciones = new Set(ficha.detalles.flatMap((detalle) => detalle.campos.flatMap((campo) => campo.opciones.map((opcion) => String(opcion.id)))))
      setIdsFichasSeleccionadas((actuales) => actuales.filter((id) => id !== idFicha))
      setRespuestasFicha((actuales) => Object.fromEntries(Object.entries(actuales).filter(([id]) => !idsOpciones.has(id))))
      return
    }
    setIdsFichasSeleccionadas((actuales) => [...actuales, idFicha])
    const respuestas = { ...respuestasFicha }
    ficha.detalles.forEach((detalle) => detalle.campos.forEach((campo) => campo.opciones.forEach((opcion) => {
      respuestas[opcion.id] = { idOpcion: opcion.id, valor: null, seleccionada: opcion.tipo === 'SELECCION' ? false : null }
    })))
    setRespuestasFicha(respuestas)
  }

  const agregarFichaEnEdicion = () => {
    const plantilla = fichasDisponibles.find((item) => String(item.id) === idPlantillaNueva)
    if (!plantilla) return
    const respuestas = {}
    plantilla.detalles.forEach((detalle) => detalle.campos.forEach((campo) => campo.opciones.forEach((opcion) => {
      respuestas[opcion.id] = { idOpcion: opcion.id, valor: null, seleccionada: opcion.tipo === 'SELECCION' ? false : null }
    })))
    setFichasEdicion((actuales) => [...actuales, {
      clave: `nueva-${Date.now()}-${actuales.length}`,
      idFichaPaciente: null,
      plantilla,
      respuestas,
    }])
    setIdPlantillaNueva('')
  }

  const eliminar = async (seleccionado) => {
    if (!window.confirm(`¿Eliminar al paciente “${seleccionado.apellido}, ${seleccionado.nombre}”?`)) return
    setCargando(true); setMensaje(null)
    try {
      await apiPacientes.eliminar(idProfesional, seleccionado.id)
      await cargar()
      setMensaje({ tipo: 'exito', texto: 'Paciente eliminado correctamente.' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  return (
    <main className="contenido pagina-fichas pagina-pacientes">
      <button className="volver" onClick={modo === 'vista-paciente' ? () => setModo('consultar') : modo ? volverAlMenu : onVolver}>← {modo === 'vista-paciente' ? 'Volver a pacientes consultados' : modo ? 'Volver a opciones' : 'Volver al inicio'}</button>
      <div className="cabecera-pagina">
        <div><p className="sobrelinea">Gestión clínica</p><h1>Pacientes</h1><p>Administrá los pacientes asociados a cada profesional.</p></div>
        <label className="profesional">ID del profesional<input type="number" min="1" required value={idProfesional} onChange={(e) => { setIdProfesional(e.target.value); setFichasDisponibles([]); setIdsFichasSeleccionadas([]); setRespuestasFicha({}) }} />{modo === 'consultar' && <button type="button" onClick={cargar}>Consultar</button>}</label>
      </div>
      {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
      {!modo && <section className="opciones-ficha">
        <button className="opcion-ficha principal" onClick={abrirRegistro}>
          <span className="paso">01</span><span className="simbolo-opcion">＋</span>
          <span><strong>Registrar nuevo paciente</strong><small>Cargá sus datos personales y de contacto.</small></span><b>Comenzar →</b>
        </button>
        <button className="opcion-ficha" onClick={abrirConsulta}>
          <span className="paso">02</span><span className="simbolo-opcion">⌕</span>
          <span><strong>Consultar mis pacientes</strong><small>Revisá, editá o eliminá los pacientes registrados.</small></span><b>Consultar →</b>
        </button>
      </section>}
      {(modo === 'crear' || modo === 'editar') && <div className="contenedor-editor contenedor-paciente">
        <form className="panel editor editor-paciente" onSubmit={guardar}>
          <div className="encabezado-panel">
            <div><h2>{editando ? 'Editar paciente' : 'Nuevo paciente'}</h2><p>{editando ? 'Actualizá sus datos personales y las respuestas de sus fichas médicas.' : 'Completá sus datos personales.'}</p></div>
            {editando && <button type="button" className="boton-secundario" onClick={volverAlMenu}>Cancelar</button>}
          </div>
          <div className="grilla-formulario">
            <label>Nombre<input required maxLength="100" value={paciente.nombre} onChange={(e) => setPaciente({ ...paciente, nombre: e.target.value })} /></label>
            <label>Apellido<input required maxLength="100" value={paciente.apellido} onChange={(e) => setPaciente({ ...paciente, apellido: e.target.value })} /></label>
            <label>DNI<input required inputMode="numeric" pattern="[0-9]{6,12}" maxLength="12" value={paciente.dni} onChange={(e) => setPaciente({ ...paciente, dni: e.target.value.replace(/\D/g, '') })} /></label>
            <label>Teléfono <span>(opcional, solo números)</span><input type="tel" inputMode="numeric" pattern="[0-9]*" maxLength="30" value={paciente.telefono} onChange={(e) => setPaciente({ ...paciente, telefono: e.target.value.replace(/\D/g, '') })} /></label>
            <label>Fecha de nacimiento<input required type="date" max={new Date().toISOString().slice(0, 10)} value={paciente.fechaNacimiento} onChange={(e) => setPaciente({ ...paciente, fechaNacimiento: e.target.value })} /></label>
            <label>Sexo<select required value={paciente.sexo} onChange={(e) => setPaciente({ ...paciente, sexo: e.target.value })}>
              <option value="">Seleccionar</option><option value="FEMENINO">Femenino</option><option value="MASCULINO">Masculino</option><option value="OTRO">Otro</option><option value="NO_ESPECIFICA">Prefiere no especificar</option>
            </select></label>
          </div>
          {!editando && <AsignacionFicha fichas={fichasDisponibles} cargando={cargandoFichas}
            idsSeleccionadas={idsFichasSeleccionadas} respuestas={respuestasFicha}
            onSeleccionar={seleccionarFicha} setRespuestas={setRespuestasFicha}
            onActualizar={cargarFichasDisponibles} />}
          {editando && <section className="edicion-fichas-paciente">
            <div className="encabezado-asignacion"><div><h3>Fichas médicas del paciente</h3><p>Editá las respuestas, agregá una nueva ficha o quitá una ficha asociada.</p></div></div>
            <div className="agregar-ficha-edicion">
              <label>Nueva ficha médica<select value={idPlantillaNueva} onChange={(e) => setIdPlantillaNueva(e.target.value)}>
                <option value="">Seleccionar plantilla</option>
                {fichasDisponibles.map((ficha) => <option value={ficha.id} key={ficha.id}>{ficha.nombre}</option>)}
              </select></label>
              <button type="button" className="boton-secundario" disabled={!idPlantillaNueva} onClick={agregarFichaEnEdicion}>Agregar ficha</button>
            </div>
            {cargandoFichas ? <p className="aviso-ficha">Cargando fichas médicas…</p> : !fichasEdicion.length ? <p className="aviso-ficha">Este paciente no tiene fichas médicas asignadas.</p> : fichasEdicion.map((item) =>
              <div className="ficha-editable-paciente" key={item.clave}>
                <button type="button" className="quitar-ficha-paciente" onClick={() => setFichasEdicion((actuales) => actuales.filter((actual) => actual.clave !== item.clave))}>Eliminar ficha asociada</button>
                <AsignacionFicha fichas={[item.plantilla]} cargando={false}
                  idsSeleccionadas={[String(item.plantilla.id)]} respuestas={item.respuestas} ocultarSelector
                  onSeleccionar={() => {}} onActualizar={() => {}}
                  setRespuestas={(respuestas) => setFichasEdicion((actuales) => actuales.map((actual) =>
                    actual.clave === item.clave ? { ...actual, respuestas } : actual))} />
              </div>)}
          </section>}
          <button className="boton-principal" disabled={cargando || !idProfesional}>{cargando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear paciente'}</button>
        </form>
      </div>}
      {modo === 'consultar' && <section className="panel listado listado-completo listado-pacientes">
          <div className="encabezado-panel"><div><h2>Pacientes registrados</h2><p>{pacientes.length} pacientes</p></div><button className="boton-secundario" onClick={cargar} disabled={cargando}>Actualizar</button></div>
          {pacientes.length > 0 && <label className="buscador-pacientes"><span>Buscar por apellido - nombre</span><input type="search" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Ej. Pérez - Ana" /></label>}
          {cargando && !pacientes.length ? <p className="estado-vacio">Cargando pacientes…</p> : pacientes.length === 0 ? <p className="estado-vacio">Este profesional todavía no tiene pacientes.</p> : pacientesFiltrados.length === 0 ? <p className="estado-vacio">No se encontraron pacientes para “{busqueda}”.</p> : pacientesFiltrados.map((item) => (
            <article className="ficha-lista paciente-lista" key={item.id}>
              <span className="numero-ficha">{item.nombre.charAt(0)}{item.apellido.charAt(0)}</span>
              <div><h3>{item.apellido}, {item.nombre}</h3><p>DNI {item.dni}{item.telefono ? ` · ${item.telefono}` : ''}</p><small>Nacimiento: {new Date(`${item.fechaNacimiento}T00:00:00`).toLocaleDateString('es-AR')} · {item.fichas?.length || 0} fichas médicas</small></div>
              <div className="acciones"><button type="button" className="vista" onClick={() => verPaciente(item)}>Ver todos los datos</button><button type="button" onClick={() => editar(item)}>Editar</button><button type="button" className="peligro" onClick={() => eliminar(item)}>Eliminar</button></div>
            </article>
          ))}
      </section>}
      {modo === 'vista-paciente' && pacienteSeleccionado && <VistaCompletaPaciente paciente={pacienteSeleccionado} onEditar={() => editar(pacienteSeleccionado)} />}
    </main>
  )
}

function VistaCompletaPaciente({ paciente, onEditar }) {
  const sexo = { FEMENINO: 'Femenino', MASCULINO: 'Masculino', OTRO: 'Otro', NO_ESPECIFICA: 'Prefiere no especificar' }
  const fecha = (valor) => valor ? new Date(valor.includes('T') ? valor : `${valor}T00:00:00`).toLocaleString('es-AR', valor.includes('T') ? {} : { dateStyle: 'long' }) : 'No informado'

  return <section className="vista-completa-paciente">
    <header className="panel cabecera-paciente-completo">
      <div><p className="sobrelinea">Paciente #{paciente.id}</p><h2>{paciente.apellido}, {paciente.nombre}</h2><p>DNI {paciente.dni}</p></div>
      {onEditar && <button type="button" className="boton-principal boton-editar-paciente" onClick={onEditar}>Editar datos personales</button>}
    </header>
    <section className="panel datos-paciente-completo">
      <div className="encabezado-panel"><div><h2>Datos personales</h2><p>Información registrada del paciente.</p></div></div>
      <dl className="grilla-datos-paciente">
        <div><dt>Nombre</dt><dd>{paciente.nombre}</dd></div><div><dt>Apellido</dt><dd>{paciente.apellido}</dd></div>
        <div><dt>DNI</dt><dd>{paciente.dni}</dd></div><div><dt>Teléfono</dt><dd>{paciente.telefono || 'No informado'}</dd></div>
        <div><dt>Fecha de nacimiento</dt><dd>{fecha(paciente.fechaNacimiento)}</dd></div><div><dt>Sexo</dt><dd>{sexo[paciente.sexo] || paciente.sexo}</dd></div>
        <div><dt>Fecha de registro</dt><dd>{fecha(paciente.fechaCreacion)}</dd></div><div><dt>Última actualización</dt><dd>{fecha(paciente.fechaActualizacion)}</dd></div>
      </dl>
    </section>
    <section className="fichas-paciente-completo">
      <div className="titulo-fichas-paciente"><div><p className="sobrelinea">Información clínica</p><h2>Fichas médicas cargadas</h2></div><span>{paciente.fichas?.length || 0} fichas</span></div>
      {!paciente.fichas?.length ? <div className="panel estado-vacio">Este paciente no tiene fichas médicas asignadas.</div> : paciente.fichas.map((ficha) => <FichaPacienteCompleta ficha={ficha} key={ficha.id} />)}
    </section>
  </section>
}

function FichaPacienteCompleta({ ficha }) {
  const secciones = ficha.respuestas.reduce((resultado, respuesta) => {
    const seccion = respuesta.tituloDetalle || 'Sección'
    const campo = respuesta.tituloCampo || 'Campo'
    resultado[seccion] ||= {}
    resultado[seccion][campo] ||= []
    resultado[seccion][campo].push(respuesta)
    return resultado
  }, {})
  const claseCampo = (respuestas) => {
    const soloEscritura = respuestas.length === 1 && respuestas[0].tipo === 'ENTRADA'
    const tieneSiNo = respuestas.some((respuesta) => respuesta.tipo === 'SI_NO')
    if (soloEscritura) return 'campo-formulario campo-formulario--linea'
    if (respuestas.length >= 5) return 'campo-formulario campo-formulario--grupo'
    if (tieneSiNo) return 'campo-formulario campo-formulario--binario'
    return 'campo-formulario'
  }
  return <article className="panel vista-previa-ficha ficha-paciente-completa ficha-hoja">
    <header className="cabecera-vista"><div><p className="sobrelinea">Ficha del paciente · solo lectura</p><h3>{ficha.nombreFicha}</h3><p>Asignada el {new Date(ficha.fechaAsignacion).toLocaleString('es-AR')}</p></div><div className="acciones-vista"><span>{Object.keys(secciones).length} secciones</span></div></header>
    <div className="cuerpo-vista">{Object.entries(secciones).map(([seccion, campos], indiceSeccion) => <article className="fila-seccion seccion-formulario" key={seccion}>
      <div className="nombre-seccion cabecera-seccion-formulario"><span>{String(indiceSeccion + 1).padStart(2, '0')}</span><div><h3>{seccion}</h3></div></div>
      <div className="campos-compactos rejilla-formulario">{Object.entries(campos).map(([campo, respuestas]) => <div className={claseCampo(respuestas)} key={campo}>
        <div className="titulo-campo-previo"><strong>{campo}</strong></div>
        <div className="respuestas-previas">{respuestas.map((respuesta) => <RespuestaPacienteVista respuesta={respuesta} key={respuesta.id} />)}</div>
      </div>)}</div>
    </article>)}</div>
  </article>
}

function RespuestaPacienteVista({ respuesta }) {
  if (respuesta.tipo === 'SI_NO') return <span className="respuesta-si-no respuesta-clinica-si-no">
    <label><input type="checkbox" checked={respuesta.valor === 'SI'} disabled /> Sí</label>
    <label><input type="checkbox" checked={respuesta.valor === 'NO'} disabled /> No</label>
  </span>
  if (respuesta.tipo === 'ENTRADA') return <span className="respuesta-entrada respuesta-entrada-cargada">
    {respuesta.tituloOpcion && <b>{respuesta.tituloOpcion}</b>}<i>{respuesta.valor || 'No aplica'}</i>
  </span>
  return <label className={`respuesta-seleccion respuesta-seleccion-casilla ${respuesta.seleccionada ? 'marcada' : ''}`}>
    <input type="checkbox" checked={Boolean(respuesta.seleccionada)} disabled /><span>{respuesta.tituloOpcion}</span>
  </label>
}

function AsignacionFicha({ fichas, cargando, idsSeleccionadas, respuestas, onSeleccionar, setRespuestas, onActualizar, ocultarSelector = false }) {
  const seleccionadas = fichas.filter((item) => idsSeleccionadas.includes(String(item.id)))
  const claseCampo = (campo) => {
    const opciones = campo.opciones || []
    const soloEscritura = opciones.length === 1 && opciones[0].tipo === 'ENTRADA'
    const tieneSiNo = opciones.some((opcion) => opcion.tipo === 'SI_NO')
    if (soloEscritura) return 'campo-respuesta campo-formulario campo-formulario--linea'
    if (opciones.length >= 5) return 'campo-respuesta campo-formulario campo-formulario--grupo'
    if (tieneSiNo) return 'campo-respuesta campo-formulario campo-formulario--binario'
    return 'campo-respuesta campo-formulario'
  }

  const responder = (opcion, campo, cambios) => {
    const siguientes = { ...respuestas, [opcion.id]: { ...respuestas[opcion.id], ...cambios } }
    if (opcion.tipo === 'SELECCION' && cambios.seleccionada) {
      campo.opciones.filter((otra) => otra.tipo === 'SELECCION' && otra.id !== opcion.id
        && (!campo.permiteSeleccionMultiple || (opcion.grupoExclusion && otra.grupoExclusion === opcion.grupoExclusion)))
        .forEach((otra) => { siguientes[otra.id] = { ...siguientes[otra.id], seleccionada: false } })
    }
    setRespuestas(siguientes)
  }

  return <section className="asignacion-ficha">
    {!ocultarSelector && <div className="encabezado-asignacion">
      <div><h3>Asignar ficha médica <span>(opcional)</span></h3><p>Seleccioná una plantilla del profesional y completá sus campos.</p></div>
      <button type="button" className="boton-secundario" onClick={onActualizar} disabled={cargando}>{cargando ? 'Cargando…' : 'Actualizar fichas'}</button>
    </div>}
    {!ocultarSelector && <div className="selector-fichas">{fichas.map((item) => <label key={item.id}>
      <input type="checkbox" checked={idsSeleccionadas.includes(String(item.id))} onChange={(e) => onSeleccionar(String(item.id), e.target.checked)} disabled={cargando} />
      <span><strong>{item.nombre}</strong><small>{item.descripcion || `${item.detalles.length} secciones`}</small></span>
    </label>)}</div>}
    {!ocultarSelector && !cargando && fichas.length === 0 && <p className="aviso-ficha">Este profesional todavía no tiene fichas médicas disponibles.</p>}
    {seleccionadas.map((ficha) => <div className="ficha-para-completar ficha-carga-hoja" key={ficha.id}>
      <div className="titulo-ficha-asignada"><strong>{ficha.nombre}</strong>{ficha.descripcion && <small>{ficha.descripcion}</small>}</div>
      {ficha.detalles.map((detalle, indiceDetalle) => <section className="detalle-respuesta seccion-carga-formulario" key={detalle.id}>
        <div className="nombre-seccion cabecera-seccion-formulario"><span>{String(indiceDetalle + 1).padStart(2, '0')}</span><div><h4>{detalle.titulo}</h4>{detalle.descripcion && <p>{detalle.descripcion}</p>}</div></div>
        <div className="rejilla-formulario rejilla-carga-formulario">{detalle.campos.map((campo) => <fieldset className={claseCampo(campo)} key={campo.id}>
          <legend>{campo.titulo}{campo.permiteSeleccionMultiple && <small> · selección múltiple</small>}</legend>
          {campo.descripcion && <p>{campo.descripcion}</p>}
          <div className="opciones-respuesta">{campo.opciones.map((opcion) => {
            if (opcion.tipo === 'ENTRADA') return <label className="entrada-respuesta" key={opcion.id}>{opcion.titulo || 'Respuesta'}<input maxLength="1000" placeholder="Si queda vacío se guardará No aplica" value={respuestas[opcion.id]?.valor || ''} onChange={(e) => responder(opcion, campo, { valor: e.target.value })} /></label>
            if (opcion.tipo === 'SI_NO') return <div className="si-no-respuesta" key={opcion.id}><span>{opcion.titulo || campo.titulo}</span><label><input required type="radio" name={`opcion-${opcion.id}`} checked={respuestas[opcion.id]?.valor === 'SI'} onChange={() => responder(opcion, campo, { valor: 'SI' })} /> Sí</label><label><input required type="radio" name={`opcion-${opcion.id}`} checked={respuestas[opcion.id]?.valor === 'NO'} onChange={() => responder(opcion, campo, { valor: 'NO' })} /> No</label></div>
            return <label className="seleccion-respuesta" key={opcion.id}><input type="checkbox" name={`campo-${campo.id}`} checked={Boolean(respuestas[opcion.id]?.seleccionada)} onChange={(e) => responder(opcion, campo, { seleccionada: e.target.checked })} /> {opcion.titulo}</label>
          })}</div>
        </fieldset>)}</div>
      </section>)}
    </div>)}
  </section>
}

function VistaPreviaFicha({ ficha, onEditar }) {
  const claseCampo = (campo) => {
    const opciones = campo.opciones || []
    const soloEscritura = opciones.length === 1 && opciones[0].tipo === 'ENTRADA'
    const tieneSiNo = opciones.some((opcion) => opcion.tipo === 'SI_NO')
    if (soloEscritura) return 'campo-formulario campo-formulario--linea'
    if (opciones.length >= 5) return 'campo-formulario campo-formulario--grupo'
    if (tieneSiNo) return 'campo-formulario campo-formulario--binario'
    return 'campo-formulario'
  }

  return (
    <section className="panel vista-previa-ficha ficha-hoja" aria-label={`Vista previa de ${ficha.nombre}`}>
      <header className="cabecera-vista">
        <div><p className="sobrelinea">Vista previa · solo lectura</p><h2>{ficha.nombre}</h2><p>{ficha.descripcion || 'Sin descripción'}</p></div>
        <div className="acciones-vista"><span>{ficha.detalles.length} secciones</span><button type="button" onClick={onEditar}>Editar plantilla</button></div>
      </header>
      <div className="cuerpo-vista">
        {ficha.detalles.map((detalle, indiceDetalle) => (
          <article className="fila-seccion seccion-formulario" key={detalle.id || indiceDetalle}>
            <div className="nombre-seccion cabecera-seccion-formulario"><span>{String(indiceDetalle + 1).padStart(2, '0')}</span><div><h3>{detalle.titulo}</h3>{detalle.descripcion && <p>{detalle.descripcion}</p>}</div></div>
            <div className="campos-compactos rejilla-formulario">
              {detalle.campos.map((campo, indiceCampo) => (
                <div className={claseCampo(campo)} key={campo.id || indiceCampo}>
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
    <form className="panel editor ficha-editor-hoja" onSubmit={onGuardar}>
      <div className="encabezado-panel cabecera-editor-hoja"><div><p className="sobrelinea">Diseño de ficha clínica</p><h2>{editando ? 'Editar plantilla' : 'Nueva ficha médica'}</h2><p>Definí la estructura que completarás para cada paciente.</p></div>{editando && <button type="button" className="boton-secundario" onClick={onCancelar}>Cancelar</button>}</div>
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
