const base = (idProfesional, idPaciente) => `/api/v1/profesionales/${idProfesional}/pacientes/${idPaciente}/tratamientos`
function obtenerDispositivo() {
  let id = localStorage.getItem('dispositivo-clinico')
  if (!id) { id = crypto.randomUUID(); localStorage.setItem('dispositivo-clinico', id) }
  return id
}
async function solicitar(url, opciones = {}) {
  const respuesta = await fetch(url, { headers: { 'Content-Type': 'application/json', 'X-Request-Id': crypto.randomUUID(),
    'X-Device-Id': obtenerDispositivo(), ...opciones.headers }, ...opciones })
  if (!respuesta.ok) { const error = await respuesta.json().catch(() => null); throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`) }
  return respuesta.status === 204 ? null : respuesta.json()
}
async function descargar(url, nombre) {
  const respuesta = await fetch(url, { headers: { 'X-Request-Id': crypto.randomUUID(), 'X-Device-Id': obtenerDispositivo() } })
  if (!respuesta.ok) { const error = await respuesta.json().catch(() => null); throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`) }
  const enlace = document.createElement('a'); enlace.href = URL.createObjectURL(await respuesta.blob()); enlace.download = nombre
  enlace.click(); URL.revokeObjectURL(enlace.href)
}
export const apiTratamientos = {
  crear: (idProfesional, idPaciente, datos) => solicitar(base(idProfesional, idPaciente), { method: 'POST', body: JSON.stringify(datos) }),
  listar: (idProfesional, idPaciente) => solicitar(base(idProfesional, idPaciente)),
  listarSinTerminar: (idProfesional, idPaciente) => solicitar(`${base(idProfesional, idPaciente)}/sin-terminar`),
  registrarSesion: (idProfesional, idPaciente, idTratamiento, datos) => solicitar(`${base(idProfesional, idPaciente)}/${idTratamiento}/sesiones`, {
    method: 'POST', body: JSON.stringify(datos),
  }),
  rectificar: (idProfesional, idPaciente, idTratamiento, datos) => solicitar(`${base(idProfesional, idPaciente)}/${idTratamiento}/rectificaciones`, {
    method: 'POST', body: JSON.stringify(datos),
  }),
  rectificarSesion: (idProfesional, idPaciente, idTratamiento, idSesion, datos) => solicitar(
    `${base(idProfesional, idPaciente)}/${idTratamiento}/sesiones/${idSesion}/rectificaciones`, { method: 'POST', body: JSON.stringify(datos) }),
  auditoria: (idProfesional, idPaciente, idTratamiento) => solicitar(`${base(idProfesional, idPaciente)}/${idTratamiento}/auditoria`),
  auditoriaSesion: (idProfesional, idPaciente, idTratamiento, idSesion) => solicitar(
    `${base(idProfesional, idPaciente)}/${idTratamiento}/sesiones/${idSesion}/auditoria`),
  descargarInforme: (idProfesional, idPaciente, idTratamiento) => descargar(
    `${base(idProfesional, idPaciente)}/${idTratamiento}/informe-auditoria`, `auditoria-tratamiento-${idTratamiento}.json`),
  descargarInformeSesion: (idProfesional, idPaciente, idTratamiento, idSesion) => descargar(
    `${base(idProfesional, idPaciente)}/${idTratamiento}/sesiones/${idSesion}/informe-auditoria`, `auditoria-sesion-${idSesion}.json`),
}
