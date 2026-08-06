const base = (idProfesional, idPaciente) =>
  `/api/v1/profesionales/${idProfesional}/pacientes/${idPaciente}/epicrisis`

async function solicitar(url, opciones = {}) {
  const respuesta = await fetch(url, {
    headers: { 'Content-Type': 'application/json', ...opciones.headers },
    ...opciones,
  })
  if (!respuesta.ok) {
    const error = await respuesta.json().catch(() => null)
    throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`)
  }
  return respuesta.status === 204 ? null : respuesta.json()
}

export const apiEpicrisis = {
  registrar: (idProfesional, idPaciente, datos) => solicitar(base(idProfesional, idPaciente), {
    method: 'POST',
    body: JSON.stringify(datos),
  }),
  listar: (idProfesional, idPaciente) => solicitar(base(idProfesional, idPaciente)),
}
