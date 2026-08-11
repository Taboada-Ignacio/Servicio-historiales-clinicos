package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfHistoriaClinicaExporter implements HistoriaClinicaExporter {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    @Override public FormatoExportacion formato() { return FormatoExportacion.PDF; }

    @Override
    public byte[] exportar(HistoriaClinicaDocumento historia) {
        try (var pdf = new PDDocument(); var salida = new ByteArrayOutputStream()) {
            var escritor = new EscritorPdf(pdf);
            escritor.portada(historia);
            if (!historia.archivosPaciente().isEmpty()) {
                escritor.seccion("Archivos del paciente", "Documentación asociada directamente al paciente");
                escritor.archivosAdjuntos(historia.archivosPaciente(), EscritorPdf.MARGEN, EscritorPdf.ANCHO);
            }
            escritor.seccion("Cronología clínica", "Registros presentados desde el más antiguo al más reciente");
            if (historia.registros().isEmpty()) escritor.aviso("No hay registros clínicos disponibles.");
            for (var registro : historia.registros()) escritor.registro(registro);
            escritor.cerrar();
            pdf.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar la historia clínica en PDF", ex);
        }
    }

    private static final class EscritorPdf {
        private static final float MARGEN = 50;
        private static final float PIE = 42;
        private static final float ANCHO = PDRectangle.A4.getWidth() - MARGEN * 2;
        private static final Color VERDE = new Color(23, 107, 87);
        private static final Color VERDE_OSCURO = new Color(23, 55, 47);
        private static final Color VERDE_CLARO = new Color(231, 242, 237);
        private static final Color TINTA = new Color(38, 56, 52);
        private static final Color GRIS = new Color(101, 116, 111);
        private static final Color BORDE = new Color(220, 229, 225);
        private final PDDocument documento;
        private final PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPage pagina;
        private PDPageContentStream contenido;
        private float y;

        private EscritorPdf(PDDocument documento) throws IOException {
            this.documento = documento;
            paginaNueva();
        }

        private void portada(HistoriaClinicaDocumento historia) throws IOException {
            texto("REGISTRO CLÍNICO CONSOLIDADO", MARGEN, y, ANCHO, 9, negrita, VERDE, 12);
            y -= 24;
            texto("Historia clínica completa", MARGEN, y, ANCHO, 24, negrita, VERDE_OSCURO, 29);
            y -= 38;
            texto("Generada el " + FECHA.format(historia.fechaGeneracion())
                    + "  ·  Documento en orden cronológico", MARGEN, y, ANCHO, 9, normal, GRIS, 12);
            y -= 28;
            tarjetaPaciente(historia.paciente());
        }

        private void tarjetaPaciente(HistoriaClinicaDocumento.Paciente paciente) throws IOException {
            asegurarEspacio(118);
            float alto = 112;
            float superior = y;
            rectangulo(MARGEN, superior - alto, ANCHO, alto, VERDE_CLARO);
            rectangulo(MARGEN, superior - 5, ANCHO, 5, VERDE);
            texto("DATOS IDENTIFICATORIOS DEL PACIENTE", MARGEN + 16, superior - 24, ANCHO - 32,
                    8, negrita, VERDE, 10);
            float mitad = ANCHO / 2;
            datoTarjeta("Apellido y nombre", paciente.apellido() + ", " + paciente.nombre(),
                    MARGEN + 16, superior - 46, mitad - 28);
            datoTarjeta("DNI", paciente.dni(), MARGEN + mitad + 6, superior - 46, mitad - 22);
            datoTarjeta("Fecha de nacimiento", paciente.fechaNacimiento().toString(),
                    MARGEN + 16, superior - 79, mitad - 28);
            datoTarjeta("Sexo", paciente.sexo(), MARGEN + mitad + 6, superior - 79, mitad - 22);
            texto("Teléfono: " + (paciente.telefono() == null || paciente.telefono().isBlank()
                    ? "No informado" : paciente.telefono()), MARGEN + 16, superior - 101, ANCHO - 32,
                    8.5f, normal, TINTA, 11);
            y -= alto + 14;
        }

        private void datoTarjeta(String etiqueta, String valor, float x, float posicionY, float ancho) throws IOException {
            texto(etiqueta.toUpperCase(), x, posicionY, ancho, 7, negrita, GRIS, 9);
            texto(valor, x, posicionY - 13, ancho, 10, negrita, VERDE_OSCURO, 12);
        }

        private void seccion(String titulo, String descripcion) throws IOException {
            asegurarEspacio(62);
            y -= 10;
            texto(titulo, MARGEN, y, ANCHO, 17, negrita, VERDE_OSCURO, 21);
            y -= 25;
            texto(descripcion, MARGEN, y, ANCHO, 9, normal, GRIS, 12);
            y -= 22;
            lineaHorizontal(MARGEN, y, MARGEN + ANCHO, BORDE, 0.8f);
            y -= 16;
        }

        private void aviso(String mensaje) throws IOException {
            asegurarEspacio(52);
            rectangulo(MARGEN, y - 42, ANCHO, 42, VERDE_CLARO);
            texto(mensaje, MARGEN + 14, y - 17, ANCHO - 28, 10, normal, VERDE_OSCURO, 13);
            y -= 54;
        }

        private void registro(HistoriaClinicaDocumento.RegistroClinico registro) throws IOException {
            asegurarEspacio(90);
            float xContenido = MARGEN + 27;
            float anchoContenido = ANCHO - 27;
            circulo(MARGEN + 7, y - 5, 4, VERDE);
            lineaVertical(MARGEN + 7, y - 11, y - 31, BORDE, 1.1f);
            texto(FECHA.format(registro.fecha()) + "  ·  " + registro.tipo(), xContenido, y,
                    anchoContenido, 8, negrita, VERDE, 10);
            y -= 18;
            textoFluido(registro.titulo(), xContenido, anchoContenido, 13, negrita, VERDE_OSCURO, 16);
            y -= 6;
            if (registro.campos().isEmpty()) {
                textoFluido("Sin datos adicionales.", xContenido, anchoContenido, 9.5f, normal, GRIS, 13);
            }
            for (var campo : registro.campos()) {
                asegurarEspacio(42);
                texto(campo.nombre().toUpperCase(), xContenido, y, anchoContenido, 7.2f, negrita, GRIS, 9);
                y -= 12;
                textoFluido(campo.valor(), xContenido, anchoContenido, 9.5f, normal, TINTA, 13);
                y -= 7;
            }
            archivosAdjuntos(registro.archivosAdjuntos(), xContenido, anchoContenido);
            lineaHorizontal(xContenido, y, MARGEN + ANCHO, BORDE, 0.6f);
            y -= 18;
        }

        private void archivosAdjuntos(List<HistoriaClinicaDocumento.ArchivoAdjunto> archivos,
                float x, float ancho) throws IOException {
            if (archivos.isEmpty()) return;
            asegurarEspacio(36);
            texto("ARCHIVOS ADJUNTOS", x, y, ancho, 7.2f, negrita, GRIS, 9);
            y -= 13;
            for (var archivo : archivos) {
                textoFluido(describir(archivo), x, ancho, 9.5f, normal, TINTA, 13);
                y -= 3;
            }
            y -= 4;
        }

        private String describir(HistoriaClinicaDocumento.ArchivoAdjunto archivo) {
            String descripcion = archivo.descripcion() == null || archivo.descripcion().isBlank()
                    ? "" : " — " + archivo.descripcion();
            return "- " + archivo.nombreOriginal() + " (" + archivo.categoria() + ")" + descripcion;
        }

        private void textoFluido(String texto, float x, float ancho, float tamanio, PDFont fuente,
                Color color, float altoLinea) throws IOException {
            for (String renglon : envolver(limpiar(texto, fuente), fuente, tamanio, ancho)) {
                if (y - altoLinea < PIE) paginaNueva();
                texto(renglon, x, y, ancho, tamanio, fuente, color, altoLinea);
                y -= altoLinea;
            }
        }

        private float texto(String texto, float x, float posicionY, float ancho, float tamanio,
                PDFont fuente, Color color, float altoLinea) throws IOException {
            List<String> lineas = envolver(limpiar(texto, fuente), fuente, tamanio, ancho);
            float cursor = posicionY;
            for (String renglon : lineas) {
                contenido.beginText();
                contenido.setNonStrokingColor(color);
                contenido.setFont(fuente, tamanio);
                contenido.newLineAtOffset(x, cursor);
                contenido.showText(renglon);
                contenido.endText();
                cursor -= altoLinea;
            }
            return lineas.size() * altoLinea;
        }

        private List<String> envolver(String texto, PDFont fuente, float tamanio, float ancho) throws IOException {
            List<String> lineas = new ArrayList<>();
            StringBuilder actual = new StringBuilder();
            for (String palabraOriginal : texto.replaceAll("[\\r\\n]+", " ").trim().split("\\s+")) {
                for (String palabra : dividirPalabra(palabraOriginal, fuente, tamanio, ancho)) {
                    String candidata = actual.isEmpty() ? palabra : actual + " " + palabra;
                    if (!actual.isEmpty() && medir(candidata, fuente, tamanio) > ancho) {
                        lineas.add(actual.toString());
                        actual = new StringBuilder(palabra);
                    } else actual = new StringBuilder(candidata);
                }
            }
            if (!actual.isEmpty()) lineas.add(actual.toString());
            if (lineas.isEmpty()) lineas.add("");
            return lineas;
        }

        private List<String> dividirPalabra(String palabra, PDFont fuente, float tamanio, float ancho) throws IOException {
            if (medir(palabra, fuente, tamanio) <= ancho) return List.of(palabra);
            List<String> partes = new ArrayList<>();
            StringBuilder parte = new StringBuilder();
            for (int i = 0; i < palabra.length(); i++) {
                String candidata = parte.toString() + palabra.charAt(i);
                if (!parte.isEmpty() && medir(candidata, fuente, tamanio) > ancho) {
                    partes.add(parte.toString()); parte = new StringBuilder();
                }
                parte.append(palabra.charAt(i));
            }
            if (!parte.isEmpty()) partes.add(parte.toString());
            return partes;
        }

        private float medir(String texto, PDFont fuente, float tamanio) throws IOException {
            return fuente.getStringWidth(texto) / 1000 * tamanio;
        }

        private String limpiar(String texto, PDFont fuente) {
            if (texto == null) return "";
            StringBuilder limpio = new StringBuilder();
            texto.codePoints().forEach(cp -> {
                String caracter = new String(Character.toChars(cp));
                try { fuente.encode(caracter); limpio.append(caracter); }
                catch (Exception ex) { limpio.append('?'); }
            });
            return limpio.toString();
        }

        private void asegurarEspacio(float alto) throws IOException {
            if (y - alto < PIE) paginaNueva();
        }

        private void rectangulo(float x, float y, float ancho, float alto, Color color) throws IOException {
            contenido.setNonStrokingColor(color); contenido.addRect(x, y, ancho, alto); contenido.fill();
        }

        private void circulo(float x, float y, float radio, Color color) throws IOException {
            float k = 0.552284749831f * radio;
            contenido.setNonStrokingColor(color); contenido.moveTo(x + radio, y);
            contenido.curveTo(x + radio, y + k, x + k, y + radio, x, y + radio);
            contenido.curveTo(x - k, y + radio, x - radio, y + k, x - radio, y);
            contenido.curveTo(x - radio, y - k, x - k, y - radio, x, y - radio);
            contenido.curveTo(x + k, y - radio, x + radio, y - k, x + radio, y); contenido.fill();
        }

        private void lineaHorizontal(float x1, float y, float x2, Color color, float grosor) throws IOException {
            contenido.setStrokingColor(color); contenido.setLineWidth(grosor);
            contenido.moveTo(x1, y); contenido.lineTo(x2, y); contenido.stroke();
        }

        private void lineaVertical(float x, float y1, float y2, Color color, float grosor) throws IOException {
            contenido.setStrokingColor(color); contenido.setLineWidth(grosor);
            contenido.moveTo(x, y1); contenido.lineTo(x, y2); contenido.stroke();
        }

        private void paginaNueva() throws IOException {
            if (contenido != null) contenido.close();
            pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            contenido = new PDPageContentStream(documento, pagina);
            rectangulo(0, PDRectangle.A4.getHeight() - 46, PDRectangle.A4.getWidth(), 46, VERDE_OSCURO);
            texto("CLÍNICA", MARGEN, PDRectangle.A4.getHeight() - 28, 90, 9, negrita, Color.WHITE, 11);
            texto("HISTORIA CLÍNICA", MARGEN + 78, PDRectangle.A4.getHeight() - 28, 160, 8, normal,
                    new Color(198, 222, 214), 10);
            y = PDRectangle.A4.getHeight() - 72;
        }

        private void cerrar() throws IOException {
            contenido.close();
            int total = documento.getNumberOfPages();
            for (int i = 0; i < total; i++) {
                PDPage paginaPie = documento.getPage(i);
                try (var pie = new PDPageContentStream(documento, paginaPie,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    pie.setStrokingColor(BORDE); pie.setLineWidth(0.6f);
                    pie.moveTo(MARGEN, 34); pie.lineTo(MARGEN + ANCHO, 34); pie.stroke();
                    textoPie(pie, "Documento clínico confidencial", MARGEN, 20, normal, 7.5f, GRIS);
                    String paginas = "Página " + (i + 1) + " de " + total;
                    float anchoTexto = medir(paginas, normal, 7.5f);
                    textoPie(pie, paginas, MARGEN + ANCHO - anchoTexto, 20, normal, 7.5f, GRIS);
                }
            }
        }

        private void textoPie(PDPageContentStream stream, String texto, float x, float y, PDFont fuente,
                float tamanio, Color color) throws IOException {
            stream.beginText(); stream.setNonStrokingColor(color); stream.setFont(fuente, tamanio);
            stream.newLineAtOffset(x, y); stream.showText(limpiar(texto, fuente)); stream.endText();
        }
    }
}
