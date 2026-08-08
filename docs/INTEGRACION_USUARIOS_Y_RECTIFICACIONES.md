# Integración de identidad y rectificaciones clínicas

## Modelo de responsabilidad

El sistema no comparte pacientes ni atenciones entre profesionales. Aunque una instalación aloje varias cuentas,
cada paciente, epicrisis, tratamiento y sesión pertenece a un único profesional.

La seguridad se divide deliberadamente entre servicios:

| Componente | Responsabilidad |
|---|---|
| Microservicio de usuarios / proveedor de identidad | Autenticar al profesional, administrar credenciales, MFA, altas, bajas, matrícula y emitir el JWT. |
| API Gateway | Rechazar tokens ausentes, propagar `Authorization`, eliminar cabeceras de identidad aportadas por clientes y asignar `X-Request-Id`. |
| Servicio de historias clínicas | Validar criptográficamente el JWT y comprobar que la identidad autenticada coincide con el propietario de cada recurso. |

La autenticación centralizada **no reemplaza** la autorización local. El servicio de usuarios no conoce la columna
`id_profesional` de cada paciente. Por eso este servicio aplica siempre la propiedad en sus consultas y responde
`404 Not Found` cuando el paciente o registro no corresponde al profesional, sin revelar si el identificador existe.

## Contrato JWT requerido

En producción debe configurarse `SECURITY_MODE=jwt`. El token debe contener:

```json
{
  "iss": "https://identidad.ejemplo.ar",
  "aud": ["clinical-history-service"],
  "sub": "identificador-del-usuario",
  "professional_id": 123,
  "professional_name": "Nombre Apellido",
  "professional_license": "MP 12345",
  "iat": 1786136400,
  "exp": 1786140000
}
```

Requisitos:

- `professional_id` es obligatorio, numérico, estable y no reutilizable. Si se cambia el nombre del claim, usar
  `PROFESSIONAL_ID_CLAIM`.
- `professional_name` y `professional_license` son obligatorios y se copian en cada auditoría como evidencia histórica. Sus nombres
  son configurables con `PROFESSIONAL_NAME_CLAIM` y `PROFESSIONAL_LICENSE_CLAIM`.
- `iss`, firma, vencimiento y `aud` son validados por este servicio. Configurar `JWT_ISSUER_URI` y `JWT_AUDIENCE`.
- El emisor debe publicar sus claves mediante OIDC/JWKS y contemplar rotación de claves y revocación de cuentas.
- El JWT se envía como `Authorization: Bearer <token>`.

Las rutas conservan temporalmente `/{idProfesional}` por compatibilidad. En modo JWT el interceptor compara ese valor
con `professional_id`. Una diferencia produce `404`, incluso si el recurso solicitado pertenece a otro profesional.
En una versión futura puede retirarse el identificador de la URL sin cambiar la regla de propiedad.

### Modo local

`SECURITY_MODE=local` permite el funcionamiento actual del frontend sin proveedor de identidad. En este modo el
identificador de la ruta no está autenticado y **no es apto para producción ni para redes no confiables**. Las
cabeceras `X-Professional-Name` y `X-Professional-License` solo sirven para desarrollo. En modo JWT se ignoran y se
utilizan exclusivamente los claims firmados.

## Secuencia de integración

1. Crear el cliente/recurso `clinical-history-service` en el proveedor de identidad.
2. Emitir los claims anteriores y configurar su audiencia.
3. Definir `SECURITY_MODE=jwt`, `JWT_ISSUER_URI` y `JWT_AUDIENCE`.
4. Impedir el acceso público directo al puerto del servicio; todo tráfico externo debe ingresar por el gateway.
5. Hacer que el gateway elimine cualquier `X-Professional-*`, `X-Forwarded-For`, `X-Device-Id` y `X-Request-Id`
   recibido del exterior antes de agregar valores confiables. `X-Device-Id` puede conservarse solo si fue emitido y
   validado por la aplicación.
