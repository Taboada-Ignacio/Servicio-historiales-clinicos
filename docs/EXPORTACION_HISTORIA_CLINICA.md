# Exportación de historia clínica

## Alcance

El módulo permite que un profesional autenticado descargue inmediatamente la historia clínica completa de uno de sus pacientes en `PDF`, `DOCX`, `CSV` o `XLSX`. No crea solicitudes pendientes ni implementa aprobaciones, representantes, entregas diferidas, estados o prórrogas.

El archivo contiene:

- nombre, apellido, DNI, fecha de nacimiento, sexo y teléfono del paciente;
- fichas médicas asignadas directamente al paciente;
- epicrisis;
- tratamientos;
- consultas/sesiones de tratamiento;
- fichas completadas dentro de epicrisis y sesiones;
- estado visible de un registro cuando fue rectificado o anulado.

Los registros se ordenan por fecha clínica ascendente. No se incluyen IDs técnicos, versiones internas, snapshots de rectificación, contraseñas, tokens, información del sistema ni contenido del AuditLog.

## Integración futura con el servicio de profesionales

Cuando el sistema se integre con el servicio responsable de los datos del profesional, todas las exportaciones de historia clínica (`PDF`, `DOCX`, `CSV` y `XLSX`) deberán incorporar los siguientes datos:

```yaml
Profesional:
  - Nombre y apellido
  - Matrícula profesional
  - Especialidad
```

Estos datos deberán obtenerse en el backend a partir de la identidad del profesional autenticado y consultarse en el servicio de profesionales. No deberán aceptarse como parte del request de exportación ni confiar en valores enviados por el frontend.

En `PDF` y `DOCX` deberán presentarse en la cabecera identificatoria del documento. En `CSV` y `XLSX` deberán incluirse dentro de la sección de metadata previa a la cronología clínica. La ausencia temporal de alguno de estos datos deberá tratarse de forma explícita, sin inventar valores y sin exponer identificadores técnicos.

Esta incorporación queda documentada como pendiente hasta que el servicio de profesionales esté disponible. La implementación actual identifica al profesional para autorización, auditoría y trazabilidad, pero todavía no agrega esos datos descriptivos al archivo exportado.

## Flujo del frontend

1. Abrir `Exportar historia clínica` desde el inicio.
2. Buscar al paciente por apellido o nombre. La búsqueda usa `GET /api/pacientes`, que devuelve exclusivamente pacientes del profesional autenticado.
3. Seleccionar al paciente y confirmar la operación. La pantalla siguiente presenta apellido, nombre y DNI.
4. Elegir formato y motivo, y completar opcionalmente el detalle.
5. Confirmar. El backend genera el archivo, guarda metadata y hash, registra el AuditLog y responde el binario para su descarga.

## API

### Pacientes exportables

```http
GET /api/pacientes
Authorization: Bearer <jwt>
```

El cliente no envía `profesionalId`. El backend lo obtiene del `SecurityContext`.

### Generar y descargar

```http
POST /api/pacientes/{pacienteId}/historia-clinica/exportar
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "formato": "PDF",
  "motivo": "SOLICITUD_DEL_PACIENTE",
  "detalleMotivo": "Copia solicitada por el paciente"
}
```

Formatos admitidos: `PDF`, `DOCX`, `CSV`, `XLSX`.

Motivos admitidos:

- `SOLICITUD_DEL_PACIENTE`
- `CONTINUIDAD_DE_TRATAMIENTO`
- `DERIVACION`
- `SEGUNDA_OPINION`
- `TRAMITE_ADMINISTRATIVO`
- `OTRO`

`formato` y `motivo` son obligatorios. `detalleMotivo` es opcional y admite hasta 500 caracteres.

Respuesta exitosa:

- `200 OK`;
- `Content-Type` correspondiente al formato;
- `Content-Disposition: attachment` con nombre seguro;
- cuerpo binario del archivo.

Errores relevantes:

- `400 Bad Request`: formato desconocido, motivo ausente u otra validación inválida;
- `403 Forbidden`: falta una identidad profesional válida o el paciente no pertenece a ese profesional.

Se responde `403` tanto para un paciente ajeno como para un identificador no accesible, evitando IDOR y filtrado de existencia.

## Seguridad e identidad

En producción debe utilizarse `app.seguridad.modo=jwt`. `ProveedorIdentidadProfesional` obtiene el identificador, nombre y matrícula desde claims firmados. El request de exportación nunca acepta `profesionalId`, y la consulta del paciente aplica conjuntamente `pacienteId` e `idProfesional` antes de cargar información clínica.

El modo `local` usa una identidad fija configurada en el servidor únicamente para desarrollo:

```text
LOCAL_PROFESSIONAL_ID=1
LOCAL_PROFESSIONAL_NAME=Profesional local
LOCAL_PROFESSIONAL_LICENSE=LOCAL
```

