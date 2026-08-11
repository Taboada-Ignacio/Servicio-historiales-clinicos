function obtenerDispositivo() {
  let id = localStorage.getItem('dispositivo-clinico')
  if (!id) { id = crypto.randomUUID(); localStorage.setItem('dispositivo-clinico', id) }
  return id
}

const headersSolicitud = () => ({
  'X-Request-Id': crypto.randomUUID(),
  'X-Device-Id': obtenerDispositivo(),
})

function limiteSegunArchivo(adjunto) {
  const extension = adjunto?.file?.name?.split('.').pop()?.toLowerCase()
  const imagen = ['jpg', 'jpeg', 'png'].includes(extension)
  return { extension: extension?.toUpperCase() || 'seleccionado', megabytes: imagen ? 15 : 20 }
}

async function leerError(respuesta, adjunto) {
  const tipoContenido = respuesta.headers.get('content-type') || ''
  let error = null
  let texto = ''
  if (tipoContenido.includes('json')) error = await respuesta.json().catch(() => null)
  else texto = await respuesta.text().catch(() => '')

  const violaciones = Array.isArray(error?.violaciones)
    ? error.violaciones.map((item) => item.mensaje).filter(Boolean)
    : []
  const mensajeBackend = error?.mensaje || error?.message || error?.detail
    || (texto && texto.length <= 300 && !texto.trim().startsWith('<') ? texto.trim() : '')
  if (respuesta.status === 413) {
    const limite = limiteSegunArchivo(adjunto)
    throw new Error(`El archivo supera el tamaño permitido para ${limite.extension}: máximo ${limite.megabytes} MB.`)
  }
  if (mensajeBackend) throw new Error([mensajeBackend, ...violaciones].join(' '))
  if (respuesta.status === 422) throw new Error('El archivo fue rechazado por el análisis de seguridad. Verificá que no contenga malware.')
  if (respuesta.status === 415) throw new Error('El formato o el contenido real del archivo no está permitido.')
  if (respuesta.status === 400) throw new Error('El archivo es inválido. Verificá su formato, contenido y tamaño.')
  if (respuesta.status === 403) throw new Error('No tenés autorización para adjuntar archivos a este paciente o registro clínico.')
  if (respuesta.status === 404) throw new Error('No se encontró el paciente o registro clínico al que intentás adjuntar el archivo.')
  throw new Error(`No se pudo cargar el archivo. El servidor respondió con el código ${respuesta.status}.`)
}

async function adjuntar(url, adjunto) {
  const datos = new FormData()
  datos.append('archivo', adjunto.file, adjunto.file.name)
  datos.append('categoria', adjunto.categoria)
  if (adjunto.descripcion?.trim()) datos.append('descripcion', adjunto.descripcion.trim())
  const respuesta = await fetch(url, { method: 'POST', headers: headersSolicitud(), body: datos })
  if (!respuesta.ok) return leerError(respuesta, adjunto)
  return respuesta.json()
}

async function solicitar(url) {
  const respuesta = await fetch(url, { headers: headersSolicitud() })
  if (!respuesta.ok) return leerError(respuesta)
  return respuesta.json()
}

async function obtenerContenido(id) {
  const respuesta = await fetch(`/api/archivos/${id}/download`, { headers: headersSolicitud() })
  if (!respuesta.ok) return leerError(respuesta)
  return respuesta.blob()
}

async function obtenerVistaPrevia(id) {
  const respuesta = await fetch(`/api/archivos/${id}/preview`, { headers: headersSolicitud() })
  if (!respuesta.ok) return leerError(respuesta)
  return respuesta.blob()
}

async function descargar(id, nombre) {
  const contenido = await obtenerContenido(id)
  const enlace = document.createElement('a')
  enlace.href = URL.createObjectURL(contenido)
  enlace.download = nombre || 'archivo-clinico'
  document.body.appendChild(enlace)
  enlace.click()
  enlace.remove()
  URL.revokeObjectURL(enlace.href)
}

export const apiArchivos = {
  adjuntarAPaciente: (pacienteId, adjunto) => adjuntar(`/api/pacientes/${pacienteId}/archivos`, adjunto),
  adjuntarATratamiento: (tratamientoId, adjunto) => adjuntar(`/api/tratamientos/${tratamientoId}/archivos`, adjunto),
  adjuntarASesion: (sesionId, adjunto) => adjuntar(`/api/sesiones/${sesionId}/archivos`, adjunto),
  adjuntarAEpicrisis: (epicrisisId, adjunto) => adjuntar(`/api/epicrisis/${epicrisisId}/archivos`, adjunto),
  listarDelPaciente: (pacienteId) => solicitar(`/api/pacientes/${pacienteId}/archivos`),
  obtenerContenido,
  obtenerVistaPrevia,
  descargar,
}
