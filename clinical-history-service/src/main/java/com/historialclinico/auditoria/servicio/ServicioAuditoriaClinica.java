package com.historialclinico.auditoria.servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.historialclinico.auditoria.dto.InformeAuditoriaClinica;
import com.historialclinico.auditoria.dto.RespuestaAuditoriaRectificacion;
import com.historialclinico.auditoria.dto.SolicitudMotivoRectificacion;
import com.historialclinico.auditoria.modelo.AuditoriaRectificacionClinica;
import com.historialclinico.auditoria.modelo.TipoRegistroClinico;
import com.historialclinico.auditoria.repositorio.RepositorioAuditoriaRectificacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ServicioAuditoriaClinica {
    private final RepositorioAuditoriaRectificacion repositorio;
    private final ServicioCifradoAuditoria cifrado;
    private final ProveedorContextoAuditoria proveedorContexto;
    private final ObjectMapper objectMapper;
    private final int retencionAnios;

    public ServicioAuditoriaClinica(RepositorioAuditoriaRectificacion repositorio, ServicioCifradoAuditoria cifrado,
            ProveedorContextoAuditoria proveedorContexto, ObjectMapper objectMapper,
            @Value("${app.auditoria.retencion-anios:10}") int retencionAnios) {
        if (retencionAnios < 10) throw new IllegalStateException("La retención de auditoría no puede ser menor a diez años");
        this.repositorio = repositorio; this.cifrado = cifrado; this.proveedorContexto = proveedorContexto;
        this.objectMapper = objectMapper; this.retencionAnios = retencionAnios;
    }

    public void registrar(TipoRegistroClinico tipo, Long idRegistro, Long idPaciente, Long idProfesional,
            int versionAnterior, int versionNueva, SolicitudMotivoRectificacion motivo, Object antes, Object despues) {
        try {
            String jsonAntes = objectMapper.writeValueAsString(antes);
            String jsonDespues = objectMapper.writeValueAsString(despues);
            var antesCifrado = cifrado.cifrar(jsonAntes); var despuesCifrado = cifrado.cifrar(jsonDespues);
            String hashAntes = cifrado.hash(jsonAntes); String hashDespues = cifrado.hash(jsonDespues);
            String anterior = repositorio.findFirstByTipoRegistroAndIdRegistroOrderByIdDesc(tipo, idRegistro)
                    .map(AuditoriaRectificacionClinica::getHashCadena).orElse(null);
            // PostgreSQL persiste TIMESTAMP WITH TIME ZONE con precisión de microsegundos. La fecha debe
            // normalizarse antes de firmar para reconstruir exactamente el mismo material al verificarla.
            Instant fecha = Instant.now().truncatedTo(ChronoUnit.MICROS);
            var contexto = proveedorContexto.obtener(idProfesional);
            String material = materialCadena(anterior, tipo, idRegistro, idPaciente, idProfesional, versionAnterior,
                    versionNueva, motivo.tipoMotivo().name(), motivo.motivo().trim(), fecha, hashAntes, hashDespues,
                    contexto.idSolicitud());
            var evento = new AuditoriaRectificacionClinica(tipo, idRegistro, idPaciente, idProfesional,
                    contexto.nombreProfesional(), contexto.matriculaProfesional(), versionAnterior, versionNueva,
                    motivo.tipoMotivo(), motivo.motivo().trim(), fecha,
                    fecha.atZone(ZoneOffset.UTC).plusYears(retencionAnios).toInstant(),
                    contexto.ipOrigen() == null ? "desconocida" : contexto.ipOrigen(), contexto.equipo(),
                    contexto.idSesion(), contexto.idSolicitud(), antesCifrado.contenido(), antesCifrado.iv(),
                    despuesCifrado.contenido(), despuesCifrado.iv(), hashAntes, hashDespues, anterior,
                    cifrado.firmaCadena(material));
            repositorio.save(evento);
        } catch (IllegalStateException ex) { throw ex; }
        catch (Exception ex) { throw new IllegalStateException("No fue posible registrar la auditoría clínica", ex); }
    }

    public List<RespuestaAuditoriaRectificacion> listar(Long idProfesional, Long idPaciente,
            TipoRegistroClinico tipo, Long idRegistro) {
        List<AuditoriaRectificacionClinica> eventos = repositorio
                .findAllByIdProfesionalAndIdPacienteAndTipoRegistroAndIdRegistroOrderByIdAsc(
                        idProfesional, idPaciente, tipo, idRegistro);
        String anterior = null;
        java.util.ArrayList<RespuestaAuditoriaRectificacion> respuestas = new java.util.ArrayList<>();
        for (var evento : eventos) {
            boolean valida = validar(evento, anterior);
            respuestas.add(convertir(evento, valida));
            anterior = evento.getHashCadena();
        }
        return respuestas;
    }

    public InformeAuditoriaClinica informe(Long idProfesional, Long idPaciente,
            TipoRegistroClinico tipo, Long idRegistro) {
        var eventos = listar(idProfesional, idPaciente, tipo, idRegistro);
        boolean valida = eventos.stream().allMatch(RespuestaAuditoriaRectificacion::integridadValida);
        return new InformeAuditoriaClinica("AUDITORIA_CLINICA_V1", Instant.now(), idProfesional, idPaciente,
                tipo, idRegistro, eventos.size(), valida,
                "La verificación jurídica definitiva depende de la infraestructura de firma digital que se integre.", eventos);
    }

    private RespuestaAuditoriaRectificacion convertir(AuditoriaRectificacionClinica evento, boolean valida) {
        try {
            String antes = cifrado.descifrar(evento.getAntesCifrado(), evento.getAntesIv());
            String despues = cifrado.descifrar(evento.getDespuesCifrado(), evento.getDespuesIv());
            JsonNode nodoAntes = objectMapper.readTree(antes); JsonNode nodoDespues = objectMapper.readTree(despues);
            boolean hashesValidos = cifrado.hash(antes).equals(evento.getHashAntes())
                    && cifrado.hash(despues).equals(evento.getHashDespues());
            return new RespuestaAuditoriaRectificacion(evento.getId(), evento.getTipoRegistro(), evento.getIdRegistro(),
                    evento.getIdPaciente(), evento.getIdProfesional(), evento.getNombreProfesional(),
                    evento.getMatriculaProfesional(), evento.getVersionAnterior(), evento.getVersionNueva(),
                    evento.getTipoMotivo(), evento.getMotivo(), evento.getResultado(), evento.getFechaHora(),
                    evento.getConservarHasta(), evento.getIpOrigen(), evento.getEquipo(), evento.getIdSesion(),
                    evento.getIdSolicitud(), nodoAntes, nodoDespues, evento.getHashAntes(), evento.getHashDespues(),
                    evento.getHashAnteriorCadena(), evento.getHashCadena(), valida && hashesValidos);
        } catch (Exception ex) { throw new IllegalStateException("No fue posible leer la auditoría clínica", ex); }
    }

    private boolean validar(AuditoriaRectificacionClinica e, String anteriorEsperado) {
        if (!java.util.Objects.equals(anteriorEsperado, e.getHashAnteriorCadena())) return false;
        String material = materialCadena(e.getHashAnteriorCadena(), e.getTipoRegistro(), e.getIdRegistro(),
                e.getIdPaciente(), e.getIdProfesional(), e.getVersionAnterior(), e.getVersionNueva(),
                e.getTipoMotivo().name(), e.getMotivo(), e.getFechaHora(), e.getHashAntes(), e.getHashDespues(),
                e.getIdSolicitud());
        return cifrado.firmaCadena(material).equals(e.getHashCadena());
    }

    private String materialCadena(String anterior, TipoRegistroClinico tipo, Long idRegistro, Long idPaciente,
            Long idProfesional, int versionAnterior, int versionNueva, String tipoMotivo, String motivo,
            Instant fecha, String hashAntes, String hashDespues, String idSolicitud) {
        return String.join("|", anterior == null ? "" : anterior, tipo.name(), idRegistro.toString(),
                idPaciente.toString(), idProfesional.toString(), String.valueOf(versionAnterior),
                String.valueOf(versionNueva), tipoMotivo, motivo, fecha.toString(), hashAntes, hashDespues, idSolicitud);
    }
}

