import { useEffect, useState } from 'react'
import { apiFichasMedicas } from './api/fichasMedicas.js'
import { apiPacientes } from './api/pacientes.js'
import { apiEpicrisis } from './api/epicrisis.js'
import { apiTratamientos } from './api/tratamientos.js'
import { apiExportacionHistoriaClinica } from './api/exportacionHistoriaClinica.js'
import { apiArchivos } from './api/archivos.js'

const modulos = [
  { icono: 'FM', titulo: 'Fichas médicas', descripcion: 'Diseñá y administrá plantillas clínicas.', disponible: true },
  { icono: 'PA', titulo: 'Pacientes', descripcion: 'Información personal y datos de contacto.', disponible: true, destino: 'pacientes' },
  { icono: 'HC', titulo: 'Historias clínicas', descripcion: 'Evoluciones y antecedentes por paciente.', disponible: true, destino: 'historias' },
  { icono: 'TR', titulo: 'Tratamientos', descripcion: 'Sesiones, avances y observaciones.', disponible: true, destino: 'tratamientos' },
  { icono: 'EP', titulo: 'Epicrisis', descripcion: 'Síntesis de episodios clínicos.', disponible: true, destino: 'epicrisis' },
  { icono: 'AR', titulo: 'Archivos clínicos', descripcion: 'Adjuntá y consultá documentación clínica por paciente.', disponible: true, destino: 'archivos' },
  { icono: 'EX', titulo: 'Exportar historia clínica', descripcion: 'Descargá la historia completa en PDF, Word, CSV o XLSX.', disponible: true, destino: 'exportar-historia' },
  { icono: 'CE', titulo: 'Consultar exportaciones', descripcion: 'Revisá el historial de exportaciones realizadas por paciente.', disponible: true, destino: 'consultar-exportaciones' },
]

const nuevaOpcion = (orden = 0) => ({ titulo: '', tipo: 'SELECCION', descripcion: '', orden, grupoExclusion: '' })
const nuevoCampo = (orden = 0) => ({ titulo: '', descripcion: '', orden, permiteSeleccionMultiple: false, opciones: [nuevaOpcion()] })
const nuevoDetalle = (orden = 0) => ({ titulo: '', descripcion: '', orden, campos: [nuevoCampo()] })
const fichaVacia = () => ({ nombre: '', descripcion: '', detalles: [nuevoDetalle()] })
const pacienteVacio = () => ({ nombre: '', apellido: '', dni: '', telefono: '', fechaNacimiento: '', sexo: '' })
const categoriasArchivo = [
  ['LABORATORIO', 'Laboratorio'], ['INFORME', 'Informe'], ['IMAGEN', 'Imagen'],
  ['CONSENTIMIENTO', 'Consentimiento'], ['RECETA', 'Receta'], ['ESTUDIO', 'Estudio'], ['OTRO', 'Otro'],
]
const extensionesArchivo = ['pdf', 'docx', 'jpg', 'jpeg', 'png']
const limiteGeneralArchivo = 20 * 1024 * 1024
const limiteImagenArchivo = 15 * 1024 * 1024

function limiteParaExtension(extension) {
  return ['jpg', 'jpeg', 'png'].includes(extension) ? limiteImagenArchivo : limiteGeneralArchivo
}

function etiquetaLimiteParaExtension(extension) {
  return `${limiteParaExtension(extension) / 1024 / 1024} MB`
}

function formatearBytes(bytes) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function validarArchivoAntesDeEnviar(file) {
  const extension = file.name.split('.').pop()?.toLowerCase()
  if (!extensionesArchivo.includes(extension)) return 'Solo se permiten PDF, DOCX, JPG/JPEG y PNG.'
  const limite = limiteParaExtension(extension)
  if (!file.size) return `“${file.name}” está vacío.`
  if (file.size > limite) return `“${file.name}” pesa ${formatearBytes(file.size)} y supera el máximo de ${etiquetaLimiteParaExtension(extension)} para archivos ${extension.toUpperCase()}.`
  return null
}

async function subirAdjuntosClinicos(adjuntos, subir) {
  const cargados = []; const fallidos = []; const advertencias = []
  for (const adjunto of adjuntos) {
    try {
      const respuesta = await subir(adjunto)
      cargados.push(respuesta)
      if (respuesta.warningStorage) advertencias.push(respuesta.warningStorage)
      if (respuesta.warningDuplicate) advertencias.push(respuesta.warningDuplicate)
    } catch (error) { fallidos.push({ adjunto, error: error.message }) }
  }
  return { cargados, fallidos, advertencias: [...new Set(advertencias)] }
}

function combinarResultadosAdjuntos(...resultados) {
  return {
    cargados: resultados.flatMap((item) => item.cargados),
    fallidos: resultados.flatMap((item) => item.fallidos),
    advertencias: [...new Set(resultados.flatMap((item) => item.advertencias))],
  }
}

function AdjuntosClinicosInput({ adjuntos, onChange, titulo = 'Archivos clínicos', descripcion, disabled = false }) {
  const [error, setError] = useState(null)
  const agregar = (evento) => {
    const nuevos = []
    for (const file of Array.from(evento.target.files || [])) {
      const validacion = validarArchivoAntesDeEnviar(file)
      if (validacion) { setError(validacion); continue }
      const extension = file.name.split('.').pop()?.toLowerCase()
      nuevos.push({ id: crypto.randomUUID(), file,
        categoria: ['jpg', 'jpeg', 'png'].includes(extension) ? 'IMAGEN' : 'INFORME', descripcion: '' })
    }
    if (nuevos.length) { onChange([...adjuntos, ...nuevos]); setError(null) }
    evento.target.value = ''
  }
  const actualizar = (id, cambios) => onChange(adjuntos.map((item) => item.id === id ? { ...item, ...cambios } : item))
  return <section className="panel bloque-adjuntos">
    <div className="encabezado-adjuntos"><div><p className="sobrelinea">Adjuntos opcionales</p><h3>{titulo}</h3><p>{descripcion || 'Podés adjuntar informes, estudios, imágenes, recetas o consentimientos.'}</p></div>
      <label className="selector-archivos"><input type="file" multiple disabled={disabled}
        accept=".pdf,.docx,.jpg,.jpeg,.png,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/jpeg,image/png"
        onChange={agregar} /><span>＋ Seleccionar archivos</span></label></div>
    <div className="limites-adjuntos" role="note" aria-label="Límites de tamaño por tipo de archivo"><strong>Límites por archivo</strong><span>PDF <b>20 MB</b></span><span>DOCX <b>20 MB</b></span><span>JPG/JPEG <b>15 MB</b></span><span>PNG <b>15 MB</b></span><small>El límite general es de 20 MB. Todos los archivos serán validados y analizados antes de guardarse.</small></div>
    {!!adjuntos.length && <div className="lista-adjuntos-seleccionados">{adjuntos.map((item) => <article className="adjunto-seleccionado" key={item.id}>
      <div className="identidad-adjunto"><strong title={item.file.name}>{item.file.name}</strong><small>{formatearBytes(item.file.size)} · máximo {etiquetaLimiteParaExtension(item.file.name.split('.').pop()?.toLowerCase())}</small></div>
      <label>Categoría<select value={item.categoria} disabled={disabled} onChange={(e) => actualizar(item.id, { categoria: e.target.value })}>{categoriasArchivo.map(([valor, etiqueta]) => <option value={valor} key={valor}>{etiqueta}</option>)}</select></label>
      <label>Descripción opcional<input maxLength="1000" value={item.descripcion} disabled={disabled} onChange={(e) => actualizar(item.id, { descripcion: e.target.value })} placeholder="Ej. Informe corregido" /></label>
      <button type="button" className="quitar-adjunto" aria-label={`Quitar ${item.file.name}`} disabled={disabled} onClick={() => onChange(adjuntos.filter((actual) => actual.id !== item.id))}>×</button>
    </article>)}</div>}
    {error && <p className="error-adjuntos" role="alert">{error}</p>}
  </section>
}

function ResultadoCargaAdjuntos({ resultado }) {
  if (!resultado || (!resultado.cargados.length && !resultado.fallidos.length)) return null
  return <div className={`resultado-adjuntos ${resultado.fallidos.length ? 'con-fallos' : ''}`}>
    <strong>{resultado.cargados.length} archivo{resultado.cargados.length === 1 ? '' : 's'} adjuntado{resultado.cargados.length === 1 ? '' : 's'}.</strong>
    {!!resultado.fallidos.length && <><span> {resultado.fallidos.length} no pudieron cargarse:</span><ul className="errores-carga-adjuntos">{resultado.fallidos.map((item) => <li key={item.adjunto.id}><strong>{item.adjunto.file.name}:</strong> {item.error}</li>)}</ul></>}
    {!!resultado.advertencias.length && <span> {resultado.advertencias.join(' ')}</span>}
  </div>
}

async function abrirArchivoEnNuevaPestana(archivo) {
  const pestana = window.open('about:blank', '_blank')
  if (!pestana) throw new Error('El navegador bloqueó la pestaña de visualización. Habilitá las ventanas emergentes para este sitio.')
  pestana.opener = null
  pestana.document.title = 'Preparando archivo…'
  pestana.document.body.style.cssText = 'margin:0;min-height:100vh;display:grid;place-items:center;background:#edf2f0;color:#315047;font:600 16px Arial,sans-serif'
  pestana.document.body.textContent = 'Preparando vista protegida…'
  try {
    const contenido = await apiArchivos.obtenerVistaPrevia(archivo.id)
    const url = URL.createObjectURL(contenido)
    pestana.location.replace(url)
    window.setTimeout(() => URL.revokeObjectURL(url), 5 * 60 * 1000)
  } catch (fallo) {
    if (!pestana.closed) {
      pestana.document.title = 'No se pudo visualizar el archivo'
      pestana.document.body.textContent = fallo.message
    }
    throw fallo
  }
}

function esImagenClinica(archivo) {
  const extension = archivo.nombreOriginal?.split('.').pop()?.toLowerCase()
  return ['jpg', 'jpeg', 'png'].includes(extension)
}

function etiquetaVisualizacionArchivo(archivo) {
  return esImagenClinica(archivo) ? 'Visualizar imagen' : 'Abrir en pestaña'
}

