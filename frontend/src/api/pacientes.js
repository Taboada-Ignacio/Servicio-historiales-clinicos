const base = (idProfesional) => `/api/v1/profesionales/${idProfesional}/pacientes`

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

export const apiPacientes = {
  listar: (idProfesional) => solicitar(base(idProfesional)),
  buscar: (idProfesional, idPaciente) => solicitar(`${base(idProfesional)}/${idPaciente}`),
  crear: (idProfesional, paciente) => solicitar(base(idProfesional), {
    method: 'POST',
    body: JSON.stringify(paciente),
  }),
  actualizar: (idProfesional, idPaciente, paciente) => solicitar(`${base(idProfesional)}/${idPaciente}`, {
    method: 'PUT',
    body: JSON.stringify(paciente),
  }),
  eliminar: (idProfesional, idPaciente) => solicitar(`${base(idProfesional)}/${idPaciente}`, {
    method: 'DELETE',
  }),
}
