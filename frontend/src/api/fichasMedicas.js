const base = (idProfesional) => `/api/v1/profesionales/${idProfesional}/fichas-medicas`

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

export const apiFichasMedicas = {
  listar: (idProfesional) => solicitar(base(idProfesional)),
  crear: (idProfesional, ficha) => solicitar(base(idProfesional), {
    method: 'POST',
    body: JSON.stringify(ficha),
  }),
  actualizar: (idProfesional, idFicha, ficha) => solicitar(`${base(idProfesional)}/${idFicha}`, {
    method: 'PUT',
    body: JSON.stringify(ficha),
  }),
  eliminar: (idProfesional, idFicha) => solicitar(`${base(idProfesional)}/${idFicha}`, {
    method: 'DELETE',
  }),
}
