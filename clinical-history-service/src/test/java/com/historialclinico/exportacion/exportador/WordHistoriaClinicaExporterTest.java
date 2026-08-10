package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class WordHistoriaClinicaExporterTest {

    @Test
    void generaDocumentoExtensoConRegistrosMultipagina() throws Exception {
        Instant inicio = Instant.parse("2026-01-10T12:00:00Z");
        List<HistoriaClinicaDocumento.RegistroClinico> registros = IntStream.rangeClosed(1, 12)
                .mapToObj(numero -> new HistoriaClinicaDocumento.RegistroClinico(
                        inicio.plus(numero, ChronoUnit.DAYS),
                        numero % 3 == 0 ? "EPICRISIS" : "CONSULTA / SESIÓN",
                        numero % 3 == 0 ? "Epicrisis de seguimiento" : "Sesión N.º " + numero,
                        List.of(
                                new HistoriaClinicaDocumento.Campo("Tratamiento", "Plan integral de rehabilitación"),
                                new HistoriaClinicaDocumento.Campo("Observaciones",
                                        "Evolución clínica favorable. Se registran hallazgos, respuesta al tratamiento "
                                                + "y recomendaciones para asegurar la continuidad del seguimiento del paciente."),
                                new HistoriaClinicaDocumento.Campo("Indicaciones",
                                        "Mantener las pautas indicadas y realizar un nuevo control según evolución clínica."))))
                .toList();
        var historia = new HistoriaClinicaDocumento(
                Instant.parse("2026-08-10T20:00:00Z"),
                new HistoriaClinicaDocumento.Paciente(
                        "Valentina", "Demostración", "40123456", "3515550000",
                        LocalDate.of(1988, 4, 23), "FEMENINO"),
                registros);

        byte[] contenido = new WordHistoriaClinicaExporter().exportar(historia);
        Path qa = Path.of("target", "qa-exportacion");
        Files.createDirectories(qa);
        Files.write(qa.resolve("historia-clinica-extensa.docx"), contenido);

        try (var documento = new XWPFDocument(new ByteArrayInputStream(contenido))) {
            assertThat(documento.getTables()).hasSize(13);
            assertThat(documento.getTables().getLast().getText())
                    .contains("Epicrisis de seguimiento", "Mantener las pautas indicadas");
            assertThat(documento.getHeaderList()).hasSize(1);
            assertThat(documento.getFooterList()).hasSize(1);
        }
    }
}
