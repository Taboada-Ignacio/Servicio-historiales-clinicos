const base = (idProfesional, idPaciente) =>
  `/api/v1/profesionales/${idProfesional}/pacientes/${idPaciente}/epicrisis`

async function solicitar(url, opciones = {}) {
  const respuesta = await fetch(url, {
    headers: { 'Content-Type': 'application/json', 'X-Request-Id': crypto.randomUUID(),
      'X-Device-Id': obtenerDispositivo(), ...opciones.headers },
    ...opciones,
  })
  if (!respuesta.ok) {
    const error = await respuesta.json().catch(() => null)
    throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`)
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

function obtenerDispositivo() {
  let id = localStorage.getItem('dispositivo-clinico')
  if (!id) { id = crypto.randomUUID(); localStorage.setItem('dispositivo-clinico', id) }
  return id
}

async function descargar(url, nombre) {
  const respuesta = await fetch(url, { headers: { 'X-Request-Id': crypto.randomUUID(), 'X-Device-Id': obtenerDispositivo() } })
  if (!respuesta.ok) { const error = await respuesta.json().catch(() => null); throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`) }
  const enlace = document.createElement('a'); enlace.href = URL.createObjectURL(await respuesta.blob()); enlace.download = nombre
  enlace.click(); URL.revokeObjectURL(enlace.href)
}

export const apiEpicrisis = {
  registrar: (idProfesional, idPaciente, datos) => solicitar(base(idProfesional, idPaciente), {
    method: 'POST',
    body: JSON.stringify(datos),
  }),
  listar: (idProfesional, idPaciente) => solicitar(base(idProfesional, idPaciente)),
  rectificar: (idProfesional, idPaciente, idEpicrisis, datos) => solicitar(`${base(idProfesional, idPaciente)}/${idEpicrisis}/rectificaciones`, {
    method: 'POST', body: JSON.stringify(datos),
  }),
  auditoria: (idProfesional, idPaciente, idEpicrisis) => solicitar(`${base(idProfesional, idPaciente)}/${idEpicrisis}/auditoria`),
  descargarInforme: (idProfesional, idPaciente, idEpicrisis) => descargar(
    `${base(idProfesional, idPaciente)}/${idEpicrisis}/informe-auditoria`, `auditoria-epicrisis-${idEpicrisis}.json`),
}
