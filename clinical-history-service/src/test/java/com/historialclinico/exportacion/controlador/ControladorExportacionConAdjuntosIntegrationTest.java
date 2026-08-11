package com.historialclinico.exportacion.controlador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.historialclinico.archivo.modelo.*;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinico;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinicoVersion;
import com.historialclinico.archivo.storage.ClinicalFileStorage;
import com.historialclinico.auditoria.modelo.AuditLog;
import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.repositorio.RepositorioAuditLog;
import com.historialclinico.auditoria.servicio.ServicioCifradoAuditoria;
import com.historialclinico.epicrisis.modelo.Epicrisis;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.excepcion.ExcepcionServicioArchivosNoDisponible;
import com.historialclinico.exportacion.modelo.FormatoArchivoFinal;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.modelo.TipoExportacion;
import com.historialclinico.exportacion.repositorio.RepositorioExportacionHistoriaClinica;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.modelo.Sexo;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.tratamiento.modelo.SesionTratamiento;
import com.historialclinico.tratamiento.modelo.Tratamiento;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ControladorExportacionConAdjuntosIntegrationTest.Configuracion.class)
class ControladorExportacionConAdjuntosIntegrationTest {
    private static final long PROFESIONAL = 92001L;

    @Autowired MockMvc mvc;
    @Autowired RepositorioPaciente pacientes;
    @Autowired RepositorioDocumentoClinico documentos;
    @Autowired RepositorioDocumentoClinicoVersion versiones;
    @Autowired RepositorioTratamiento tratamientos;
    @Autowired RepositorioEpicrisis epicrisis;
    @Autowired RepositorioExportacionHistoriaClinica exportaciones;
    @Autowired RepositorioAuditLog auditLog;
    @Autowired ServicioCifradoAuditoria integridad;
    @Autowired TestStorage storage;
    @Autowired ObjectMapper objectMapper;

