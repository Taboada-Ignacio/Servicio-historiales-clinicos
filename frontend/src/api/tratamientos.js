const base = (idProfesional, idPaciente) => `/api/v1/profesionales/${idProfesional}/pacientes/${idPaciente}/tratamientos`
async function solicitar(url, opciones = {}) {
  const respuesta = await fetch(url, { headers: { 'Content-Type': 'application/json', ...opciones.headers }, ...opciones })
  if (!respuesta.ok) { const error = await respuesta.json().catch(() => null); throw new Error(error?.mensaje || `La operación falló (${respuesta.status})`) }
  return respuesta.status === 204 ? null : respuesta.json()
}
export const apiTratamientos = {
  crear: (idProfesional, idPaciente, datos) => solicitar(base(idProfesional, idPaciente), { method: 'POST', body: JSON.stringify(datos) }),
  listar: (idProfesional, idPaciente) => solicitar(base(idProfesional, idPaciente)),
  listarSinTerminar: (idProfesional, idPaciente) => solicitar(`${base(idProfesional, idPaciente)}/sin-terminar`),
  registrarSesion: (idProfesional, idPaciente, idTratamiento, datos) => solicitar(`${base(idProfesional, idPaciente)}/${idTratamiento}/sesiones`, {
    method: 'POST', body: JSON.stringify(datos),
  }),
}