6. Activar `TRUST_PROXY_HEADERS=true` únicamente cuando el servicio esté detrás de un proxy controlado.
7. Probar expresamente token vencido, firma inválida, audiencia incorrecta, falta de matrícula y acceso cruzado.

## Rectificaciones clínicas

No existen `PUT`, `PATCH` ni `DELETE` para epicrisis, tratamientos o sesiones. Una corrección se realiza mediante:

```text
POST .../epicrisis/{idEpicrisis}/rectificaciones
POST .../tratamientos/{idTratamiento}/rectificaciones
POST .../tratamientos/{idTratamiento}/sesiones/{idSesion}/rectificaciones
```

Ejemplo:

```json
{
  "rectificacion": {
    "versionEsperada": 1,
    "tipoMotivo": "ERROR_TRANSCRIPCION",
    "motivo": "Se corrige un error de transcripción comprobado"
  },
  "observaciones": "Contenido clínico rectificado",
  "idFichaSeguimiento": null,
  "respuestasFichaSeguimiento": null
}
```

Motivos admitidos: `ERROR_TRANSCRIPCION`, `DATO_CLINICO_INCORRECTO`, `ACLARACION`, `INFORMACION_OMITIDA` y
`ANULACION_CARGA_ERRONEA`. Una anulación no borra el registro: crea otra versión con estado `ANULADO`.

`versionEsperada` implementa control de concurrencia. Si otro navegador rectificó el registro, la API devuelve
`409 Conflict` y obliga a recargarlo. Cada operación exitosa registra fecha/hora UTC del servidor, profesional,
matrícula, paciente, IP, dispositivo/agente de usuario, sesión, solicitud, antes, después, motivo y hashes.

## Auditoría, cifrado y conservación

Los contenidos `antes` y `después` se cifran con AES-256-GCM. Cada evento incorpora hashes SHA-256 y una cadena HMAC
que permite detectar cambios, supresiones o reordenamientos. La entidad de auditoría rechaza actualizaciones y
borrados desde JPA y la API no expone operaciones destructivas. La clave HMAC se deriva de la clave externa de
auditoría, por lo que una alteración directa de base de datos no puede regenerar una cadena válida sin esa clave.

Debe configurarse `AUDIT_ENCRYPTION_KEY` con 32 bytes en Base64, por ejemplo `openssl rand -base64 32`. No guardar la
clave en Git ni en la base de datos: usar el gestor de secretos de la plataforma, mantener copias recuperables y
definir un procedimiento de rotación antes de cambiarla. Perderla impide recuperar la evidencia cifrada.

`AUDIT_RETENTION_YEARS` no admite valores menores a 10. No se ejecutan purgas automáticas. Además, el borrado físico
de pacientes está deshabilitado y las claves foráneas ya no eliminan en cascada epicrisis, tratamientos o sesiones.

Endpoints de consulta y exportación:

```text
GET .../{registro}/auditoria
GET .../{registro}/informe-auditoria
```

El informe descargable contiene versiones, motivos, identidad, IP, equipo, antes/después, hashes y el resultado de
la verificación de la cadena. Es evidencia técnica; no equivale por sí solo a una firma digital bajo la Ley 25.506.
Si se requiere esa presunción jurídica, deberá integrarse posteriormente un certificador licenciado.

## Operación y respaldo

- Cifrar también volúmenes, copias de seguridad y conexiones PostgreSQL.
- Restringir el acceso a la tabla de auditoría al usuario de esta aplicación y a un rol de auditoría de solo lectura.
- Probar restauraciones y verificar que el secreto de auditoría esté disponible en el entorno recuperado.
- No enviar el cuerpo clínico, `antes` o `después` a logs generales, APM o servicios de analítica.
- Sincronizar los servidores con una fuente horaria confiable y conservar todas las fechas en UTC.
