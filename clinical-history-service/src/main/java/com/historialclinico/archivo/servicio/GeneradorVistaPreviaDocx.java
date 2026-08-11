package com.historialclinico.archivo.servicio;

import com.historialclinico.excepcion.ExcepcionArchivoInvalido;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class GeneradorVistaPreviaDocx {
    private static final Set<String> IMAGENES_NAVEGADOR = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/bmp"
    );

    public byte[] generar(byte[] contenido, String nombreOriginal) {
        try (XWPFDocument documento = new XWPFDocument(new ByteArrayInputStream(contenido))) {
            StringBuilder cuerpo = new StringBuilder();
            documento.getHeaderList().forEach(encabezado -> {
                cuerpo.append("<header class=\"encabezado-docx\">");
                renderizarElementos(encabezado.getBodyElements(), cuerpo);
                cuerpo.append("</header>");
            });
            renderizarElementos(documento.getBodyElements(), cuerpo);
            documento.getFooterList().forEach(pie -> {
                cuerpo.append("<footer class=\"pie-docx\">");
                renderizarElementos(pie.getBodyElements(), cuerpo);
                cuerpo.append("</footer>");
            });
            return html(nombreOriginal, cuerpo.toString()).getBytes(StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException excepcion) {
            throw new ExcepcionArchivoInvalido("No fue posible generar la vista previa del archivo DOCX", excepcion);
        }
    }

    private void renderizarElementos(List<IBodyElement> elementos, StringBuilder html) {
        for (IBodyElement elemento : elementos) {
            if (elemento instanceof XWPFParagraph parrafo) renderizarParrafo(parrafo, html);
            else if (elemento instanceof XWPFTable tabla) renderizarTabla(tabla, html);
        }
    }

    private void renderizarParrafo(XWPFParagraph parrafo, StringBuilder html) {
        String etiqueta = etiquetaParrafo(parrafo);
        String alineacion = parrafo.getAlignment() == null
                ? "left" : parrafo.getAlignment().name().toLowerCase(Locale.ROOT);
        if (!Set.of("left", "center", "right", "both").contains(alineacion)) alineacion = "left";
        html.append('<').append(etiqueta).append(" style=\"text-align:")
                .append(alineacion.equals("both") ? "justify" : alineacion).append("\">");
        if (parrafo.getNumID() != null) html.append("<span class=\"marca-lista\">•</span>");
        boolean contenido = false;
        for (XWPFRun run : parrafo.getRuns()) {
            String texto = run.text();
            if (texto != null && !texto.isEmpty()) {
                renderizarRun(run, texto, html);
                contenido = true;
            }
            for (XWPFPicture imagen : run.getEmbeddedPictures()) {
                contenido |= renderizarImagen(imagen.getPictureData(), html);
            }
        }
        if (!contenido && !parrafo.getText().isBlank()) html.append(escaparTexto(parrafo.getText()));
        if (!contenido && parrafo.getText().isBlank()) html.append("&nbsp;");
        html.append("</").append(etiqueta).append('>');
    }

    private void renderizarRun(XWPFRun run, String texto, StringBuilder html) {
        StringBuilder estilo = new StringBuilder();
        Double tamanio = run.getFontSizeAsDouble();
        if (tamanio != null && tamanio >= 6 && tamanio <= 96) estilo.append("font-size:").append(tamanio).append("pt;");
        String color = run.getColor();
        if (color != null && color.matches("[0-9a-fA-F]{6}")) estilo.append("color:#").append(color).append(';');
        if (!estilo.isEmpty()) html.append("<span style=\"").append(estilo).append("\">");
        if (run.isBold()) html.append("<strong>");
        if (run.isItalic()) html.append("<em>");
        if (run.getUnderline() != null && !run.getUnderline().name().equals("NONE")) html.append("<u>");
        html.append(escaparTexto(texto));
        if (run.getUnderline() != null && !run.getUnderline().name().equals("NONE")) html.append("</u>");
        if (run.isItalic()) html.append("</em>");
        if (run.isBold()) html.append("</strong>");
        if (!estilo.isEmpty()) html.append("</span>");
    }

    private boolean renderizarImagen(XWPFPictureData imagen, StringBuilder html) {
        if (imagen == null) return false;
        String mime = imagen.getPackagePart().getContentType();
        if (!IMAGENES_NAVEGADOR.contains(mime)) return false;
        String base64 = Base64.getEncoder().encodeToString(imagen.getData());
        html.append("<img alt=\"Imagen incluida en el documento\" src=\"data:")
                .append(mime).append(";base64,").append(base64).append("\">");
        return true;
    }

    private void renderizarTabla(XWPFTable tabla, StringBuilder html) {
        html.append("<table><tbody>");
        for (XWPFTableRow fila : tabla.getRows()) {
            html.append("<tr>");
            for (XWPFTableCell celda : fila.getTableCells()) {
                html.append("<td>");
                renderizarElementos(celda.getBodyElements(), html);
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private String etiquetaParrafo(XWPFParagraph parrafo) {
        String estilo = parrafo.getStyle();
        if (estilo == null) return "p";
        String normalizado = estilo.toLowerCase(Locale.ROOT);
        if (normalizado.contains("title") || normalizado.contains("titulo")) return "h1";
        if (normalizado.contains("heading1") || normalizado.contains("heading 1")) return "h2";
        if (normalizado.contains("heading2") || normalizado.contains("heading 2")) return "h3";
        return "p";
    }

    private String escaparTexto(String valor) {
        return escapar(valor).replace("\r\n", "<br>").replace("\n", "<br>").replace("\t", "&emsp;");
    }

    private String escapar(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String html(String nombreOriginal, String cuerpo) {
        return """
                <!doctype html>
                <html lang="es">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'">
                  <title>%s</title>
                  <style>
                    *{box-sizing:border-box}body{margin:0;padding:32px;background:#edf2f0;color:#203b34;font-family:Arial,sans-serif}
                    .documento{width:min(900px,100%%);min-height:1120px;margin:auto;padding:72px 78px;background:white;box-shadow:0 12px 40px #18362d26}
                    p{margin:.45em 0;min-height:1em;line-height:1.55;white-space:normal}h1,h2,h3{color:#1f473c;line-height:1.25}h1{font-size:26pt}h2{font-size:19pt}h3{font-size:15pt}
                    table{width:100%%;margin:16px 0;border-collapse:collapse}td{padding:9px;border:1px solid #b9c9c3;vertical-align:top}td p{margin:.15em 0}
                    img{display:block;max-width:100%%;height:auto;margin:14px auto}.marca-lista{display:inline-block;margin-right:9px}
                    .encabezado-docx,.pie-docx{color:#61736d;font-size:9pt}.encabezado-docx{padding-bottom:14px;border-bottom:1px solid #d9e3df}.pie-docx{margin-top:32px;padding-top:14px;border-top:1px solid #d9e3df}
                    @media(max-width:700px){body{padding:0}.documento{min-height:100vh;padding:36px 24px;box-shadow:none}}
                    @media print{body{padding:0;background:white}.documento{width:100%%;box-shadow:none}}
                  </style>
                </head>
                <body><main class="documento">%s</main></body>
                </html>
                """.formatted(escapar(nombreOriginal), cuerpo);
    }
}