Estos valores no provienen del body, query string ni path. El modo local no debe usarse en producción.

## Arquitectura

```text
ControladorExportacionHistoriaClinica
        |
ServicioExportacionHistoriaClinica
        |-- autorización y ownership
        |-- ConstructorHistoriaClinica
        |-- Map<FormatoExportacion, HistoriaClinicaExporter>
        |      |-- PdfHistoriaClinicaExporter
        |      |-- WordHistoriaClinicaExporter
        |      |-- CsvHistoriaClinicaExporter
        |      `-- ExcelHistoriaClinicaExporter
        |-- RepositorioExportacionHistoriaClinica
        `-- ServicioAuditLog
```

La estrategia se selecciona por `FormatoExportacion` mediante un `EnumMap`; no hay una cadena extensa de condicionales. PDFBox 3.0.8 genera PDF y Apache POI 5.5.1 genera DOCX y XLSX. El CSV se genera en UTF-8 con BOM, comillas RFC 4180 y mitigación de inyección de fórmulas.

PDF y DOCX comparten una presentación clínica consistente: cabecera institucional verde, bloque identificatorio, jerarquía de secciones, cronología legible y pie confidencial paginado. El DOCX usa página A4, tipografía Arial, estilos Word reales, geometría explícita, una tarjeta identificatoria de dos columnas y tarjetas cronológicas con fondo tenue y acento lateral verde. El PDF controla saltos de página y ajuste de líneas para evitar contenido recortado.

Los archivos se mantienen en memoria solo durante la respuesta. No se almacenan en disco ni en la base de datos.

## Persistencia

Flyway aplica `V15__crear_exportaciones_historia_clinica_y_audit_log.sql` sin alterar tablas clínicas existentes.

`exportaciones_historia_clinica` conserva:

```text
id
id_paciente
id_profesional
motivo
detalle_motivo
formato
fecha_hora_exportacion
nombre_archivo
hash_archivo
```

La fecha se genera en el servidor. El hash es SHA-256 hexadecimal de 64 caracteres calculado sobre los bytes exactos entregados. La entidad es inmutable desde JPA.

`audit_log` registra metadata mínima y no contiene texto clínico:

```text
action = EXPORT_CLINICAL_HISTORY
id_profesional
id_paciente
formato
fecha_hora
resultado = SUCCESS | FAILED
```

Los intentos de acceder a pacientes ajenos y los fallos relevantes de generación se registran como `FAILED`. Las exportaciones completas se registran como `SUCCESS`.

## Pruebas

La suite `ControladorExportacionHistoriaClinicaIntegrationTest` cubre:

- exportación de un paciente propio;
- rechazo `403` para un paciente ajeno;
- formato inválido y motivo ausente;
- persistencia de `ExportacionHistoriaClinica`;
- identidad profesional tomada de la autenticación;
- hash SHA-256 del contenido entregado;
- AuditLog `SUCCESS` y `FAILED`;
- apertura y lectura de PDF real;
- estructura y contenido CSV;
- apertura de XLSX real con Apache POI;
- apertura y lectura de DOCX real con Apache POI;
- renderizado visual de una muestra PDF para verificar jerarquía, márgenes, saltos y legibilidad;
- validación estructural del DOCX mediante su apertura con Apache POI, incluyendo estilos, tablas y contenido.

Ejecutar:

```bash
cd clinical-history-service
mvn -Dtest=ControladorExportacionHistoriaClinicaIntegrationTest test
```

Para verificar todo el repositorio:

```bash
cd clinical-history-service
mvn test

cd ../frontend
npm run build
```

### QA visual del DOCX

LibreOffice se utiliza únicamente como herramienta local de desarrollo/QA para renderizar las muestras generadas por las pruebas. No es una dependencia del servicio productivo y no interviene en la generación del DOCX, que continúa a cargo de Apache POI.

En este workspace puede utilizarse una distribución portátil ignorada por Git en:

```text
.tools/LibreOfficePortable/App/libreoffice/program/soffice.com
```

La prueba `WordHistoriaClinicaExporterTest` genera `target/qa-exportacion/historia-clinica-extensa.docx`, con suficientes registros para verificar varias páginas. La muestra puede convertirse mediante:

```powershell
& '.tools\LibreOfficePortable\App\libreoffice\program\soffice.com' `
  --headless `
  --convert-to pdf `
  --outdir 'clinical-history-service\target\qa-exportacion\docx-render' `
  'clinical-history-service\target\qa-exportacion\historia-clinica-extensa.docx'
```

La revisión visual debe abarcar todas las páginas y comprobar, como mínimo: ausencia de texto recortado o superpuesto, continuidad de las tarjetas, márgenes, cabecera repetida, pie confidencial, número de página y ausencia de bordes heredados no deseados. Los PDF y PNG usados para esta revisión son artefactos temporales de QA y no forman parte de la respuesta de la API.