function VisorImagenClinica({ archivo, onCerrar }) {
  const [url, setUrl] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let activo = true
    let urlCreada = null
    const cargar = async () => {
      try {
        const contenido = await apiArchivos.obtenerVistaPrevia(archivo.id)
        if (!activo) return
        urlCreada = URL.createObjectURL(contenido)
        setUrl(urlCreada)
      } catch (fallo) {
        if (activo) setError(fallo.message)
      }
    }
    cargar()
    return () => {
      activo = false
      if (urlCreada) URL.revokeObjectURL(urlCreada)
    }
  }, [archivo.id])

  useEffect(() => {
    const overflowAnterior = document.body.style.overflow
    const cerrarConEscape = (evento) => { if (evento.key === 'Escape') onCerrar() }
    document.body.style.overflow = 'hidden'
    document.addEventListener('keydown', cerrarConEscape)
    return () => {
      document.body.style.overflow = overflowAnterior
      document.removeEventListener('keydown', cerrarConEscape)
    }
  }, [onCerrar])

  return <div className="fondo-visor-imagen" role="presentation" onMouseDown={(evento) => { if (evento.target === evento.currentTarget) onCerrar() }}>
    <section className="visor-imagen-clinica" role="dialog" aria-modal="true" aria-labelledby="titulo-visor-imagen">
      <header><div><p className="sobrelinea">Imagen clínica</p><h2 id="titulo-visor-imagen">{archivo.nombreOriginal}</h2><p className="descripcion-visor-imagen">{archivo.descripcion || 'Sin descripción'}</p></div><button type="button" className="cerrar-visor-imagen" aria-label="Cerrar visualización" onClick={onCerrar} autoFocus>×</button></header>
      <div className="contenido-visor-imagen">
        {!url && !error && <p className="estado-visor-imagen">Preparando vista protegida…</p>}
        {error && <p className="error-visor-imagen" role="alert">{error}</p>}
        {url && <img src={url} alt={`Vista previa de ${archivo.nombreOriginal}`} />}
      </div>
      <footer><button type="button" className="boton-secundario" onClick={onCerrar}>Cerrar</button></footer>
    </section>
  </div>
}

function ListadoArchivosClinicos({ archivos = [], cargando = false, titulo = 'Archivos disponibles', vacio = 'No hay archivos adjuntos.' }) {
  const [error, setError] = useState(null)
  const [imagenVisible, setImagenVisible] = useState(null)
  const contexto = { PACIENTE: 'Paciente', TRATAMIENTO: 'Tratamiento', SESION: 'Sesión', EPICRISIS: 'Epicrisis' }
  const descargar = async (archivo) => {
    setError(null)
    try { await apiArchivos.descargar(archivo.id, archivo.nombreOriginal) }
    catch (fallo) { setError(fallo.message) }
  }
  const visualizar = async (archivo) => {
    setError(null)
    if (esImagenClinica(archivo)) {
      setImagenVisible(archivo)
      return
    }
    try { await abrirArchivoEnNuevaPestana(archivo) }
    catch (fallo) { setError(fallo.message) }
  }
  return <div className="listado-archivos-clinicos">
    <div className="titulo-listado-archivos"><h3>{titulo}</h3>{!!archivos.length && <span>{archivos.length} archivo{archivos.length === 1 ? '' : 's'}</span>}</div>
    {error && <div className="mensaje error" role="alert">{error}</div>}
    {cargando && !archivos.length ? <p className="estado-vacio">Consultando archivos…</p> : !archivos.length ? <p className="estado-vacio">{vacio}</p> : archivos.map((archivo) => <article className="archivo-clinico-lista" key={archivo.id}>
      <span className="extension-archivo">{archivo.nombreOriginal.split('.').pop()}</span><div><button type="button" className="nombre-archivo-clinico" onClick={() => visualizar(archivo)}>{archivo.nombreOriginal}</button><small>{contexto[archivo.contexto]} · {categoriasArchivo.find(([valor]) => valor === archivo.categoria)?.[1] || archivo.categoria} · {formatearBytes(archivo.sizeBytes)} · v{archivo.version}</small><p className="descripcion-archivo-clinico"><strong>Descripción:</strong> {archivo.descripcion || 'Sin descripción'}</p></div><div className="acciones-archivo-clinico"><button type="button" onClick={() => visualizar(archivo)}>{etiquetaVisualizacionArchivo(archivo)}</button><button type="button" onClick={() => descargar(archivo)}>Descargar</button></div>
    </article>)}
    {imagenVisible && <VisorImagenClinica archivo={imagenVisible} onCerrar={() => setImagenVisible(null)} />}
  </div>
}

