package com.historialclinico.exportacion.controlador;

import com.historialclinico.auditoria.modelo.AuditLog;
import com.historialclinico.auditoria.modelo.ResultadoAuditLog;
import com.historialclinico.auditoria.repositorio.RepositorioAuditLog;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import com.historialclinico.exportacion.repositorio.RepositorioExportacionHistoriaClinica;
import com.historialclinico.paciente.modelo.Paciente;
import com.historialclinico.paciente.modelo.Sexo;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ControladorExportacionHistoriaClinicaIntegrationTest {
    private static final long PROFESIONAL = 91001L;
    @Autowired MockMvc mockMvc;
    @Autowired RepositorioPaciente repositorioPacientes;
    @Autowired RepositorioExportacionHistoriaClinica repositorioExportaciones;
    @Autowired RepositorioAuditLog repositorioAuditLog;

    @Test
    void busquedaSoloDevuelvePacientesDelProfesionalAutenticado() throws Exception {
        Paciente propio = crearPaciente(PROFESIONAL, "Propio", "Buscable", "40910000");
        crearPaciente(PROFESIONAL + 1, "Ajeno", "Oculto", "40919999");

        mockMvc.perform(get("/api/pacientes").with(profesional(PROFESIONAL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(propio.getId())).exists())
                .andExpect(jsonPath("$[?(@.dni == '40919999')]").isEmpty());
    }

    @Test
    void profesionalExportaPacientePropioEnPdfYRegistraMetadataHashYAuditLog() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Ana María", "Pérez", "40910001");
        byte[] contenido = mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", paciente.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"formato":"PDF","motivo":"SOLICITUD_DEL_PACIENTE",
                                 "detalleMotivo":"Copia solicitada por el paciente"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(contenido, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var pdf = Loader.loadPDF(contenido)) {
            assertThat(pdf.getNumberOfPages()).isPositive();
            assertThat(new PDFTextStripper().getText(pdf)).contains("Pérez, Ana María", "40910001");
        }
        var exportaciones = repositorioExportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(PROFESIONAL, paciente.getId());
        assertThat(exportaciones).hasSize(1);
        assertThat(exportaciones.getFirst().getProfesionalId()).isEqualTo(PROFESIONAL);
        assertThat(exportaciones.getFirst().getHashArchivo()).isEqualTo(sha256(contenido)).hasSize(64);
        assertThat(exportaciones.getFirst().getFechaHoraExportacion()).isNotNull();
        assertThat(exportaciones.getFirst().getNombreArchivo()).endsWith(".pdf");
        assertThat(repositorioAuditLog.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.EXPORT_CLINICAL_HISTORY, PROFESIONAL, paciente.getId()))
                .extracting(evento -> evento.getResultado()).containsExactly(ResultadoAuditLog.SUCCESS);
    }

    @Test
    void profesionalNoPuedeExportarPacienteAjenoYRegistraFallo() throws Exception {
        Paciente ajeno = crearPaciente(PROFESIONAL + 1, "Luis", "Ajeno", "40910002");
        mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", ajeno.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formato\":\"PDF\",\"motivo\":\"DERIVACION\"}"))
                .andExpect(status().isForbidden());

        assertThat(repositorioExportaciones
                .findAllByProfesionalIdAndPacienteIdOrderByFechaHoraExportacionDesc(PROFESIONAL, ajeno.getId())).isEmpty();
        assertThat(repositorioAuditLog.findAllByActionAndProfesionalIdAndPacienteIdOrderByFechaHoraAsc(
                AuditLog.EXPORT_CLINICAL_HISTORY, PROFESIONAL, ajeno.getId()))
                .extracting(evento -> evento.getResultado()).containsExactly(ResultadoAuditLog.FAILED);
    }

    @Test
    void rechazaFormatoInvalidoYMotivoAusente() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Julia", "Validación", "40910003");
        mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", paciente.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formato\":\"ODT\",\"motivo\":\"DERIVACION\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", paciente.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formato\":\"CSV\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generaCsvValidoConDatosIdentificatorios() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Carla", "Csv", "40910004");
        byte[] contenido = exportar(paciente, "CSV");
        String csv = new String(contenido, StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFSección,Fecha,Registro,Campo,Valor\r\n")
                .contains("\"Csv, Carla\"").contains("40910004");
    }

    @Test
    void generaXlsxValidoConDatosIdentificatorios() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Elena", "Excel", "40910005");
        byte[] contenido = exportar(paciente, "XLSX");
        try (var libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            var hoja = libro.getSheet("Historia clínica");
            assertThat(hoja).isNotNull();
            assertThat(hoja.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Sección");
            assertThat(hoja.getRow(1).getCell(4).getStringCellValue()).isEqualTo("Excel, Elena");
        }
    }

    @Test
    void generaDocxValidoConPresentacionClinica() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Marta", "Word", "40910007");
        var respuesta = mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", paciente.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formato\":\"DOCX\",\"motivo\":\"CONTINUIDAD_DE_TRATAMIENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".docx")))
                .andReturn().getResponse();
        byte[] contenido = respuesta.getContentAsByteArray();
        assertThat(new String(contenido, 0, 2, StandardCharsets.US_ASCII)).isEqualTo("PK");
        try (var documento = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(contenido))) {
            String texto = documento.getParagraphs().stream()
                    .map(org.apache.poi.xwpf.usermodel.XWPFParagraph::getText)
                    .collect(java.util.stream.Collectors.joining(" "));
            assertThat(texto).contains("Historia clínica completa", "Cronología clínica");
            assertThat(documento.getTables()).isNotEmpty();
            assertThat(documento.getTables().getFirst().getText()).contains("Word, Marta", "40910007");
            assertThat(documento.getDocument().getBody().getSectPr().getPgSz().getW())
                    .isEqualTo(java.math.BigInteger.valueOf(11906));
            assertThat(colorHex(documento.getStyles().getStyle("ClinicalTitle").getCTStyle()
                    .getRPr().getColorArray(0).getVal())).isEqualTo("17372F");
            assertThat(documento.getHeaderList()).hasSize(1);
            assertThat(colorHex(documento.getHeaderList().getFirst().getTables().getFirst()
                    .getRow(0).getCell(0).getCTTc().getTcPr().getShd().getFill())).isEqualTo("17372F");
            assertThat(colorHex(documento.getTables().getFirst().getRow(1).getCell(0)
                    .getCTTc().getTcPr().getShd().getFill())).isEqualTo("E7F2ED");
        }
    }

    @Test
    void incluyeTratamientosConsultasYEpicrisisEnOrdenCronologico() throws Exception {
        Paciente paciente = crearPaciente(PROFESIONAL, "Nora", "Cronología", "40910006");
        mockMvc.perform(post("/api/v1/profesionales/{profesional}/pacientes/{paciente}/tratamientos",
                        PROFESIONAL, paciente.getId()).with(profesional(PROFESIONAL))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"nombre":"Plan respiratorio","descripcion":"Plan inicial","cantidadSesionesTotal":2,
                                 "primeraSesion":{"observaciones":"Primera consulta documentada"}}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/profesionales/{profesional}/pacientes/{paciente}/epicrisis",
                        PROFESIONAL, paciente.getId()).with(profesional(PROFESIONAL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"Epicrisis posterior a la consulta\"}"))
                .andExpect(status().isCreated());

        String csv = new String(exportar(paciente, "CSV"), StandardCharsets.UTF_8);
        assertThat(csv).contains("Plan respiratorio", "Primera consulta documentada",
                "Epicrisis posterior a la consulta", "CONSULTA / SESIÓN");
        assertThat(csv.indexOf("Plan respiratorio")).isLessThan(csv.indexOf("Primera consulta documentada"));
        assertThat(csv.indexOf("Primera consulta documentada"))
                .isLessThan(csv.indexOf("Epicrisis posterior a la consulta"));

        Path qa = Path.of("target", "qa-exportacion");
        Files.createDirectories(qa);
        byte[] pdf = exportar(paciente, "PDF");
        Files.write(qa.resolve("historia-clinica-muestra.pdf"), pdf);
        byte[] docx = exportar(paciente, "DOCX");
        Files.write(qa.resolve("historia-clinica-muestra.docx"), docx);
        try (var documentoWord = new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(docx))) {
            assertThat(documentoWord.getTables()).hasSizeGreaterThanOrEqualTo(4);
            var tarjetaRegistro = documentoWord.getTables().get(1).getRow(0).getCell(0).getCTTc().getTcPr();
            assertThat(colorHex(tarjetaRegistro.getShd().getFill())).isEqualTo("F7FAF8");
            assertThat(colorHex(tarjetaRegistro.getTcBorders().getStart().getColor())).isEqualTo("176B57");
            assertThat(documentoWord.getTables().get(1).getText())
                    .contains("CONSULTA / SESIÓN", "Primera consulta documentada");
        }
        try (var documento = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(documento);
            for (int pagina = 0; pagina < documento.getNumberOfPages(); pagina++) {
                ImageIO.write(renderer.renderImageWithDPI(pagina, 144, ImageType.RGB), "png",
                        qa.resolve("historia-clinica-muestra-pagina-%02d.png".formatted(pagina + 1)).toFile());
            }
        }
    }

    private byte[] exportar(Paciente paciente, String formato) throws Exception {
        return mockMvc.perform(post("/api/pacientes/{pacienteId}/historia-clinica/exportar", paciente.getId())
                        .with(profesional(PROFESIONAL)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"formato\":\"%s\",\"motivo\":\"CONTINUIDAD_DE_TRATAMIENTO\"}".formatted(formato)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    }

    private static String colorHex(Object valor) {
        if (valor instanceof byte[] bytes) return HexFormat.of().formatHex(bytes).toUpperCase();
        return String.valueOf(valor).toUpperCase();
    }

    private Paciente crearPaciente(long profesionalId, String nombre, String apellido, String dni) {
        return repositorioPacientes.saveAndFlush(new Paciente(profesionalId, nombre, apellido, dni, "3515550000",
                LocalDate.of(1990, 1, 15), Sexo.FEMENINO));
    }

    private RequestPostProcessor profesional(long id) {
        return jwt().jwt(token -> token.subject("profesional-" + id)
                .claim("professional_id", id)
                .claim("professional_name", "Profesional de prueba")
                .claim("professional_license", "MP 91001"));
    }

    private String sha256(byte[] contenido) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido));
    }
}
