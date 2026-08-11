package com.historialclinico.archivo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.historialclinico.archivo.malware.FileMalwareScanner;
import com.historialclinico.archivo.modelo.DocumentoClinico;
import com.historialclinico.archivo.modelo.DocumentoClinicoVersion;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinico;
import com.historialclinico.archivo.repositorio.RepositorioDocumentoClinicoVersion;
import com.historialclinico.archivo.storage.ClinicalFileStorage;
import com.historialclinico.auditoria.modelo.AuditLog;
import com.historialclinico.auditoria.repositorio.RepositorioAuditLog;
import com.historialclinico.auditoria.servicio.ServicioCifradoAuditoria;
import com.historialclinico.epicrisis.modelo.Epicrisis;
import com.historialclinico.epicrisis.repositorio.RepositorioEpicrisis;
import com.historialclinico.excepcion.ExcepcionMalwareDetectado;
import com.historialclinico.excepcion.ExcepcionServicioArchivosNoDisponible;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.modelo.Sexo;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import com.historialclinico.tratamiento.modelo.SesionTratamiento;
import com.historialclinico.tratamiento.modelo.Tratamiento;
import com.historialclinico.tratamiento.repositorio.RepositorioTratamiento;
import jakarta.servlet.ServletException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.archivos.cuota-recomendada-bytes=100")
@AutoConfigureMockMvc
@Import(ControladorDocumentoClinicoIntegrationTest.Configuracion.class)
class ControladorDocumentoClinicoIntegrationTest {
    private static final long PROFESIONAL = 92001L;
    private static final AtomicLong DNI = new AtomicLong(45000000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RepositorioPaciente pacientes;
    @Autowired RepositorioTratamiento tratamientos;
    @Autowired RepositorioEpicrisis epicrisis;
    @Autowired RepositorioDocumentoClinicoVersion versiones;
    @Autowired RepositorioAuditLog auditoria;
    @Autowired ServicioCifradoAuditoria integridad;
    @Autowired TestStorage storage;
    @Autowired TestScanner scanner;
    @SpyBean RepositorioDocumentoClinico documentos;

    @BeforeEach
    void preparar() {
        storage.limpiar();
        scanner.limpiar();
        reset(documentos);
    }

    @Test
    void aceptaPdfDocxJpegYPngValidos() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        assertThat(subirPaciente(paciente, "informe.pdf", pdf(30)).path("mimeType").asText())
                .isEqualTo("application/pdf");
        assertThat(subirPaciente(paciente, "informe.docx", docx()).path("mimeType").asText())
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertThat(subirPaciente(paciente, "foto.jpg", jpg(1)).path("mimeType").asText()).isEqualTo("image/jpeg");
        assertThat(subirPaciente(paciente, "foto.jpeg", jpg(2)).path("mimeType").asText()).isEqualTo("image/jpeg");
        assertThat(subirPaciente(paciente, "imagen.png", png()).path("mimeType").asText()).isEqualTo("image/png");
    }