    @Test
    void exportacionCompletaPdfIncluyeManifestContextosYSoloVersionesActivasActuales() throws Exception {
        Paciente paciente = paciente("Completa PDF", "42000001");
        Tratamiento tratamiento = new Tratamiento(paciente, "Plan integral", "Seguimiento", 2);
        tratamiento.agregarSesion(new SesionTratamiento("Primera sesión", null, null));
        tratamiento = tratamientos.saveAndFlush(tratamiento);
        Long sesionId = tratamiento.getSesiones().getFirst().getId();
        Epicrisis resumen = epicrisis.saveAndFlush(new Epicrisis(paciente, null, null, "Alta clínica"));

        documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(), "informe.pdf",
                "directo".getBytes(StandardCharsets.UTF_8), "Informe directo");
        documento(paciente, ContextoDocumentoClinico.TRATAMIENTO, tratamiento.getId(), "informe.pdf",
                "tratamiento".getBytes(StandardCharsets.UTF_8), "Informe del tratamiento");
        documento(paciente, ContextoDocumentoClinico.SESION, sesionId, "../imagen.png",
                new byte[]{1, 2, 3, 4}, "Imagen de sesión");
        documento(paciente, ContextoDocumentoClinico.EPICRISIS, resumen.getId(), "resumen.docx",
                new byte[]{5, 6, 7}, "Resumen de alta");
        DocumentoClinico eliminado = documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(),
                "borrado.pdf", new byte[]{8, 9}, "No debe incluirse");
        eliminado.eliminar("Carga errónea", Instant.now(), 10);
        documentos.saveAndFlush(eliminado);
        documentoVersionado(paciente);

        byte[] zip = exportarCompleta(paciente, "PDF");
        Map<String, byte[]> entradas = entradasZip(zip);

        assertThat(entradas).containsKeys("Historia_Clinica.pdf", "manifest.json",
                "Adjuntos/informe.pdf", "Adjuntos/informe_2.pdf", "Adjuntos/imagen.png",
                "Adjuntos/resumen.docx", "Adjuntos/versionado.pdf");
        assertThat(entradas.keySet()).noneMatch(nombre -> nombre.contains("..") || nombre.contains("borrado"));
        assertThat(new String(entradas.get("Historia_Clinica.pdf"), 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
        assertThat(entradas.get("Adjuntos/versionado.pdf")).isEqualTo("actual".getBytes(StandardCharsets.UTF_8));
        JsonNode manifest = objectMapper.readTree(entradas.get("manifest.json"));
        assertThat(manifest.path("patientId").asLong()).isEqualTo(paciente.getId());
        assertThat(manifest.path("clinicalHistoryFormat").asText()).isEqualTo("PDF");
        assertThat(manifest.path("files")).hasSize(5);
        assertThat(manifest.toString()).contains("PACIENTE", "TRATAMIENTO", "SESION", "EPICRISIS")
                .doesNotContain("storageKey", "patients/");

        var registro = exportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(PROFESIONAL, paciente.getId())
                .getFirst();
        assertThat(registro.getTipoExportacion()).isEqualTo(TipoExportacion.HISTORIA_CLINICA_CON_ADJUNTOS);
        assertThat(registro.getFormatoHistoriaClinica()).isEqualTo(FormatoExportacion.PDF);
        assertThat(registro.getFormatoArchivoFinal()).isEqualTo(FormatoArchivoFinal.ZIP);
        assertThat(registro.getNombreArchivo()).endsWith(".zip");
        assertThat(registro.getHashArchivo()).isEqualTo(integridad.hash(zip));
        assertThat(auditLog.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.EXPORT_CLINICAL_HISTORY, PROFESIONAL, paciente.getId()))
                .extracting(evento -> evento.getResultado()).containsExactly(ResultadoAuditLog.SUCCESS);
    }

    @Test
    void exportacionCompletaDocxConservaDocumentoPrincipalYAdjuntoOriginal() throws Exception {
        Paciente paciente = paciente("Completa DOCX", "42000002");
        documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(), "radiografia.jpg",
                new byte[]{(byte) 0xff, (byte) 0xd8, 1, 2}, "Radiografía de control");

        byte[] zip = exportarCompleta(paciente, "DOCX");
        Map<String, byte[]> entradas = entradasZip(zip);

        assertThat(entradas).containsKeys("Historia_Clinica.docx", "manifest.json", "Adjuntos/radiografia.jpg");
        assertThat(new String(entradas.get("Historia_Clinica.docx"), 0, 2, StandardCharsets.US_ASCII))
                .isEqualTo("PK");
        assertThat(entradas.get("Adjuntos/radiografia.jpg"))
                .isEqualTo(new byte[]{(byte) 0xff, (byte) 0xd8, 1, 2});
    }

    @Test
    void exportacionSimpleReferenciaNombresYDescripcionesSinIncluirBinarios() throws Exception {
        Paciente paciente = paciente("Referencias", "42000003");
        documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(), "laboratorio.pdf",
                "BINARIO-SECRETO".getBytes(StandardCharsets.UTF_8), "Hemograma anual");

        byte[] csv = mvc.perform(post("/api/pacientes/{id}/historia-clinica/exportar", paciente.getId())
                        .with(profesional()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formato":"CSV","tipoExportacion":"HISTORIA_CLINICA",
                                 "motivo":"CONTINUIDAD_DE_TRATAMIENTO"}
                                """))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        String texto = new String(csv, StandardCharsets.UTF_8);
        assertThat(texto).contains("laboratorio.pdf", "Hemograma anual").doesNotContain("BINARIO-SECRETO");
    }

    @Test
    void rechazaCsvYXlsxComoDocumentoPrincipalDeExportacionCompleta() throws Exception {
        Paciente paciente = paciente("Formato inválido", "42000004");
        for (String formato : new String[]{"CSV", "XLSX"}) {
            mvc.perform(post("/api/pacientes/{id}/historia-clinica/exportar", paciente.getId())
                            .with(profesional()).contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"formato":"%s","tipoExportacion":"HISTORIA_CLINICA_CON_ADJUNTOS",
                                     "motivo":"SOLICITUD_DEL_PACIENTE"}
                                    """.formatted(formato)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void falloDeIntegridadAbortaZipYRegistraUnFalloGlobal() throws Exception {
        Paciente paciente = paciente("Integridad", "42000005");
        DocumentoClinico documento = documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(),
                "integridad.pdf", "original".getBytes(StandardCharsets.UTF_8), null);
        storage.corromper(versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documento.getId())
                .getFirst().getStorageKey());

        mvc.perform(post("/api/pacientes/{id}/historia-clinica/exportar", paciente.getId())
                        .with(profesional()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formato":"PDF","tipoExportacion":"HISTORIA_CLINICA_CON_ADJUNTOS",
                                 "motivo":"SOLICITUD_DEL_PACIENTE"}
                                """))
                .andExpect(status().isConflict());

        assertThat(exportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(PROFESIONAL, paciente.getId()))
                .isEmpty();
        assertThat(auditLog.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.EXPORT_CLINICAL_HISTORY, PROFESIONAL, paciente.getId()))
                .extracting(evento -> evento.getResultado()).containsExactly(ResultadoAuditLog.FAILED);
    }

    @Test
    void falloDeStorageNoGeneraPaqueteParcial() throws Exception {
        Paciente paciente = paciente("Storage", "42000006");
        documento(paciente, ContextoDocumentoClinico.PACIENTE, paciente.getId(), "storage.pdf",
                "contenido".getBytes(StandardCharsets.UTF_8), null);
        storage.fallarLectura();

        mvc.perform(post("/api/pacientes/{id}/historia-clinica/exportar", paciente.getId())
                        .with(profesional()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formato":"PDF","tipoExportacion":"HISTORIA_CLINICA_CON_ADJUNTOS",
                                 "motivo":"SOLICITUD_DEL_PACIENTE"}
                                """))
                .andExpect(status().isServiceUnavailable());

        assertThat(exportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(PROFESIONAL, paciente.getId()))
                .isEmpty();
    }

    private byte[] exportarCompleta(Paciente paciente, String formato) throws Exception {
        return mvc.perform(post("/api/pacientes/{id}/historia-clinica/exportar", paciente.getId())
                        .with(profesional()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formato":"%s","tipoExportacion":"HISTORIA_CLINICA_CON_ADJUNTOS",
                                 "motivo":"SOLICITUD_DEL_PACIENTE","detalleMotivo":"Copia completa"}
                                """.formatted(formato)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".zip")))
                .andReturn().getResponse().getContentAsByteArray();
    }

    private DocumentoClinico documento(Paciente paciente, ContextoDocumentoClinico contexto, Long contextoId,
            String nombre, byte[] contenido, String descripcion) {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        DocumentoClinico documento = new DocumentoClinico(UUID.randomUUID(), paciente, contexto, contextoId,
                CategoriaDocumentoClinico.INFORME, descripcion, ahora);
        documento = documentos.saveAndFlush(documento);
        UUID versionId = UUID.randomUUID();
        String key = "test/" + versionId;
        DocumentoClinicoVersion version = new DocumentoClinicoVersion(versionId, documento, 1, nombre,
                extension(nombre), mime(nombre), contenido.length, key, integridad.hash(contenido), null, ahora);
        versiones.saveAndFlush(version);
        documento.asignarVersionActual(version, ahora);
        documento = documentos.saveAndFlush(documento);
        storage.almacenar(key, contenido, mime(nombre));
        return documento;
    }

    private void documentoVersionado(Paciente paciente) {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        DocumentoClinico documento = new DocumentoClinico(UUID.randomUUID(), paciente,
                ContextoDocumentoClinico.PACIENTE, paciente.getId(), CategoriaDocumentoClinico.INFORME,
                "Documento corregido", ahora);
        documento = documentos.saveAndFlush(documento);
        byte[] historico = "historico".getBytes(StandardCharsets.UTF_8);
        DocumentoClinicoVersion version1 = new DocumentoClinicoVersion(UUID.randomUUID(), documento, 1,
                "versionado.pdf", "pdf", "application/pdf", historico.length, "test/" + UUID.randomUUID(),
                integridad.hash(historico), null, ahora);
        version1.marcarHistorica();
        versiones.saveAndFlush(version1);
        storage.almacenar(version1.getStorageKey(), historico, version1.getMimeType());
        byte[] actual = "actual".getBytes(StandardCharsets.UTF_8);
        DocumentoClinicoVersion version2 = new DocumentoClinicoVersion(UUID.randomUUID(), documento, 2,
                "versionado.pdf", "pdf", "application/pdf", actual.length, "test/" + UUID.randomUUID(),
                integridad.hash(actual), "Corrección", ahora.plus(1, ChronoUnit.MICROS));
        versiones.saveAndFlush(version2);
        storage.almacenar(version2.getStorageKey(), actual, version2.getMimeType());
        documento.asignarVersionActual(version2, ahora.plus(1, ChronoUnit.MICROS));
        documentos.saveAndFlush(documento);
    }

    private Map<String, byte[]> entradasZip(byte[] contenido) throws Exception {
        Map<String, byte[]> entradas = new LinkedHashMap<>();
        try (var zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            java.util.zip.ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) entradas.put(entrada.getName(), zip.readAllBytes());
        }
        return entradas;
    }

    private Paciente paciente(String apellido, String dni) {
        return pacientes.saveAndFlush(new Paciente(PROFESIONAL, "Paciente", apellido, dni, "3515550000",
                LocalDate.of(1990, 1, 15), Sexo.FEMENINO));
    }

    private String extension(String nombre) { return nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(); }
    private String mime(String nombre) {
        String extension = extension(nombre);
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/pdf";
        };
    }

    private RequestPostProcessor profesional() {
        return jwt().jwt(token -> token.subject("profesional-" + PROFESIONAL)
                .claim("professional_id", PROFESIONAL)
                .claim("professional_name", "Profesional de prueba")
                .claim("professional_license", "MP 92001"));
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary TestStorage testStorage() { return new TestStorage(); }
    }

    static class TestStorage implements ClinicalFileStorage {
        private final Map<String, byte[]> objetos = new HashMap<>();
        private boolean fallaLectura;
        @Override public void almacenar(String storageKey, byte[] contenido, String mimeType) {
            objetos.put(storageKey, contenido.clone());
        }
        @Override public byte[] leer(String storageKey) {
            if (fallaLectura) {
                fallaLectura = false;
                throw new ExcepcionServicioArchivosNoDisponible("Fallo de storage simulado", null);
            }
            byte[] contenido = objetos.get(storageKey);
            if (contenido == null) throw new ExcepcionServicioArchivosNoDisponible("Objeto inexistente", null);
            return contenido.clone();
        }
        @Override public boolean existe(String storageKey) { return objetos.containsKey(storageKey); }
        @Override public void eliminar(String storageKey) { objetos.remove(storageKey); }
        void corromper(String storageKey) { objetos.put(storageKey, "corrupto".getBytes(StandardCharsets.UTF_8)); }
        void fallarLectura() { fallaLectura = true; }
    }
}
