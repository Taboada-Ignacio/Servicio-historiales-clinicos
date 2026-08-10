async function errorDe(respuesta) {
  const error = await respuesta.json().catch(() => null)
  return new Error(error?.mensaje || `La operación falló (${respuesta.status})`)
}

function nombreDescarga(respuesta, formato) {
  const disposicion = respuesta.headers.get('Content-Disposition') || ''
  const utf8 = disposicion.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8) return decodeURIComponent(utf8[1])
  const simple = disposicion.match(/filename="?([^";]+)"?/i)
  return simple?.[1] || `historia-clinica.${formato.toLowerCase()}`
}

export const apiExportacionHistoriaClinica = {
  listarPacientes: async () => {
    const respuesta = await fetch('/api/pacientes')
    if (!respuesta.ok) throw await errorDe(respuesta)
    return respuesta.json()
  },
  exportar: async (pacienteId, datos) => {
    const respuesta = await fetch(`/api/pacientes/${pacienteId}/historia-clinica/exportar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(datos),
    })
    if (!respuesta.ok) throw await errorDe(respuesta)
    const enlace = document.createElement('a')
    const url = URL.createObjectURL(await respuesta.blob())
    enlace.href = url
    enlace.download = nombreDescarga(respuesta, datos.formato)
    document.body.appendChild(enlace)
    enlace.click()
    enlace.remove()
    URL.revokeObjectURL(url)
    return enlace.download
  },
}