    @Test
    void rechazaExtensionTamanioMimeFalsoYPathTraversal() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        subirEsperando(paciente, new MockMultipartFile("archivo", "malware.exe",
                "application/octet-stream", pdf(20)), 400);
        byte[] enorme = new byte[15 * 1024 * 1024 + 1];
        enorme[0] = (byte) 0xff; enorme[1] = (byte) 0xd8; enorme[2] = (byte) 0xff;
        enorme[enorme.length - 2] = (byte) 0xff; enorme[enorme.length - 1] = (byte) 0xd9;
        subirEsperando(paciente, new MockMultipartFile("archivo", "grande.jpg", "image/jpeg", enorme), 400);
        subirEsperando(paciente, new MockMultipartFile("archivo", "falso.pdf", "application/pdf", png()), 400);
        subirEsperando(paciente, new MockMultipartFile("archivo", "../escape.pdf", "application/pdf", pdf(20)), 400);
        assertThat(storage.cantidad()).isZero();
    }

    @Test
    void malwareSeRechazaSinMetadataNiStorageYSeAuditaElFallo() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        long antes = documentos.count();
        subirEsperando(paciente, new MockMultipartFile("archivo", "infectado.pdf", "application/pdf",
                "%PDF-1.4 EICAR".getBytes(StandardCharsets.US_ASCII)), 422);
        assertThat(documentos.count()).isEqualTo(antes);
        assertThat(storage.cantidad()).isZero();
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_UPLOAD, PROFESIONAL, paciente.getId()))
                .anyMatch(a -> a.getResultado().name().equals("FAILED"));
    }

    @Test
    void resuelvePacienteParaLosCuatroContextosYNoAceptaRecursosAjenos() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        Tratamiento tratamiento = new Tratamiento(paciente, "Tratamiento", null, 2);
        tratamiento.agregarSesion(new SesionTratamiento("Primera sesión", null, null));
        tratamiento = tratamientos.saveAndFlush(tratamiento);
        Long sesionId = tratamiento.getSesiones().getFirst().getId();
        Epicrisis cierre = epicrisis.saveAndFlush(new Epicrisis(paciente, null, null, "Alta"));

        assertThat(subirPaciente(paciente, "p.pdf", pdf(20)).path("contexto").asText()).isEqualTo("PACIENTE");
        assertThat(subir("/api/tratamientos/" + tratamiento.getId() + "/archivos", "t.pdf", pdf(21), PROFESIONAL)
                .path("pacienteId").asLong()).isEqualTo(paciente.getId());
        assertThat(subir("/api/sesiones/" + sesionId + "/archivos", "s.pdf", pdf(22), PROFESIONAL)
                .path("pacienteId").asLong()).isEqualTo(paciente.getId());
        assertThat(subir("/api/epicrisis/" + cierre.getId() + "/archivos", "e.pdf", pdf(23), PROFESIONAL)
                .path("pacienteId").asLong()).isEqualTo(paciente.getId());

        mvc.perform(multipart("/api/tratamientos/{id}/archivos", tratamiento.getId())
                        .file(new MockMultipartFile("archivo", "ajeno.pdf", "application/pdf", pdf(20)))
                        .param("categoria", "INFORME").with(profesional(PROFESIONAL + 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creaNuevaVersionConMotivoYDejaUnaSolaCurrent() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode creada = subirPaciente(paciente, "v1.pdf", pdf(25));
        UUID documentoId = UUID.fromString(creada.path("id").asText());
        JsonNode actualizada = nuevaVersion(documentoId, "v2.pdf", pdf(26), "Informe corregido");

        assertThat(actualizada.path("version").asInt()).isEqualTo(2);
        assertThat(actualizada.path("versiones").toString()).contains("HISTORICAL", "CURRENT", "Informe corregido");
        assertThat(versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documentoId))
                .filteredOn(v -> v.getEstadoVersion().name().equals("CURRENT")).hasSize(1);
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_UPDATE, PROFESIONAL, paciente.getId())).hasSize(1);

        mvc.perform(multipart("/api/archivos/{id}/versions", documentoId)
                        .file(new MockMultipartFile("archivo", "v3.pdf", "application/pdf", pdf(27)))
                        .param("motivo", " ").with(profesional(PROFESIONAL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restauraVersionHistoricaVerificandoIntegridad() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode v1 = subirPaciente(paciente, "v1.pdf", pdf(25));
        UUID documentoId = UUID.fromString(v1.path("id").asText());
        UUID version1 = UUID.fromString(v1.path("versiones").get(0).path("id").asText());
        nuevaVersion(documentoId, "v2.pdf", pdf(26), "Corrección");

        mvc.perform(post("/api/archivos/{id}/versions/{version}/restore", documentoId, version1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Volver al original\"}")
                        .with(profesional(PROFESIONAL)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        assertThat(versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documentoId))
                .filteredOn(v -> v.getEstadoVersion().name().equals("CURRENT"))
                .singleElement().extracting(DocumentoClinicoVersion::getId).isEqualTo(version1);
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_VERSION_RESTORE, PROFESIONAL, paciente.getId()))
                .singleElement().satisfies(a -> {
                    assertThat(a.getVersionAnterior()).isEqualTo(2);
                    assertThat(a.getVersionRestaurada()).isEqualTo(1);
                });

        nuevaVersion(documentoId, "v3.pdf", pdf(27), "Nueva corrección");
        storage.corromper(storage.keyDeVersion(version1));
        mvc.perform(post("/api/archivos/{id}/versions/{version}/restore", documentoId, version1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Intento inválido\"}")
                        .with(profesional(PROFESIONAL)))
                .andExpect(status().isConflict());
    }

    @Test
    void borradoEsLogicoExigeMotivoYPermiteRestaurarDocumentoIntegro() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode creada = subirPaciente(paciente, "archivo.pdf", pdf(30));
        UUID id = UUID.fromString(creada.path("id").asText());
        int objetos = storage.cantidad();

        mvc.perform(delete("/api/archivos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\" \"}").with(profesional(PROFESIONAL)))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/archivos/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Carga errónea\"}").with(profesional(PROFESIONAL)))
                .andExpect(status().isNoContent());
        assertThat(storage.cantidad()).isEqualTo(objetos);
        mvc.perform(get("/api/archivos/{id}", id).with(profesional(PROFESIONAL)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DELETED"));
        mvc.perform(post("/api/archivos/{id}/restore", id).with(profesional(PROFESIONAL)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ACTIVE"));
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_DELETE, PROFESIONAL, paciente.getId())).hasSize(1);
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_RESTORE, PROFESIONAL, paciente.getId())).hasSize(1);
    }

    @Test
    void duplicadoSeRechazaSinCrearOtraMetadataNiObjeto() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        byte[] contenido = pdf(60);
        long documentosAntes = documentos.count();
        subirPaciente(paciente, "uno.pdf", contenido);
        int objetosDespuesDePrimera = storage.cantidad();

        mvc.perform(multipart("/api/pacientes/{id}/archivos", paciente.getId())
                        .file(new MockMultipartFile("archivo", "otro-nombre.pdf", "application/pdf", contenido))
                        .param("categoria", "INFORME").with(profesional(PROFESIONAL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este archivo ya fue cargado para el paciente."));

        assertThat(documentos.count()).isEqualTo(documentosAntes + 1);
        assertThat(storage.cantidad()).isEqualTo(objetosDespuesDePrimera);
    }

    @Test
    void nuevaVersionConContenidoYaCargadoSeRechaza() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        byte[] contenido = pdf(62);
        JsonNode creado = subirPaciente(paciente, "original.pdf", contenido);
        UUID documentoId = UUID.fromString(creado.path("id").asText());

        mvc.perform(multipart("/api/archivos/{id}/versions", documentoId)
                        .file(new MockMultipartFile("archivo", "copia.pdf", "application/pdf", contenido))
                        .param("motivo", "Intento de reemplazo").with(profesional(PROFESIONAL)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Este archivo ya fue cargado para el paciente."));

        assertThat(versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documentoId)).hasSize(1);
    }

    @Test
    void cuotaRecomendadaGeneraAdvertenciaSinBloquear() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode primera = subirPaciente(paciente, "uno.pdf", pdf(60));
        JsonNode segunda = subirPaciente(paciente, "dos.pdf", pdf(61));
        assertThat(primera.path("warningStorage").isNull()).isTrue();
        assertThat(segunda.path("warningStorage").asText()).contains("500 MB");
    }

    @Test
    void compensaStorageSiFallaElInsert() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        doThrow(new DataIntegrityViolationException("fallo simulado"))
                .when(documentos).saveAndFlush(any(DocumentoClinico.class));
        assertThatThrownBy(() -> mvc.perform(multipart("/api/pacientes/{id}/archivos", paciente.getId())
                        .file(new MockMultipartFile("archivo", "fallo.pdf", "application/pdf", pdf(30)))
                        .param("categoria", "INFORME").with(profesional(PROFESIONAL))))
                .isInstanceOf(ServletException.class);
        assertThat(storage.cantidad()).isZero();
    }

    @Test
    void falloDeStorageNoPersisteMetadata() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        long antes = documentos.count();
        storage.fallarProximoAlmacenamiento();
        subirEsperando(paciente, new MockMultipartFile("archivo", "storage.pdf", "application/pdf", pdf(30)), 503);
        assertThat(documentos.count()).isEqualTo(antes);
        assertThat(storage.cantidad()).isZero();
    }

    @Test
    void todaOperacionExigeIdentidadConAccesoAlPaciente() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        mvc.perform(multipart("/api/pacientes/{id}/archivos", paciente.getId())
                        .file(new MockMultipartFile("archivo", "sin-identidad.pdf", "application/pdf", pdf(30)))
                        .param("categoria", "INFORME"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/pacientes/{id}/archivos", Long.MAX_VALUE)
                        .file(new MockMultipartFile("archivo", "huerfano.pdf", "application/pdf", pdf(30)))
                        .param("categoria", "INFORME").with(profesional(PROFESIONAL)))
                .andExpect(status().isForbidden());
        assertThat(storage.cantidad()).isZero();
    }

    @Test
    void storageKeyEsGeneradaYLaIntegridadUsaElServicioExistente() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        byte[] contenido = pdf(35);
        JsonNode creada = subirPaciente(paciente, "Nombre resonancia agosto.pdf", contenido);
        UUID documentoId = UUID.fromString(creada.path("id").asText());
        DocumentoClinicoVersion version = versiones.findAllByDocumentoIdOrderByNumeroVersionDesc(documentoId).getFirst();
        assertThat(version.getStorageKey()).startsWith("patients/" + paciente.getId() + "/documents/")
                .doesNotContain("Nombre", "resonancia", "agosto.pdf");
        assertThat(version.getIntegridadHash()).isEqualTo(integridad.hash(contenido));
    }

    @Test
    void descargaYVistaValidanAccesoPeroNoGeneranAuditoria() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode creada = subirPaciente(paciente, "descarga.pdf", pdf(30));
        UUID id = UUID.fromString(creada.path("id").asText());
        int eventos = auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_UPLOAD, PROFESIONAL, paciente.getId()).size();

        mvc.perform(get("/api/archivos/{id}", id).with(profesional(PROFESIONAL))).andExpect(status().isOk());
        mvc.perform(get("/api/archivos/{id}/download", id).with(profesional(PROFESIONAL)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/archivos/{id}/download", id).with(profesional(PROFESIONAL + 1)))
                .andExpect(status().isNotFound());
        assertThat(auditoria.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.FILE_UPLOAD, PROFESIONAL, paciente.getId())).hasSize(eventos);
        assertThat(auditoria.findAll().stream().map(AuditLog::getAction))
                .noneMatch(a -> a.equals("FILE_VIEW") || a.equals("FILE_DOWNLOAD"));
    }

    @Test
    void previsualizaDocxComoHtmlProtegido() throws Exception {
        Paciente paciente = paciente(PROFESIONAL);
        JsonNode creado = subirPaciente(paciente, "informe.docx", docx());
        UUID id = UUID.fromString(creado.path("id").asText());

        String vista = mvc.perform(get("/api/archivos/{id}/preview", id).with(profesional(PROFESIONAL)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(vista).contains("Informe clínico", "Content-Security-Policy");

        mvc.perform(get("/api/archivos/{id}/preview", id).with(profesional(PROFESIONAL + 1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void metadataJpaNoContieneBinarios() {
        assertThat(Arrays.stream(DocumentoClinico.class.getDeclaredFields()).map(f -> f.getType()))
                .noneMatch(tipo -> tipo.equals(byte[].class));
        assertThat(Arrays.stream(DocumentoClinicoVersion.class.getDeclaredFields()).map(f -> f.getType()))
                .noneMatch(tipo -> tipo.equals(byte[].class));
    }

    private JsonNode subirPaciente(Paciente paciente, String nombre, byte[] contenido) throws Exception {
        return subir("/api/pacientes/" + paciente.getId() + "/archivos", nombre, contenido, PROFESIONAL);
    }

    private JsonNode subir(String ruta, String nombre, byte[] contenido, long profesional) throws Exception {
        String respuesta = mvc.perform(multipart(ruta)
                        .file(new MockMultipartFile("archivo", nombre, "application/octet-stream", contenido))
                        .param("categoria", "INFORME").param("descripcion", "Documento de prueba")
                        .with(profesional(profesional)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private void subirEsperando(Paciente paciente, MockMultipartFile archivo, int estado) throws Exception {
        mvc.perform(multipart("/api/pacientes/{id}/archivos", paciente.getId()).file(archivo)
                        .param("categoria", "INFORME").with(profesional(PROFESIONAL)))
                .andExpect(status().is(estado));
    }

    private JsonNode nuevaVersion(UUID documentoId, String nombre, byte[] contenido, String motivo) throws Exception {
        String respuesta = mvc.perform(multipart("/api/archivos/{id}/versions", documentoId)
                        .file(new MockMultipartFile("archivo", nombre, "application/pdf", contenido))
                        .param("motivo", motivo).with(profesional(PROFESIONAL)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(respuesta);
    }

    private Paciente paciente(long profesional) {
        return pacientes.saveAndFlush(new Paciente(profesional, "Paciente", "Archivos",
                String.valueOf(DNI.incrementAndGet()), null, LocalDate.of(1990, 1, 1), Sexo.FEMENINO));
    }

    private RequestPostProcessor profesional(long id) {
        return jwt().jwt(token -> token.subject("profesional-" + id).claim("professional_id", id)
                .claim("professional_name", "Profesional de prueba").claim("professional_license", "MP 92001"));
    }

    private static byte[] pdf(int tamanio) {
        byte[] bytes = new byte[Math.max(tamanio, 8)];
        Arrays.fill(bytes, (byte) 'A');
        System.arraycopy("%PDF-1.4".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 8);
        return bytes;
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    }

    private static byte[] jpg(int variacion) {
        return new byte[]{(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 1, (byte) variacion,
                (byte) 0xff, (byte) 0xd9};
    }

    private static byte[] docx() throws Exception {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream(); XWPFDocument documento = new XWPFDocument()) {
            documento.createParagraph().createRun().setText("Informe clínico");
            documento.write(salida);
            return salida.toByteArray();
        }
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary TestStorage testStorage() { return new TestStorage(); }
        @Bean @Primary TestScanner testScanner() { return new TestScanner(); }
    }

    static class TestStorage implements ClinicalFileStorage {
        private final Map<String, byte[]> objetos = new ConcurrentHashMap<>();
        private volatile boolean fallarAlmacenamiento;
        @Override public void almacenar(String storageKey, byte[] contenido, String mimeType) {
            if (fallarAlmacenamiento) {
                fallarAlmacenamiento = false;
                throw new ExcepcionServicioArchivosNoDisponible("Fallo de storage simulado", null);
            }
            objetos.put(storageKey, contenido.clone());
        }
        @Override public byte[] leer(String storageKey) { return objetos.get(storageKey).clone(); }
        @Override public boolean existe(String storageKey) { return objetos.containsKey(storageKey); }
        @Override public void eliminar(String storageKey) { objetos.remove(storageKey); }
        int cantidad() { return objetos.size(); }
        void limpiar() { objetos.clear(); fallarAlmacenamiento = false; }
        void fallarProximoAlmacenamiento() { fallarAlmacenamiento = true; }
        void corromper(String key) { objetos.put(key, "corrupto".getBytes(StandardCharsets.UTF_8)); }
        String keyDeVersion(UUID versionId) {
            return objetos.keySet().stream().filter(k -> k.endsWith(versionId.toString())).findFirst().orElseThrow();
        }
    }

    static class TestScanner implements FileMalwareScanner {
        @Override public void analizar(byte[] contenido) {
            if (new String(contenido, StandardCharsets.US_ASCII).contains("EICAR"))
                throw new ExcepcionMalwareDetectado("Malware detectado por el scanner de prueba");
        }
        void limpiar() {}
    }
}