function ArchivosClinicosPaciente({ paciente, onCargaExitosa, onArchivosActualizados, mostrarListado = true }) {
  const [adjuntos, setAdjuntos] = useState([])
  const [archivos, setArchivos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [mensaje, setMensaje] = useState(null)
  const cargar = async () => {
    setCargando(true)
    try {
      const documentos = await apiArchivos.listarDelPaciente(paciente.id)
      setArchivos(documentos); onArchivosActualizados?.(documentos); setMensaje(null)
    }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }
  useEffect(() => { cargar() }, [paciente.id])
  const adjuntar = async () => {
    if (!adjuntos.length) return
    setCargando(true); setMensaje(null)
    const resultado = await subirAdjuntosClinicos(adjuntos, (item) => apiArchivos.adjuntarAPaciente(paciente.id, item))
    setAdjuntos(resultado.fallidos.map((item) => item.adjunto))
    if (resultado.cargados.length && onCargaExitosa) {
      setCargando(false)
      onCargaExitosa(resultado)
      return
    }
    if (resultado.cargados.length) await cargar()
    const partes = [`${resultado.cargados.length} archivo${resultado.cargados.length === 1 ? '' : 's'} guardado${resultado.cargados.length === 1 ? '' : 's'}.`]
    if (resultado.fallidos.length) partes.push(`${resultado.fallidos.length} no pudieron cargarse: ${resultado.fallidos.map((item) => item.error).join(' ')}`)
    if (resultado.advertencias.length) partes.push(resultado.advertencias.join(' '))
    setMensaje({ tipo: resultado.fallidos.length ? 'error' : 'exito', texto: partes.join(' ') })
    setCargando(false)
  }
  return <section className="panel administrador-adjuntos">
    <div className="encabezado-adjuntos"><div><p className="sobrelinea">Documentación clínica</p><h2>Archivos adjuntos</h2><p>{mostrarListado ? 'Adjuntá un archivo directamente al paciente o consultá los vinculados a sus registros.' : 'Adjuntá documentación al paciente. Una vez guardada se incorporará automáticamente a la cronología clínica.'}</p></div></div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    <AdjuntosClinicosInput adjuntos={adjuntos} onChange={setAdjuntos} disabled={cargando} titulo="Adjuntar al paciente" />
    <div className="acciones-carga-adjuntos"><span>{adjuntos.length ? `${adjuntos.length} archivo${adjuntos.length === 1 ? '' : 's'} listo${adjuntos.length === 1 ? '' : 's'}` : 'No seleccionaste archivos.'}</span><button type="button" className="boton-principal" disabled={cargando || !adjuntos.length} onClick={adjuntar}>{cargando ? 'Analizando y guardando…' : 'Adjuntar archivos'}</button></div>
    {mostrarListado && <ListadoArchivosClinicos archivos={archivos} cargando={cargando} vacio="Este paciente todavía no tiene archivos adjuntos." />}
  </section>
}
const capitalizarPalabras = (valor) => valor
  .toLocaleLowerCase('es-AR')
  .replace(/(^|[^\p{L}])(\p{L})/gu, (_, separador, letra) => `${separador}${letra.toLocaleUpperCase('es-AR')}`)

const vistaDesdeRuta = () => {
  if (window.location.pathname.includes('fichas-medicas')) return 'fichas'
  if (window.location.pathname.includes('pacientes')) return 'pacientes'
  if (window.location.pathname.includes('historias-clinicas')) return 'historias'
  if (window.location.pathname.includes('epicrisis')) return 'epicrisis'
  if (window.location.pathname.includes('tratamientos')) return 'tratamientos'
  if (window.location.pathname.includes('archivos-clinicos')) return 'archivos'
  if (window.location.pathname.includes('consultar-exportaciones')) return 'consultar-exportaciones'
  if (window.location.pathname.includes('exportar-historia-clinica')) return 'exportar-historia'
  return 'inicio'
}

export default function App() {
  const [vista, setVista] = useState(vistaDesdeRuta)

  const navegar = (destino) => {
    const rutas = { fichas: '/fichas-medicas', pacientes: '/pacientes', historias: '/historias-clinicas', epicrisis: '/epicrisis', tratamientos: '/tratamientos', archivos: '/archivos-clinicos', 'exportar-historia': '/exportar-historia-clinica', 'consultar-exportaciones': '/consultar-exportaciones', inicio: '/' }
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
      {vista === 'historias' && <GestionHistoriasClinicas onVolver={() => navegar('inicio')} />}
      {vista === 'epicrisis' && <GestionEpicrisis onVolver={() => navegar('inicio')} />}
      {vista === 'tratamientos' && <GestionTratamientos onVolver={() => navegar('inicio')} />}
      {vista === 'archivos' && <GestionArchivosClinicos onVolver={() => navegar('inicio')} />}
      {vista === 'exportar-historia' && <ExportarHistoriaClinica onVolver={() => navegar('inicio')} />}
      {vista === 'consultar-exportaciones' && <ConsultarExportacionesHistoriaClinica onVolver={() => navegar('inicio')} />}
    </div>
  )
}

function GestionArchivosClinicos({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [idProfesional, setIdProfesional] = useState('1')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [buscado, setBuscado] = useState(false)
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [resultadoCarga, setResultadoCarga] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const filtrados = pacientes.filter((paciente) =>
    normalizar(`${paciente.apellido} ${paciente.nombre}`).includes(normalizar(busqueda)))

  const buscar = async () => {
    if (!busqueda.trim()) return
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiPacientes.listar(idProfesional)); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const confirmarPaciente = () => {
    if (!seleccionado || !window.confirm(`¿Confirmás a ${seleccionado.apellido}, ${seleccionado.nombre} para gestionar sus archivos clínicos?`)) return
    setPantalla('archivos'); setMensaje(null); setResultadoCarga(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cargaExitosa = (resultado) => {
    setResultadoCarga(resultado); setPantalla('exito')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const volver = pantalla === 'buscar' ? onVolver : () => {
    setPantalla('buscar'); setSeleccionado(null); setMensaje(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  if (pantalla === 'exito') return <main className="contenido pagina-fichas pagina-archivos"><section className="panel resultado-epicrisis-exitoso" role="status"><span className="icono-exito-epicrisis">✓</span><p className="sobrelinea">Carga completada</p><h1>Archivos adjuntados con éxito</h1><p>La documentación clínica de {seleccionado.apellido}, {seleccionado.nombre} fue guardada correctamente.</p><ResultadoCargaAdjuntos resultado={resultadoCarga} /><ListadoArchivosClinicos archivos={resultadoCarga?.cargados || []} titulo="Archivos recién cargados" /><button className="boton-principal" onClick={onVolver}>Volver al panel principal</button></section></main>

  return <main className="contenido pagina-fichas pagina-archivos">
    <button className="volver" onClick={volver}>← {pantalla === 'buscar' ? 'Volver al inicio' : 'Volver a buscar pacientes'}</button>
    <div className="cabecera-pagina"><div><p className="sobrelinea">Documentación clínica</p><h1>Archivos clínicos</h1><p>{pantalla === 'buscar' ? 'Buscá y seleccioná al paciente para continuar.' : 'Adjuntá archivos directamente al paciente y consultá toda su documentación.'}</p></div>{pantalla === 'buscar' && <label className="profesional">ID del profesional<input type="number" min="1" value={idProfesional} onChange={(e) => setIdProfesional(e.target.value)} /></label>}</div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis">
      <div className="titulo-paso"><h2>Buscar paciente</h2><p>Ingresá apellido o nombre para buscar coincidencias.</p></div>
      <div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscar() } }} placeholder="Ej. Pérez - Ana" /></label><button type="button" className="boton-principal" onClick={buscar} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div>
      <div className="resultados-pacientes-epicrisis">{!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !filtrados.length ? <p>No se encontraron pacientes.</p> : filtrados.map((paciente) => <label className={seleccionado?.id === paciente.id ? 'seleccionado' : ''} key={paciente.id}><input type="radio" name="paciente-archivos" checked={seleccionado?.id === paciente.id} onChange={() => setSeleccionado(paciente)} /><span><strong>{paciente.apellido}, {paciente.nombre}</strong><small>DNI {paciente.dni}</small></span></label>)}</div>
      <div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button type="button" className="boton-principal" disabled={!seleccionado || cargando} onClick={confirmarPaciente}>Confirmar paciente</button></div>
    </section>}
    {pantalla === 'archivos' && <><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div></section><ArchivosClinicosPaciente paciente={seleccionado} onCargaExitosa={cargaExitosa} /></>}
  </main>
}

function ExportarHistoriaClinica({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [buscado, setBuscado] = useState(false)
  const [formato, setFormato] = useState('PDF')
  const [tipoExportacion, setTipoExportacion] = useState('HISTORIA_CLINICA')
  const [motivo, setMotivo] = useState('SOLICITUD_DEL_PACIENTE')
  const [detalleMotivo, setDetalleMotivo] = useState('')
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const filtrados = pacientes.filter((paciente) =>
    normalizar(`${paciente.apellido} ${paciente.nombre}`).includes(normalizar(busqueda)))

  const buscar = async () => {
    if (!busqueda.trim()) return
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiExportacionHistoriaClinica.listarPacientes()); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const confirmarPaciente = () => {
    if (!seleccionado) return
    if (!window.confirm(`¿Confirmás que querés exportar la historia clínica de ${seleccionado.apellido}, ${seleccionado.nombre}?`)) return
    setPantalla('configurar'); setMensaje(null)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const exportar = async (evento) => {
    evento.preventDefault(); setCargando(true); setMensaje(null)
    try {
      const nombre = await apiExportacionHistoriaClinica.exportar(seleccionado.id, {
        formato, tipoExportacion, motivo, detalleMotivo: detalleMotivo.trim() || null,
      })
      setMensaje({ tipo: 'exito', texto: `La historia clínica se exportó correctamente como ${nombre}.` })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const volver = pantalla === 'buscar' ? onVolver : () => {
    setPantalla('buscar'); setMensaje(null); setSeleccionado(null)
  }

  return <main className="contenido pagina-exportacion">
    <button className="volver" onClick={volver}>← {pantalla === 'buscar' ? 'Volver al inicio' : 'Volver a buscar pacientes'}</button>
    <div className="cabecera-pagina"><div><p className="sobrelinea">Portabilidad clínica</p><h1>Exportar historia clínica</h1><p>{pantalla === 'buscar' ? 'Buscá y seleccioná al paciente cuya historia querés descargar.' : 'Elegí el formato y registrá el motivo de la exportación.'}</p></div></div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis">
      <div className="titulo-paso"><h2>Buscar paciente</h2><p>Solo se muestran pacientes asociados al profesional autenticado.</p></div>
      <div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscar() } }} placeholder="Ej. Pérez - Ana" /></label><button type="button" className="boton-principal" onClick={buscar} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div>
      <div className="resultados-pacientes-epicrisis">{!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !filtrados.length ? <p>No se encontraron pacientes.</p> : filtrados.map((paciente) => <label className={seleccionado?.id === paciente.id ? 'seleccionado' : ''} key={paciente.id}><input type="radio" name="paciente-exportacion" checked={seleccionado?.id === paciente.id} onChange={() => setSeleccionado(paciente)} /><span><strong>{paciente.apellido}, {paciente.nombre}</strong><small>DNI {paciente.dni}</small></span></label>)}</div>
      <div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button type="button" className="boton-principal" disabled={!seleccionado || cargando} onClick={confirmarPaciente}>Confirmar paciente</button></div>
    </section>}
    {pantalla === 'configurar' && <form className="formulario-exportacion" onSubmit={exportar}>
      <section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Historia clínica de</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div></section>
      <section className="panel configuracion-exportacion"><div className="titulo-paso"><h2>Datos de la exportación</h2><p>La historia incluirá referencias a los archivos clínicos activos. También podés descargar un ZIP con sus versiones actuales.</p></div>
        <div className="grilla-formulario grilla-opciones-exportacion"><label>Tipo de exportación<select required value={tipoExportacion} onChange={(e) => { const tipo = e.target.value; setTipoExportacion(tipo); if (tipo === 'HISTORIA_CLINICA_CON_ADJUNTOS' && !['PDF', 'DOCX'].includes(formato)) setFormato('PDF') }}><option value="HISTORIA_CLINICA">Historia clínica</option><option value="HISTORIA_CLINICA_CON_ADJUNTOS">Historia clínica con adjuntos (ZIP)</option></select></label><label>Formato del documento principal<select required value={formato} onChange={(e) => setFormato(e.target.value)}><option value="PDF">PDF</option><option value="DOCX">Word (DOCX)</option>{tipoExportacion === 'HISTORIA_CLINICA' && <><option value="CSV">CSV</option><option value="XLSX">Excel (XLSX)</option></>}</select></label><label>Motivo<select required value={motivo} onChange={(e) => setMotivo(e.target.value)}><option value="SOLICITUD_DEL_PACIENTE">Solicitud del paciente</option><option value="CONTINUIDAD_DE_TRATAMIENTO">Continuidad de tratamiento</option><option value="DERIVACION">Derivación</option><option value="SEGUNDA_OPINION">Segunda opinión</option><option value="TRAMITE_ADMINISTRATIVO">Trámite administrativo</option><option value="OTRO">Otro</option></select></label></div>
        <div className="resumen-tipo-exportacion" role="note">{tipoExportacion === 'HISTORIA_CLINICA_CON_ADJUNTOS' ? <><strong>Exportación completa</strong><span>Se descargará un ZIP con la historia en {formato}, un manifest de integridad y los adjuntos activos en su formato original.</span></> : <><strong>Exportación de historia clínica</strong><span>Se descargará un archivo {formato} con referencias a los adjuntos, sin incluir sus binarios.</span></>}</div>
        <label className="detalle-motivo-exportacion">Detalle del motivo <span>(opcional)</span><textarea maxLength="500" rows="4" value={detalleMotivo} onChange={(e) => setDetalleMotivo(e.target.value)} placeholder="Ej. Copia solicitada por el paciente" /><small>{detalleMotivo.length} / 500 caracteres</small></label>
        <button className="boton-principal" disabled={cargando}>{cargando ? 'Generando archivo…' : tipoExportacion === 'HISTORIA_CLINICA_CON_ADJUNTOS' ? `Generar ZIP con historia en ${formato}` : `Exportar como ${formato}`}</button>
      </section>
    </form>}
  </main>
}

const etiquetasMotivoExportacion = {
  SOLICITUD_DEL_PACIENTE: 'Solicitud del paciente',
  CONTINUIDAD_DE_TRATAMIENTO: 'Continuidad de tratamiento',
  DERIVACION: 'Derivación',
  SEGUNDA_OPINION: 'Segunda opinión',
  TRAMITE_ADMINISTRATIVO: 'Trámite administrativo',
  OTRO: 'Otro',
}
const etiquetasTipoExportacion = {
  HISTORIA_CLINICA: 'Historia clínica',
  HISTORIA_CLINICA_CON_ADJUNTOS: 'Historia clínica con adjuntos',
}

function ConsultarExportacionesHistoriaClinica({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [buscado, setBuscado] = useState(false)
  const [exportaciones, setExportaciones] = useState([])
  const [exportacionSeleccionada, setExportacionSeleccionada] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const filtrados = pacientes.filter((paciente) =>
    normalizar(`${paciente.apellido} ${paciente.nombre}`).includes(normalizar(busqueda)))
  const formatearFecha = (valor) => new Date(valor).toLocaleString('es-AR', {
    dateStyle: 'long', timeStyle: 'short',
  })

  const buscar = async () => {
    if (!busqueda.trim()) return
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiExportacionHistoriaClinica.listarPacientes()); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const confirmarPaciente = async () => {
    if (!seleccionado || !window.confirm(`¿Confirmás que querés consultar las exportaciones de ${seleccionado.apellido}, ${seleccionado.nombre}?`)) return
    setCargando(true); setMensaje(null)
    try {
      setExportaciones(await apiExportacionHistoriaClinica.listarExportaciones(seleccionado.id))
      setPantalla('listado')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const consultarDetalle = async (exportacion) => {
    setCargando(true); setMensaje(null)
    try {
      setExportacionSeleccionada(await apiExportacionHistoriaClinica.obtenerExportacion(
        seleccionado.id, exportacion.id))
      setPantalla('detalle')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const volver = pantalla === 'buscar' ? onVolver : pantalla === 'detalle' ? () => {
    setPantalla('listado'); setExportacionSeleccionada(null); setMensaje(null)
  } : () => {
    setPantalla('buscar'); setSeleccionado(null); setExportaciones([]); setMensaje(null)
  }

  return <main className="contenido pagina-consulta-exportaciones">
    <button className="volver" onClick={volver}>← {pantalla === 'buscar' ? 'Volver al inicio' : pantalla === 'detalle' ? 'Volver a las exportaciones' : 'Volver a buscar pacientes'}</button>
    <div className="cabecera-pagina"><div><p className="sobrelinea">Trazabilidad clínica</p><h1>Consultar exportaciones de historias clínicas</h1><p>{pantalla === 'buscar' ? 'Buscá y confirmá el paciente cuyo historial de exportaciones querés revisar.' : pantalla === 'listado' ? 'Consultá las exportaciones ordenadas desde la más reciente.' : 'Revisá todos los datos registrados para esta exportación.'}</p></div></div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`} role="alert">{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis">
      <div className="titulo-paso"><h2>Buscar paciente</h2><p>Solo se muestran pacientes asociados al profesional autenticado.</p></div>
      <div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscar() } }} placeholder="Ej. Pérez - Ana" /></label><button type="button" className="boton-principal" onClick={buscar} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div>
      <div className="resultados-pacientes-epicrisis">{!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !filtrados.length ? <p>No se encontraron pacientes.</p> : filtrados.map((paciente) => <label className={seleccionado?.id === paciente.id ? 'seleccionado' : ''} key={paciente.id}><input type="radio" name="paciente-consulta-exportaciones" checked={seleccionado?.id === paciente.id} onChange={() => setSeleccionado(paciente)} /><span><strong>{paciente.apellido}, {paciente.nombre}</strong><small>DNI {paciente.dni}</small></span></label>)}</div>
      <div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button type="button" className="boton-principal" disabled={!seleccionado || cargando} onClick={confirmarPaciente}>{cargando ? 'Consultando…' : 'Confirmar paciente'}</button></div>
    </section>}
    {pantalla === 'listado' && <><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div></section><section className="panel listado-exportaciones-historia"><div className="encabezado-listado-exportaciones"><div><p className="sobrelinea">Historial cronológico</p><h2>Exportaciones realizadas</h2></div><span>{exportaciones.length} exportación{exportaciones.length === 1 ? '' : 'es'}</span></div>{!exportaciones.length ? <p className="estado-vacio">Todavía no se exportó la historia clínica de este paciente.</p> : <div className="exportaciones-historia">{exportaciones.map((exportacion) => { const formatoFinal = exportacion.formatoArchivoFinal || exportacion.formato; return <article className="exportacion-historia" key={exportacion.id}><span className={`formato-exportacion formato-${formatoFinal.toLowerCase()}`}>{formatoFinal}</span><div><strong>{exportacion.nombreArchivo}</strong><small>{formatearFecha(exportacion.fechaHoraExportacion)}</small><p>{etiquetasTipoExportacion[exportacion.tipoExportacion] || 'Historia clínica'} · {etiquetasMotivoExportacion[exportacion.motivo] || exportacion.motivo}</p></div><button type="button" className="boton-secundario" disabled={cargando} onClick={() => consultarDetalle(exportacion)}>Ver datos</button></article> })}</div>}</section></>}
    {pantalla === 'detalle' && exportacionSeleccionada && <><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div></section><section className="panel detalle-exportacion-historia"><div className="cabecera-detalle-exportacion"><div><p className="sobrelinea">Exportación registrada</p><h2>{exportacionSeleccionada.nombreArchivo}</h2><p>{formatearFecha(exportacionSeleccionada.fechaHoraExportacion)}</p></div><span className={`formato-exportacion formato-${(exportacionSeleccionada.formatoArchivoFinal || exportacionSeleccionada.formato).toLowerCase()}`}>{exportacionSeleccionada.formatoArchivoFinal || exportacionSeleccionada.formato}</span></div><dl className="datos-exportacion-historia"><div><dt>ID de exportación</dt><dd>{exportacionSeleccionada.id}</dd></div><div><dt>ID del paciente</dt><dd>{exportacionSeleccionada.pacienteId}</dd></div><div><dt>ID del profesional</dt><dd>{exportacionSeleccionada.profesionalId}</dd></div><div><dt>Fecha y hora</dt><dd>{formatearFecha(exportacionSeleccionada.fechaHoraExportacion)}</dd></div><div><dt>Tipo de exportación</dt><dd>{etiquetasTipoExportacion[exportacionSeleccionada.tipoExportacion] || 'Historia clínica'}</dd></div><div><dt>Formato de la historia clínica</dt><dd>{exportacionSeleccionada.formatoHistoriaClinica || exportacionSeleccionada.formato}</dd></div><div><dt>Formato del archivo final</dt><dd>{exportacionSeleccionada.formatoArchivoFinal || exportacionSeleccionada.formato}</dd></div><div><dt>Motivo</dt><dd>{etiquetasMotivoExportacion[exportacionSeleccionada.motivo] || exportacionSeleccionada.motivo}</dd></div><div className="dato-exportacion-ancho"><dt>Detalle del motivo</dt><dd>{exportacionSeleccionada.detalleMotivo || 'Sin detalle adicional'}</dd></div><div className="dato-exportacion-ancho"><dt>Nombre del archivo</dt><dd>{exportacionSeleccionada.nombreArchivo}</dd></div><div className="dato-exportacion-ancho"><dt>Hash de integridad SHA-256</dt><dd><code>{exportacionSeleccionada.hashArchivo}</code></dd></div></dl></section></>}
  </main>
}

function GestionHistoriasClinicas({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [idProfesional, setIdProfesional] = useState('1')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [buscado, setBuscado] = useState(false)
  const [eventos, setEventos] = useState([])
  const [archivosHistoria, setArchivosHistoria] = useState([])
  const [eventoSeleccionado, setEventoSeleccionado] = useState(null)
  const [eventoDetalleAnterior, setEventoDetalleAnterior] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [rectificando, setRectificando] = useState(false)
  const [auditoriaEvento, setAuditoriaEvento] = useState(null)
  const [filtroTipoEvento, setFiltroTipoEvento] = useState('todos')
  const [imagenVisible, setImagenVisible] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '')
    .replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const pacientesFiltrados = pacientes.filter((paciente) =>
    normalizar(`${paciente.apellido} ${paciente.nombre}`).includes(normalizar(busqueda)))
  const formatearFecha = (valor) => new Date(valor).toLocaleString('es-AR', { dateStyle: 'long', timeStyle: 'short' })
  const eventosDeArchivos = (archivos) => archivos.map((archivo) => ({
    tipo: 'archivo', fecha: archivo.createdAt || archivo.updatedAt,
    nombre: archivo.nombreOriginal, datos: archivo,
  }))
  const ordenarCronologia = (items) => [...items].sort((a, b) => new Date(b.fecha) - new Date(a.fecha))

  const actualizarArchivosHistoria = (archivos) => {
    setArchivosHistoria(archivos)
    setEventos((actuales) => ordenarCronologia([
      ...actuales.filter((evento) => evento.tipo !== 'archivo'), ...eventosDeArchivos(archivos),
    ]))
  }

  const visualizarArchivo = async (archivo) => {
    setMensaje(null)
    if (esImagenClinica(archivo)) {
      setImagenVisible(archivo)
      return
    }
    try { await abrirArchivoEnNuevaPestana(archivo) }
    catch (fallo) { setMensaje({ tipo: 'error', texto: fallo.message }) }
  }

  const buscarPacientes = async () => {
    if (!idProfesional || !busqueda.trim()) return
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiPacientes.listar(idProfesional)); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const consultarHistoria = async () => {
    if (!seleccionado) return
    if (!window.confirm(`¿Confirmás que querés consultar la historia clínica de ${seleccionado.apellido}, ${seleccionado.nombre}?`)) return
    setCargando(true); setMensaje(null)
    try {
      const cronologia = await cargarCronologia()
      setEventos(cronologia); setEventoSeleccionado(null); setFiltroTipoEvento('todos'); setPantalla('historia')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const cargarCronologia = async () => {
    const [epicrisis, tratamientos, archivos] = await Promise.all([
      apiEpicrisis.listar(idProfesional, seleccionado.id), apiTratamientos.listar(idProfesional, seleccionado.id),
      apiArchivos.listarDelPaciente(seleccionado.id),
    ])
    setArchivosHistoria(archivos)
    const cronologia = ordenarCronologia([
      ...epicrisis.map((item) => ({ tipo: 'epicrisis', fecha: item.fechaHora, nombre: 'Epicrisis', datos: item })),
      ...tratamientos.map((item) => ({ tipo: 'tratamiento', fecha: item.fechaCreacion, nombre: item.nombre, datos: item })),
      ...tratamientos.flatMap((tratamiento) => tratamiento.sesiones.map((sesion) => ({
        tipo: 'sesion', fecha: sesion.fechaHora, nombre: `Sesión N.º ${sesion.nroSesion} · ${tratamiento.nombre}`,
        datos: sesion, tratamiento,
      }))),
      ...eventosDeArchivos(archivos),
    ])
    setEventos(cronologia)
    return cronologia
  }

  const guardarRectificacion = async (datos) => {
    setCargando(true); setMensaje(null)
    try {
      const e = eventoSeleccionado
      if (e.tipo === 'epicrisis') await apiEpicrisis.rectificar(idProfesional, seleccionado.id, e.datos.id, datos)
      else if (e.tipo === 'tratamiento') await apiTratamientos.rectificar(idProfesional, seleccionado.id, e.datos.id, datos)
      else await apiTratamientos.rectificarSesion(idProfesional, seleccionado.id, e.tratamiento.id, e.datos.id, datos)
      const cronologia = await cargarCronologia()
      const actualizado = cronologia.find((item) => item.tipo === e.tipo && item.datos.id === e.datos.id)
      setEventoSeleccionado(actualizado); setRectificando(false); setAuditoriaEvento(null)
      if (e.tipo === 'sesion' && eventoDetalleAnterior) {
        setEventoDetalleAnterior(cronologia.find((item) => item.tipo === 'tratamiento' && item.datos.id === e.tratamiento.id))
      }
      setMensaje({ tipo: 'exito', texto: 'La rectificación quedó registrada con su auditoría inmutable.' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const consultarAuditoria = async () => {
    setCargando(true); setMensaje(null)
    try {
      const e = eventoSeleccionado
      const datos = e.tipo === 'epicrisis' ? await apiEpicrisis.auditoria(idProfesional, seleccionado.id, e.datos.id)
        : e.tipo === 'tratamiento' ? await apiTratamientos.auditoria(idProfesional, seleccionado.id, e.datos.id)
          : await apiTratamientos.auditoriaSesion(idProfesional, seleccionado.id, e.tratamiento.id, e.datos.id)
      setAuditoriaEvento(datos)
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  const descargarAuditoria = async () => {
    try {
      const e = eventoSeleccionado
      if (e.tipo === 'epicrisis') await apiEpicrisis.descargarInforme(idProfesional, seleccionado.id, e.datos.id)
      else if (e.tipo === 'tratamiento') await apiTratamientos.descargarInforme(idProfesional, seleccionado.id, e.datos.id)
      else await apiTratamientos.descargarInformeSesion(idProfesional, seleccionado.id, e.tratamiento.id, e.datos.id)
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
  }

  const abrirDetalle = (evento, anterior = null) => {
    setEventoDetalleAnterior(anterior); setEventoSeleccionado(evento); setRectificando(false); setAuditoriaEvento(null); setPantalla('detalle')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const volver = pantalla === 'buscar' ? onVolver : () => {
    if (pantalla === 'detalle' && eventoDetalleAnterior) {
      setEventoSeleccionado(eventoDetalleAnterior); setEventoDetalleAnterior(null)
      return
    }
    setPantalla(pantalla === 'detalle' ? 'historia' : 'buscar')
    if (pantalla === 'historia') { setEventos([]); setArchivosHistoria([]); setFiltroTipoEvento('todos'); setSeleccionado(null) }
  }

  const filtrosTipoEvento = [
    ['todos', 'Todos'], ['epicrisis', 'Epicrisis'], ['tratamiento', 'Tratamientos'],
    ['sesion', 'Sesiones'], ['archivo', 'Archivos'],
  ]
  const eventosFiltrados = filtroTipoEvento === 'todos'
    ? eventos : eventos.filter((evento) => evento.tipo === filtroTipoEvento)

  return <main className="contenido pagina-historias">
    <button className="volver" onClick={volver}>← {pantalla === 'buscar' ? 'Volver al inicio' : pantalla === 'detalle' && eventoDetalleAnterior ? 'Volver al tratamiento' : pantalla === 'detalle' ? 'Volver a la historia clínica' : 'Volver a buscar pacientes'}</button>
    <div className="cabecera-pagina"><div><p className="sobrelinea">Registro clínico</p><h1>Historias clínicas</h1><p>{pantalla === 'buscar' ? 'Buscá y seleccioná al paciente.' : 'Consultá la actividad clínica ordenada cronológicamente.'}</p></div>{pantalla === 'buscar' && <label className="profesional">ID del profesional<input type="number" min="1" value={idProfesional} onChange={(e) => setIdProfesional(e.target.value)} /></label>}</div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis"><div className="titulo-paso"><h2>Buscar paciente</h2><p>Ingresá apellido o nombre para buscar coincidencias.</p></div><div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscarPacientes() } }} placeholder="Ej. Pérez - Ana" /></label><button className="boton-principal" onClick={buscarPacientes} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div><div className="resultados-pacientes-epicrisis">{!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !pacientesFiltrados.length ? <p>No se encontraron pacientes.</p> : pacientesFiltrados.map((paciente) => <label className={seleccionado?.id === paciente.id ? 'seleccionado' : ''} key={paciente.id}><input type="radio" name="paciente-historia" checked={seleccionado?.id === paciente.id} onChange={() => setSeleccionado(paciente)} /><span><strong>{paciente.apellido}, {paciente.nombre}</strong><small>DNI {paciente.dni}</small></span></label>)}</div><div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button className="boton-principal" disabled={!seleccionado || cargando} onClick={consultarHistoria}>{cargando ? 'Consultando…' : 'Consultar historia clínica'}</button></div></section>}
    {pantalla === 'historia' && <><section className="panel identidad-paciente-epicrisis cabecera-historia"><div><p className="sobrelinea">Historia clínica de</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div></section><ArchivosClinicosPaciente paciente={seleccionado} mostrarListado={false} onArchivosActualizados={actualizarArchivosHistoria} /><section className="panel listado-historia"><div className="encabezado-historia"><div><p className="sobrelinea">Cronología clínica unificada</p><h2>Registros y archivos del paciente</h2></div><span>{filtroTipoEvento === 'todos' ? `${eventos.length} elementos` : `${eventosFiltrados.length} de ${eventos.length}`}</span></div><div className="filtros-tipo-evento" role="group" aria-label="Filtrar cronología por tipo de evento">{filtrosTipoEvento.map(([valor, etiqueta]) => <button type="button" className={filtroTipoEvento === valor ? 'activo' : ''} aria-pressed={filtroTipoEvento === valor} onClick={() => setFiltroTipoEvento(valor)} key={valor}>{etiqueta}</button>)}</div><div className="eventos-historia">{!eventos.length ? <p className="estado-vacio">Este paciente todavía no tiene registros clínicos ni archivos.</p> : !eventosFiltrados.length ? <p className="estado-vacio">No hay elementos del tipo seleccionado.</p> : eventosFiltrados.map((evento) => <EventoHistoria evento={evento} formatearFecha={formatearFecha} onConsultar={() => evento.tipo === 'archivo' ? visualizarArchivo(evento.datos) : abrirDetalle(evento)} key={`${evento.tipo}-${evento.datos.id}`} />)}</div></section></>}
    {pantalla === 'detalle' && eventoSeleccionado && <DetalleHistoria evento={eventoSeleccionado} idProfesional={idProfesional} formatearFecha={formatearFecha}
      rectificando={rectificando} cargando={cargando} auditoria={auditoriaEvento}
      onRectificar={() => { setRectificando(true); setAuditoriaEvento(null) }} onCancelarRectificacion={() => setRectificando(false)}
      onGuardarRectificacion={guardarRectificacion} onConsultarAuditoria={consultarAuditoria} onDescargarAuditoria={descargarAuditoria}
      archivos={archivosHistoria}
      onConsultarSesion={(sesion) => abrirDetalle({ tipo: 'sesion', fecha: sesion.fechaHora, nombre: `Sesión N.º ${sesion.nroSesion} · ${eventoSeleccionado.datos.nombre}`, datos: sesion, tratamiento: eventoSeleccionado.datos }, eventoSeleccionado)} />}
    {imagenVisible && <VisorImagenClinica archivo={imagenVisible} onCerrar={() => setImagenVisible(null)} />}
  </main>
}

function EventoHistoria({ evento, formatearFecha, onConsultar }) {
  const etiquetas = { epicrisis: 'Epicrisis', tratamiento: 'Tratamiento', sesion: 'Sesión', archivo: 'Archivo' }
  return <article className="evento-historia"><span className={`tipo-evento ${evento.tipo}`}>{etiquetas[evento.tipo]}</span><div className="identidad-evento-historia"><strong>{evento.nombre}</strong>{evento.tipo === 'archivo' && <p>{evento.datos.descripcion || 'Sin descripción'}</p>}</div><small>{formatearFecha(evento.fecha)}</small><button type="button" className="boton-secundario" onClick={onConsultar}>{evento.tipo === 'archivo' ? etiquetaVisualizacionArchivo(evento.datos) : 'Consultar datos'}</button></article>
}

function AdjuntosEventoHistoria({ evento, archivos }) {
  const contexto = evento.tipo.toUpperCase()
  const asociados = archivos.filter((archivo) => archivo.contexto === contexto
    && String(archivo.contextoId) === String(evento.datos.id))
  const etiquetas = { EPICRISIS: 'la epicrisis', TRATAMIENTO: 'el tratamiento', SESION: 'la sesión' }
  return <section className="panel archivos-evento-historia"><div><p className="sobrelinea">Documentación asociada</p><h2>Archivos adjuntos</h2><p>Documentos vinculados específicamente a {etiquetas[contexto]}.</p></div><ListadoArchivosClinicos archivos={asociados} titulo="Documentos del registro" vacio="Este registro no tiene archivos adjuntos." /></section>
}

function DetalleHistoria({ evento, idProfesional, formatearFecha, onConsultarSesion, rectificando, cargando, auditoria,
  onRectificar, onCancelarRectificacion, onGuardarRectificacion, onConsultarAuditoria, onDescargarAuditoria, archivos }) {
  const { datos } = evento
  const acciones = <div className="acciones-rectificacion"><button type="button" className="boton-principal" onClick={onRectificar}>Rectificar registro</button><button type="button" className="boton-secundario" onClick={onConsultarAuditoria} disabled={cargando}>{cargando ? 'Consultando…' : 'Ver historial de rectificaciones'}</button><button type="button" className="boton-secundario" onClick={onDescargarAuditoria}>Descargar informe de auditoría</button></div>
  if (evento.tipo === 'tratamiento') {
    const realizadas = datos.cantidadSesionesTotal - datos.cantidadSesionesFaltantes
    return <section className="detalle-historia"><section className="panel"><p className="sobrelinea">Detalle del tratamiento</p><h2>{datos.nombre}</h2><EstadoVersion datos={datos} formatearFecha={formatearFecha} /><p className="fecha-detalle">Creado el {formatearFecha(datos.fechaCreacion)}</p><dl className="datos-evento"><div><dt>Sesiones totales</dt><dd>{datos.cantidadSesionesTotal}</dd></div><div><dt>Sesiones realizadas</dt><dd>{realizadas}</dd></div><div><dt>Sesiones pendientes</dt><dd>{datos.cantidadSesionesFaltantes}</dd></div></dl><div className="observacion-detalle"><strong>Descripción</strong><br />{datos.descripcion || 'Sin descripción'}</div>{!rectificando && acciones}</section>{rectificando && <FormularioRectificacion evento={evento} idProfesional={idProfesional} cargando={cargando} onGuardar={onGuardarRectificacion} onCancelar={onCancelarRectificacion} />}<HistorialRectificaciones auditoria={auditoria} formatearFecha={formatearFecha} /><AdjuntosEventoHistoria evento={evento} archivos={archivos} /><section className="panel"><div className="encabezado-historia"><div><p className="sobrelinea">Seguimiento</p><h2>Sesiones del tratamiento</h2></div><span>{datos.sesiones.length} sesiones</span></div><div className="sesiones-del-tratamiento">{!datos.sesiones.length ? <p className="estado-vacio">Este tratamiento todavía no tiene sesiones registradas.</p> : [...datos.sesiones].sort((a, b) => new Date(b.fechaHora) - new Date(a.fechaHora)).map((sesion) => <EventoHistoria key={sesion.id} evento={{ tipo: 'sesion', nombre: `Sesión N.º ${sesion.nroSesion}`, fecha: sesion.fechaHora, datos: sesion }} formatearFecha={formatearFecha} onConsultar={() => onConsultarSesion(sesion)} />)}</div></section></section>
  }
  const esEpicrisis = evento.tipo === 'epicrisis'
  return <section className="detalle-historia"><section className="panel"><p className="sobrelinea">Detalle de {esEpicrisis ? 'la epicrisis' : 'la sesión'}</p><h2>{esEpicrisis ? 'Epicrisis' : `Sesión N.º ${datos.nroSesion}`}</h2><EstadoVersion datos={datos} formatearFecha={formatearFecha} /><p className="fecha-detalle">{formatearFecha(evento.fecha)}</p><dl className="datos-evento">{!esEpicrisis && <div><dt>Tratamiento</dt><dd>{evento.tratamiento.nombre}</dd></div>}<div><dt>Ficha médica</dt><dd>{datos.nombreFichaSeguimiento || 'Sin ficha médica'}</dd></div>{!esEpicrisis && <div><dt>Número de sesión</dt><dd>{datos.nroSesion}</dd></div>}</dl><div className="observacion-detalle"><strong>Observaciones</strong><br />{datos.observaciones || 'Sin observaciones'}</div>{!rectificando && acciones}</section>{rectificando && <FormularioRectificacion evento={evento} idProfesional={idProfesional} cargando={cargando} onGuardar={onGuardarRectificacion} onCancelar={onCancelarRectificacion} />}<HistorialRectificaciones auditoria={auditoria} formatearFecha={formatearFecha} /><AdjuntosEventoHistoria evento={evento} archivos={archivos} /></section>
}

function EstadoVersion({ datos, formatearFecha }) {
  return <div className={`estado-version ${datos.estadoRegistro?.toLowerCase() || 'vigente'}`}><strong>Versión {datos.versionClinica || 1} · {datos.estadoRegistro || 'VIGENTE'}</strong>{datos.fechaUltimaRectificacion && <small>Última rectificación: {formatearFecha(datos.fechaUltimaRectificacion)}</small>}</div>
}

function FormularioRectificacion({ evento, idProfesional, cargando, onGuardar, onCancelar }) {
  const [tipoMotivo, setTipoMotivo] = useState('ERROR_TRANSCRIPCION')
  const [motivo, setMotivo] = useState('')
  const [nombre, setNombre] = useState(evento.datos.nombre || '')
  const [descripcion, setDescripcion] = useState(evento.datos.descripcion || '')
  const [total, setTotal] = useState(evento.datos.cantidadSesionesTotal || '')
  const [observaciones, setObservaciones] = useState(evento.datos.observaciones || '')
  const [fichasDisponibles, setFichasDisponibles] = useState([])
  const [fichaClinica, setFichaClinica] = useState(null)
  const [fichasCargadas, setFichasCargadas] = useState(evento.tipo === 'tratamiento')
  const [respuestasFicha, setRespuestasFicha] = useState(() => Object.fromEntries(
    (evento.datos.fichaCompletada?.respuestas || []).map((r) => [r.idOpcion, { ...r }])
  ))
  const anular = tipoMotivo === 'ANULACION_CARGA_ERRONEA'
  useEffect(() => {
    if (evento.tipo === 'tratamiento') return
    apiFichasMedicas.listar(idProfesional).then((fichas) => {
      setFichasDisponibles(fichas)
      setFichaClinica(fichas.find((f) => f.id === evento.datos.idFichaSeguimiento) || null)
    }).catch(() => setFichasDisponibles([])).finally(() => setFichasCargadas(true))
  }, [evento.tipo, evento.datos.idFichaSeguimiento, idProfesional])
  const seleccionarFicha = (id) => {
    const ficha = fichasDisponibles.find((f) => String(f.id) === id) || null
    setFichaClinica(ficha)
    if (!ficha) return setRespuestasFicha({})
    if (ficha.id === evento.datos.idFichaSeguimiento && evento.datos.fichaCompletada) {
      return setRespuestasFicha(Object.fromEntries(evento.datos.fichaCompletada.respuestas.map((r) => [r.idOpcion, { ...r }])))
    }
    const respuestas = {}; ficha.detalles.flatMap((d) => d.campos).flatMap((c) => c.opciones).forEach((o) => {
      respuestas[o.id] = { idOpcion: o.id, valor: null, seleccionada: o.tipo === 'SELECCION' ? false : null }
    }); setRespuestasFicha(respuestas)
  }
  const enviar = (e) => {
    e.preventDefault()
    if (!window.confirm(anular ? 'El registro quedará marcado como anulado, pero no será eliminado. ¿Confirmás?' : 'La versión anterior se conservará de forma inmutable. ¿Confirmás la rectificación?')) return
    const rectificacion = { versionEsperada: evento.datos.versionClinica || 1, tipoMotivo, motivo }
    if (evento.tipo === 'tratamiento') onGuardar({ rectificacion, nombre, descripcion, cantidadSesionesTotal: Number(total) })
    else onGuardar({ rectificacion, observaciones, idFichaSeguimiento: fichaClinica?.id || null,
      respuestasFichaSeguimiento: fichaClinica ? Object.values(respuestasFicha) : null })
  }
  return <form className="panel formulario-rectificacion" onSubmit={enviar}><div><p className="sobrelinea">Nueva versión clínica</p><h2>Rectificar sin sobrescribir</h2><p>La versión anterior permanecerá disponible en la auditoría.</p></div><label>Tipo de motivo<select value={tipoMotivo} onChange={(e) => setTipoMotivo(e.target.value)}><option value="ERROR_TRANSCRIPCION">Error de transcripción</option><option value="DATO_CLINICO_INCORRECTO">Dato clínico incorrecto</option><option value="ACLARACION">Aclaración</option><option value="INFORMACION_OMITIDA">Información omitida</option><option value="ANULACION_CARGA_ERRONEA">Anulación por carga errónea</option></select></label><label>Motivo detallado<textarea required minLength="10" maxLength="500" rows="4" value={motivo} onChange={(e) => setMotivo(e.target.value)} placeholder="Explicá concretamente por qué debe rectificarse." /><small>{motivo.length} / 500 caracteres</small></label>{anular ? <div className="aviso-anulacion">El registro seguirá existiendo y quedará identificado como anulado.</div> : evento.tipo === 'tratamiento' ? <><label>Nombre<input required maxLength="150" value={nombre} onChange={(e) => setNombre(e.target.value)} /></label><label>Cantidad total de sesiones<input required type="number" min="1" max="1000" value={total} onChange={(e) => setTotal(e.target.value)} /></label><label>Descripción<textarea maxLength="1000" rows="5" value={descripcion} onChange={(e) => setDescripcion(e.target.value)} /></label></> : <><label>Observaciones rectificadas<textarea required maxLength="1000" rows="8" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} /></label>{!fichasCargadas ? <p>Cargando fichas médicas…</p> : <><label>Ficha médica<select value={fichaClinica?.id || ''} onChange={(e) => seleccionarFicha(e.target.value)}><option value="">Sin ficha médica</option>{fichasDisponibles.map((f) => <option value={f.id} key={f.id}>{f.nombre}</option>)}</select></label>{fichaClinica && <AsignacionFicha fichas={[fichaClinica]} cargando={false} idsSeleccionadas={[String(fichaClinica.id)]} respuestas={respuestasFicha} setRespuestas={setRespuestasFicha} ocultarSelector onSeleccionar={() => {}} onActualizar={() => {}} />}</>}</>}<div className="acciones-rectificacion"><button type="button" className="boton-secundario" onClick={onCancelar} disabled={cargando}>Cancelar</button><button className="boton-principal" disabled={cargando || !fichasCargadas || motivo.trim().length < 10}>{cargando ? 'Registrando…' : 'Confirmar rectificación'}</button></div></form>
}

function HistorialRectificaciones({ auditoria, formatearFecha }) {
  if (auditoria === null) return null
  return <section className="panel historial-rectificaciones"><div><p className="sobrelinea">Trazabilidad</p><h2>Historial de rectificaciones</h2></div>{!auditoria.length ? <p className="estado-vacio">Este registro todavía no tiene rectificaciones.</p> : auditoria.map((item) => <details key={item.id}><summary><strong>Versión {item.versionAnterior} → {item.versionNueva}</strong><span>{item.tipoMotivo.replaceAll('_', ' ')}</span><small>{formatearFecha(item.fechaHoraUtc)} · Integridad {item.integridadValida ? 'verificada' : 'no válida'}</small></summary><p><b>Motivo:</b> {item.motivo}</p><p><b>Profesional:</b> {item.nombreProfesional || `ID ${item.idProfesional}`} {item.matriculaProfesional ? `· ${item.matriculaProfesional}` : ''}</p><p><b>IP:</b> {item.ipOrigen}</p><pre>{JSON.stringify({ antes: item.antes, despues: item.despues }, null, 2)}</pre></details>)}</section>
}

function GestionTratamientos({ onVolver }) {
  const [pantalla, setPantalla] = useState('buscar')
  const [idProfesional, setIdProfesional] = useState('1')
  const [pacientes, setPacientes] = useState([])
  const [busqueda, setBusqueda] = useState('')
  const [seleccionado, setSeleccionado] = useState(null)
  const [buscado, setBuscado] = useState(false)
  const [decision, setDecision] = useState(null)
  const [tratamiento, setTratamiento] = useState({ nombre: '', descripcion: '', cantidadSesionesTotal: '' })
  const [cargarPrimera, setCargarPrimera] = useState(false)
  const [observacionesSesion, setObservacionesSesion] = useState('')
  const [fichaSesion, setFichaSesion] = useState(null)
  const [respuestasFicha, setRespuestasFicha] = useState({})
  const [fichasDisponibles, setFichasDisponibles] = useState([])
  const [idFichaModal, setIdFichaModal] = useState('')
  const [modalAbierto, setModalAbierto] = useState(false)
  const [cargando, setCargando] = useState(false)
  const [mensaje, setMensaje] = useState(null)
  const [tratamientosActivos, setTratamientosActivos] = useState([])
  const [tratamientoSeleccionado, setTratamientoSeleccionado] = useState(null)
  const [tipoExito, setTipoExito] = useState('nuevo')
  const [pantallaAnteriorDatos, setPantallaAnteriorDatos] = useState('decision')
  const [adjuntosTratamiento, setAdjuntosTratamiento] = useState([])
  const [adjuntosSesion, setAdjuntosSesion] = useState([])
  const [resultadoAdjuntos, setResultadoAdjuntos] = useState(null)

  const normalizar = (valor) => valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[-,]/g, ' ').replace(/\s+/g, ' ').toLowerCase().trim()
  const filtrados = pacientes.filter((p) => normalizar(`${p.apellido} ${p.nombre}`).includes(normalizar(busqueda)))
  const buscar = async () => {
    if (!busqueda.trim()) return
    setCargando(true); setMensaje(null); setSeleccionado(null)
    try { setPacientes(await apiPacientes.listar(idProfesional)); setBuscado(true) }
    catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }
  const confirmarPaciente = () => {
    if (!seleccionado || !window.confirm(`¿Confirmás a ${seleccionado.apellido}, ${seleccionado.nombre} para gestionar sus tratamientos?`)) return
    setPantalla('decision'); window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const abrirFicha = async () => {
    setModalAbierto(true); setIdFichaModal(fichaSesion ? String(fichaSesion.id) : '')
    try { setFichasDisponibles(await apiFichasMedicas.listar(idProfesional)) }
    catch (error) { setModalAbierto(false); setMensaje({ tipo: 'error', texto: error.message }) }
  }
  const elegirFicha = () => {
    const ficha = fichasDisponibles.find((item) => String(item.id) === idFichaModal)
    if (!ficha) return
    const respuestas = {}
    ficha.detalles.flatMap((d) => d.campos).flatMap((c) => c.opciones).forEach((o) => {
      respuestas[o.id] = { idOpcion: o.id, valor: null, seleccionada: o.tipo === 'SELECCION' ? false : null }
    })
    setFichaSesion(ficha); setRespuestasFicha(respuestas); setModalAbierto(false)
  }
  const abrirTratamientosActivos = async () => {
    setCargando(true); setMensaje(null); setTratamientoSeleccionado(null)
    try {
      setTratamientosActivos(await apiTratamientos.listarSinTerminar(idProfesional, seleccionado.id))
      setDecision('continuar'); setPantalla('seleccionar-tratamiento')
      window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }
  const abrirRegistroSesion = () => {
    if (!tratamientoSeleccionado) return
    setObservacionesSesion(''); setFichaSesion(null); setRespuestasFicha({})
    setAdjuntosSesion([]); setResultadoAdjuntos(null)
    setPantalla('continuar'); window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const cancelarRegistroSesion = () => {
    if (!window.confirm('¿Confirmás que querés cancelar el registro de esta sesión? Los datos ingresados se perderán.')) return
    setObservacionesSesion(''); setFichaSesion(null); setRespuestasFicha({}); setAdjuntosSesion([]); setMensaje(null)
    setPantalla('seleccionar-tratamiento'); window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const consultarDatosPaciente = (origen) => {
    setPantallaAnteriorDatos(origen)
    setPantalla('datos')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
  const registrarSesion = async (evento) => {
    evento.preventDefault()
    if (!window.confirm(`¿Confirmás el registro de la sesión N.º ${tratamientoSeleccionado.cantidadSesionesTotal - tratamientoSeleccionado.cantidadSesionesFaltantes + 1} de “${tratamientoSeleccionado.nombre}”?`)) return
    setCargando(true); setMensaje(null)
    try {
      const actualizado = await apiTratamientos.registrarSesion(idProfesional, seleccionado.id, tratamientoSeleccionado.id, {
        observaciones: observacionesSesion.trim() || 'Sin observaciones', idFichaSeguimiento: fichaSesion?.id || null,
        respuestasFichaSeguimiento: fichaSesion ? Object.values(respuestasFicha) : null,
      })
      const sesionCreada = [...(actualizado.sesiones || [])].sort((a, b) => b.nroSesion - a.nroSesion)[0]
      const resultado = sesionCreada
        ? await subirAdjuntosClinicos(adjuntosSesion, (item) => apiArchivos.adjuntarASesion(sesionCreada.id, item))
        : { cargados: [], fallidos: adjuntosSesion.map((adjunto) => ({ adjunto, error: 'No se pudo resolver la sesión creada.' })), advertencias: [] }
      setResultadoAdjuntos(resultado)
      setTipoExito('sesion'); setPantalla('exito'); window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }
  const registrar = async (evento) => {
    evento.preventDefault()
    if (!window.confirm(`¿Confirmás el nuevo tratamiento para ${seleccionado.apellido}, ${seleccionado.nombre}?`)) return
    setCargando(true); setMensaje(null)
    try {
      const creado = await apiTratamientos.crear(idProfesional, seleccionado.id, {
        nombre: tratamiento.nombre, descripcion: tratamiento.descripcion,
        cantidadSesionesTotal: Number(tratamiento.cantidadSesionesTotal),
        primeraSesion: cargarPrimera ? { observaciones: observacionesSesion.trim() || 'Sin observaciones', idFichaSeguimiento: fichaSesion?.id || null,
          respuestasFichaSeguimiento: fichaSesion ? Object.values(respuestasFicha) : null } : null,
      })
      const resultadoTratamiento = await subirAdjuntosClinicos(adjuntosTratamiento,
        (item) => apiArchivos.adjuntarATratamiento(creado.id, item))
      let resultadoSesion = { cargados: [], fallidos: [], advertencias: [] }
      if (cargarPrimera && adjuntosSesion.length) {
        const sesionCreada = [...(creado.sesiones || [])].sort((a, b) => b.nroSesion - a.nroSesion)[0]
        resultadoSesion = sesionCreada
          ? await subirAdjuntosClinicos(adjuntosSesion, (item) => apiArchivos.adjuntarASesion(sesionCreada.id, item))
          : { cargados: [], fallidos: adjuntosSesion.map((adjunto) => ({ adjunto, error: 'No se pudo resolver la primera sesión.' })), advertencias: [] }
      }
      setResultadoAdjuntos(combinarResultadosAdjuntos(resultadoTratamiento, resultadoSesion))
      setTipoExito('nuevo'); setPantalla('exito'); window.scrollTo({ top: 0, behavior: 'smooth' })
    } catch (error) { setMensaje({ tipo: 'error', texto: error.message }) }
    finally { setCargando(false) }
  }

  if (pantalla === 'exito') return <main className="contenido pagina-fichas pagina-tratamientos"><section className="panel resultado-epicrisis-exitoso" role="status"><span className="icono-exito-epicrisis">✓</span><p className="sobrelinea">Registro completado</p><h1>{tipoExito === 'sesion' ? 'Sesión registrada con éxito' : 'Tratamiento asignado con éxito'}</h1><p>{tipoExito === 'sesion' ? `La nueva sesión de “${tratamientoSeleccionado.nombre}” fue guardada correctamente.` : `El tratamiento de ${seleccionado.apellido}, ${seleccionado.nombre} fue guardado correctamente.`}</p><ResultadoCargaAdjuntos resultado={resultadoAdjuntos} /><button className="boton-principal" onClick={onVolver}>Volver al panel principal</button></section></main>
  if (pantalla === 'datos') return <main className="contenido pagina-fichas pagina-tratamientos"><button className="volver" onClick={() => setPantalla(pantallaAnteriorDatos)}>← Volver a tratamientos</button><VistaCompletaPaciente paciente={seleccionado} /></main>
  const volver = pantalla === 'buscar' ? onVolver : () => {
    if (pantalla === 'nuevo' || pantalla === 'seleccionar-tratamiento') setPantalla('decision')
    else if (pantalla === 'continuar') setPantalla('seleccionar-tratamiento')
    else setPantalla('buscar')
    if (pantalla === 'decision') setDecision(null)
  }
  return <main className="contenido pagina-fichas pagina-tratamientos">
    <button className="volver" onClick={volver}>← {pantalla === 'buscar' ? 'Volver al inicio' : pantalla === 'decision' ? 'Volver a buscar pacientes' : pantalla === 'continuar' ? 'Volver a seleccionar tratamiento' : 'Volver a elegir una acción'}</button>
    <div className="cabecera-pagina"><div><p className="sobrelinea">Registro clínico</p><h1>Tratamientos</h1><p>{pantalla === 'buscar' ? 'Buscá y seleccioná al paciente.' : 'Gestioná el tratamiento del paciente seleccionado.'}</p></div>{pantalla === 'buscar' && <label className="profesional">ID del profesional<input type="number" min="1" value={idProfesional} onChange={(e) => setIdProfesional(e.target.value)} /></label>}</div>
    {mensaje && <div className={`mensaje ${mensaje.tipo}`}>{mensaje.texto}</div>}
    {pantalla === 'buscar' && <section className="panel buscador-paciente-epicrisis"><div className="titulo-paso"><h2>Buscar paciente</h2><p>Ingresá apellido o nombre para buscar coincidencias.</p></div><div className="fila-busqueda-epicrisis"><label className="buscador-epicrisis">Apellido - nombre<input autoFocus type="search" value={busqueda} onChange={(e) => { setBusqueda(e.target.value); setSeleccionado(null) }} onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); buscar() } }} placeholder="Ej. Pérez - Ana" /></label><button className="boton-principal" onClick={buscar} disabled={cargando || !busqueda.trim()}>{cargando ? 'Buscando…' : 'Buscar'}</button></div><div className="resultados-pacientes-epicrisis">{!buscado ? <p>Los pacientes encontrados aparecerán aquí.</p> : !filtrados.length ? <p>No se encontraron pacientes.</p> : filtrados.map((p) => <label className={seleccionado?.id === p.id ? 'seleccionado' : ''} key={p.id}><input type="radio" name="paciente-tratamiento" checked={seleccionado?.id === p.id} onChange={() => setSeleccionado(p)} /><span><strong>{p.apellido}, {p.nombre}</strong><small>DNI {p.dni}</small></span></label>)}</div><div className="confirmar-paciente-epicrisis"><span>{seleccionado ? `Seleccionado: ${seleccionado.apellido}, ${seleccionado.nombre}` : 'Seleccioná un paciente para continuar.'}</span><button className="boton-principal" disabled={!seleccionado} onClick={confirmarPaciente}>Confirmar paciente</button></div></section>}
    {pantalla === 'decision' && <><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div><button className="boton-secundario" onClick={() => consultarDatosPaciente('decision')}>Consultar todos los datos</button></section><section className="panel decision-tratamiento"><div className="titulo-paso"><h2>¿Qué querés hacer?</h2><p>Elegí cómo continuar con este paciente.</p></div><div className="opciones-tratamiento"><button onClick={() => { setDecision('nuevo'); setAdjuntosTratamiento([]); setAdjuntosSesion([]); setResultadoAdjuntos(null); setPantalla('nuevo') }}><strong>Asignar nuevo tratamiento</strong><span>Definí el tratamiento y, si corresponde, cargá la primera sesión.</span></button><button onClick={abrirTratamientosActivos} disabled={cargando}><strong>Continuar un tratamiento</strong><span>Seleccioná un tratamiento sin terminar y registrá la próxima sesión.</span></button></div></section></>}
    {pantalla === 'seleccionar-tratamiento' && <><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Paciente seleccionado</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div><button className="boton-secundario" onClick={() => consultarDatosPaciente('seleccionar-tratamiento')}>Consultar todos los datos</button></section><section className="panel seleccion-tratamiento"><div className="titulo-paso"><h2>Tratamientos sin terminar</h2><p>Seleccioná el tratamiento que querés continuar.</p></div><div className="lista-tratamientos-activos">{!tratamientosActivos.length ? <p>El paciente no tiene tratamientos pendientes.</p> : tratamientosActivos.map((item) => { const realizadas = item.cantidadSesionesTotal - item.cantidadSesionesFaltantes; return <label className={tratamientoSeleccionado?.id === item.id ? 'seleccionado' : ''} key={item.id}><input type="radio" name="tratamiento-activo" checked={tratamientoSeleccionado?.id === item.id} onChange={() => setTratamientoSeleccionado(item)} /><span><strong>{item.nombre}</strong><small>{realizadas} sesiones realizadas de {item.cantidadSesionesTotal}</small></span><em>{item.cantidadSesionesFaltantes} pendientes</em></label> })}</div><div className="pie-seleccion-tratamiento"><span>{tratamientoSeleccionado ? `Seleccionado: ${tratamientoSeleccionado.nombre}` : 'Seleccioná un tratamiento para continuar.'}</span><button className="boton-principal" disabled={!tratamientoSeleccionado} onClick={abrirRegistroSesion}>Continuar tratamiento</button></div></section></>}
    {pantalla === 'continuar' && <form className="formulario-registro-epicrisis" onSubmit={registrarSesion}><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Nueva sesión para</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div><button type="button" className="boton-secundario" onClick={() => consultarDatosPaciente('continuar')}>Consultar todos los datos</button></section><section className="panel resumen-tratamiento"><div><p className="sobrelinea">Próxima sesión</p><h2>{tratamientoSeleccionado.nombre}</h2><p>Sesión N.º {tratamientoSeleccionado.cantidadSesionesTotal - tratamientoSeleccionado.cantidadSesionesFaltantes + 1} de {tratamientoSeleccionado.cantidadSesionesTotal}</p></div><span>{tratamientoSeleccionado.cantidadSesionesFaltantes} sesiones pendientes</span></section><section className="panel primera-sesion"><div className="datos-primera-sesion datos-continuar-sesion"><div className="seccion-ficha-seguimiento"><div><h3>Ficha médica de la sesión</h3><p>Opcionalmente, asigná y completá una ficha médica.</p></div>{fichaSesion ? <div className="ficha-seguimiento-seleccionada"><span><strong>{fichaSesion.nombre}</strong></span><button type="button" onClick={abrirFicha}>Cambiar</button><button type="button" className="quitar-ficha-seguimiento" onClick={() => { setFichaSesion(null); setRespuestasFicha({}) }}>Quitar</button></div> : <button type="button" className="boton-secundario" onClick={abrirFicha}>Agregar ficha médica</button>}</div>{fichaSesion && <AsignacionFicha fichas={[fichaSesion]} cargando={false} idsSeleccionadas={[String(fichaSesion.id)]} respuestas={respuestasFicha} setRespuestas={setRespuestasFicha} ocultarSelector onSeleccionar={() => {}} onActualizar={abrirFicha} />}<label>Observaciones de la sesión <span>(opcional)</span><textarea maxLength="1000" rows="8" value={observacionesSesion} onChange={(e) => setObservacionesSesion(e.target.value)} placeholder="Si no ingresás nada, se guardará “Sin observaciones”." /><small>{observacionesSesion.length} / 1000 caracteres</small></label></div></section><AdjuntosClinicosInput adjuntos={adjuntosSesion} onChange={setAdjuntosSesion} disabled={cargando} titulo="Archivos de la sesión" descripcion="Estos archivos quedarán vinculados exclusivamente a la nueva sesión." /><section className="panel acciones-registro-sesion"><button type="button" className="boton-secundario" onClick={cancelarRegistroSesion} disabled={cargando}>Cancelar registro</button><button className="boton-principal" disabled={cargando}>{cargando ? 'Registrando y analizando archivos…' : 'Confirmar registro'}</button></section></form>}
    {pantalla === 'nuevo' && <form className="formulario-registro-epicrisis" onSubmit={registrar}><section className="panel identidad-paciente-epicrisis"><div><p className="sobrelinea">Nuevo tratamiento para</p><h2>{seleccionado.apellido}, {seleccionado.nombre}</h2><p>DNI {seleccionado.dni}</p></div><button type="button" className="boton-secundario" onClick={() => consultarDatosPaciente('nuevo')}>Consultar todos los datos</button></section><section className="panel formulario-tratamiento"><div className="titulo-paso"><h2>Datos del tratamiento</h2><p>Completá la planificación general.</p></div><div className="grilla-datos-tratamiento"><label>Nombre<input required maxLength="150" value={tratamiento.nombre} onChange={(e) => setTratamiento({ ...tratamiento, nombre: e.target.value })} /></label><label>Cantidad total de sesiones<input required type="number" min="1" max="1000" value={tratamiento.cantidadSesionesTotal} onChange={(e) => setTratamiento({ ...tratamiento, cantidadSesionesTotal: e.target.value })} /></label><label className="campo-ancho">Descripción<textarea maxLength="1000" rows="5" value={tratamiento.descripcion} onChange={(e) => setTratamiento({ ...tratamiento, descripcion: e.target.value })} /></label></div></section><AdjuntosClinicosInput adjuntos={adjuntosTratamiento} onChange={setAdjuntosTratamiento} disabled={cargando} titulo="Archivos del tratamiento" descripcion="Estos archivos quedarán vinculados al tratamiento, no a una sesión particular." /><section className="panel primera-sesion"><label className="interruptor-sesion"><input type="checkbox" checked={cargarPrimera} onChange={(e) => { setCargarPrimera(e.target.checked); if (!e.target.checked) { setFichaSesion(null); setRespuestasFicha({}); setAdjuntosSesion([]) } }} /><span><strong>Cargar la primera sesión ahora</strong><small>Se registrará como sesión N.º 1 y se descontará de las sesiones faltantes.</small></span></label>{cargarPrimera && <div className="datos-primera-sesion"><div className="seccion-ficha-seguimiento"><div><h3>Ficha médica de la sesión</h3><p>Opcionalmente, asigná y completá una ficha médica.</p></div>{fichaSesion ? <div className="ficha-seguimiento-seleccionada"><span><strong>{fichaSesion.nombre}</strong></span><button type="button" onClick={abrirFicha}>Cambiar</button><button type="button" className="quitar-ficha-seguimiento" onClick={() => { setFichaSesion(null); setRespuestasFicha({}) }}>Quitar</button></div> : <button type="button" className="boton-secundario" onClick={abrirFicha}>Agregar ficha médica</button>}</div>{fichaSesion && <AsignacionFicha fichas={[fichaSesion]} cargando={false} idsSeleccionadas={[String(fichaSesion.id)]} respuestas={respuestasFicha} setRespuestas={setRespuestasFicha} ocultarSelector onSeleccionar={() => {}} onActualizar={abrirFicha} />}<label>Observaciones de la primera sesión <span>(opcional)</span><textarea maxLength="1000" rows="7" value={observacionesSesion} onChange={(e) => setObservacionesSesion(e.target.value)} placeholder="Si no ingresás nada, se guardará “Sin observaciones”." /></label></div>}</section>{cargarPrimera && <AdjuntosClinicosInput adjuntos={adjuntosSesion} onChange={setAdjuntosSesion} disabled={cargando} titulo="Archivos de la primera sesión" descripcion="Estos archivos quedarán vinculados a la sesión N.º 1." />}<section className="panel acciones-tratamiento"><p>Se solicitará confirmación antes de guardar.</p><button className="boton-principal" disabled={cargando}>{cargando ? 'Registrando y analizando archivos…' : 'Asignar tratamiento'}</button></section></form>}
    {modalAbierto && <div className="fondo-modal-ficha" onMouseDown={(e) => { if (e.target === e.currentTarget) setModalAbierto(false) }}><section className="modal-ficha-seguimiento" role="dialog" aria-modal="true"><header><div><p className="sobrelinea">Plantillas disponibles</p><h2>Ficha médica de la sesión</h2></div><button type="button" onClick={() => setModalAbierto(false)}>×</button></header><div className="lista-modal-fichas">{!fichasDisponibles.length ? <p className="estado-modal-fichas">No hay fichas médicas disponibles.</p> : fichasDisponibles.map((f) => <label className={idFichaModal === String(f.id) ? 'seleccionada' : ''} key={f.id}><input type="radio" checked={idFichaModal === String(f.id)} onChange={() => setIdFichaModal(String(f.id))} /><span><strong>{f.nombre}</strong><small>{f.descripcion || `${f.detalles.length} secciones`}</small></span></label>)}</div><footer><button type="button" className="boton-secundario" onClick={() => setModalAbierto(false)}>Cancelar</button><button type="button" className="boton-principal" disabled={!idFichaModal} onClick={elegirFicha}>Agregar</button></footer></section></div>}
  </main>
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
  const [adjuntosEpicrisis, setAdjuntosEpicrisis] = useState([])
  const [resultadoAdjuntos, setResultadoAdjuntos] = useState(null)

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
    setMensaje(null); setAdjuntosEpicrisis([]); setResultadoAdjuntos(null); setPantalla('registrar'); window.scrollTo({ top: 0, behavior: 'smooth' })
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
      const creada = await apiEpicrisis.registrar(idProfesional, seleccionado.id, {
        observaciones: observaciones.trim() || 'Sin observaciones',
        idFichaSeguimiento: fichaSeguimiento?.id || null,
        respuestasFichaSeguimiento: fichaSeguimiento ? Object.values(respuestasSeguimiento) : null,
      })
      const resultado = await subirAdjuntosClinicos(adjuntosEpicrisis,
        (item) => apiArchivos.adjuntarAEpicrisis(creada.id, item))
      setResultadoAdjuntos(resultado)
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
      <ResultadoCargaAdjuntos resultado={resultadoAdjuntos} />
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
      <AdjuntosClinicosInput adjuntos={adjuntosEpicrisis} onChange={setAdjuntosEpicrisis} disabled={cargando} titulo="Archivos de la epicrisis" descripcion="Estos archivos quedarán vinculados exclusivamente a esta epicrisis." />
      <section className="panel seccion-observaciones-epicrisis"><div className="titulo-paso"><h2>Observaciones</h2><p>Ingresá la síntesis clínica correspondiente a esta epicrisis.</p></div><label className="observaciones-epicrisis">Síntesis clínica <span>(opcional)</span><textarea autoFocus maxLength="1000" rows="12" value={observaciones} onChange={(e) => setObservaciones(e.target.value)} placeholder="Si no ingresás nada, se guardará “Sin observaciones”." /><small>{observaciones.length} / 1000 caracteres</small></label><div className="acciones-epicrisis"><p>Se solicitará confirmación antes de guardar.</p><button className="boton-principal" disabled={cargando}>{cargando ? 'Registrando y analizando archivos…' : 'Registrar epicrisis'}</button></div></section>
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
      const solicitud = {
        ...paciente,
        nombre: capitalizarPalabras(paciente.nombre),
        apellido: capitalizarPalabras(paciente.apellido),
      }
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
      nombre: capitalizarPalabras(seleccionado.nombre),
      apellido: capitalizarPalabras(seleccionado.apellido),
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
            <label>Nombre<input required maxLength="100" value={paciente.nombre} onChange={(e) => setPaciente({ ...paciente, nombre: capitalizarPalabras(e.target.value) })} /></label>
            <label>Apellido<input required maxLength="100" value={paciente.apellido} onChange={(e) => setPaciente({ ...paciente, apellido: capitalizarPalabras(e.target.value) })} /></label>
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
