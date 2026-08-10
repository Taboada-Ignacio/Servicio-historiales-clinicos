package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPrGeneral;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSpacing;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyle;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblCellMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTc;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcBorders;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STStyleType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblLayoutType;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STVerticalJc;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class WordHistoriaClinicaExporter implements HistoriaClinicaExporter {
    private static final String FUENTE = "Arial";
    private static final String VERDE = "176B57";
    private static final String VERDE_OSCURO = "17372F";
    private static final String VERDE_MEDIO = "C6DED6";
    private static final String VERDE_CLARO = "E7F2ED";
    private static final String FONDO_REGISTRO = "F7FAF8";
    private static final String TINTA = "263834";
    private static final String GRIS = "65746F";
    private static final String BORDE = "DCE5E1";
    private static final int ANCHO_UTIL = 9978;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.of("America/Argentina/Buenos_Aires"));

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.DOCX;
    }

    @Override
    public byte[] exportar(HistoriaClinicaDocumento historia) {
        try (var documento = new XWPFDocument(); var salida = new ByteArrayOutputStream()) {
            configurarDocumento(documento);
            crearEncabezadoYPie(documento);
            crearPortada(documento, historia);
            crearDatosPaciente(documento, historia.paciente());
            crearCronologia(documento, historia);
            documento.getProperties().getCoreProperties().setTitle("Historia clínica completa");
            documento.getProperties().getCoreProperties().setCreator("Servicio de historias clínicas");
            documento.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar la historia clínica en Word", ex);
        }
    }

    private void configurarDocumento(XWPFDocument documento) {
        CTSectPr seccion = documento.getDocument().getBody().isSetSectPr()
                ? documento.getDocument().getBody().getSectPr()
                : documento.getDocument().getBody().addNewSectPr();
        var pagina = seccion.addNewPgSz();
        pagina.setW(BigInteger.valueOf(11906));
        pagina.setH(BigInteger.valueOf(16838));
        var margenes = seccion.addNewPgMar();
        margenes.setTop(BigInteger.valueOf(850));
        margenes.setBottom(BigInteger.valueOf(900));
        margenes.setLeft(BigInteger.valueOf(964));
        margenes.setRight(BigInteger.valueOf(964));
        margenes.setHeader(BigInteger.valueOf(360));
        margenes.setFooter(BigInteger.valueOf(420));

        XWPFStyles estilos = documento.createStyles();
        estilos.addStyle(estiloParrafo("ClinicalTitle", "Título clínico", 48, VERDE_OSCURO,
                true, 0, 100, 290));
        estilos.addStyle(estiloParrafo("ClinicalSubtitle", "Subtítulo clínico", 18, GRIS,
                false, 0, 220, 276));
        estilos.addStyle(estiloParrafo("ClinicalHeading1", "Sección clínica", 34, VERDE_OSCURO,
                true, 220, 70, 290));
        estilos.addStyle(estiloParrafo("ClinicalHeading2", "Registro clínico", 26, VERDE_OSCURO,
                true, 0, 90, 276));
        estilos.addStyle(estiloParrafo("ClinicalMeta", "Metadatos clínicos", 16, VERDE,
                true, 0, 45, 250));
        estilos.addStyle(estiloParrafo("ClinicalLabel", "Etiqueta clínica", 15, GRIS,
                true, 70, 20, 240));
        estilos.addStyle(estiloParrafo("ClinicalBody", "Cuerpo clínico", 19, TINTA,
                false, 0, 100, 276));
        estilos.addStyle(estiloParrafo("ClinicalPatientValue", "Dato del paciente", 21, VERDE_OSCURO,
                true, 0, 70, 260));
        estilos.addStyle(estiloParrafo("ClinicalFooter", "Pie clínico", 15, GRIS,
                false, 0, 0, 240));
    }

    private XWPFStyle estiloParrafo(String id, String nombre, int mediosPuntos, String color,
            boolean negrita, int antes, int despues, int interlineado) {
        CTStyle ct = CTStyle.Factory.newInstance();
        ct.setStyleId(id);
        ct.setType(STStyleType.PARAGRAPH);
        ct.addNewName().setVal(nombre);
        CTPPrGeneral ppr = ct.addNewPPr();
        CTSpacing espacio = ppr.addNewSpacing();
        espacio.setBefore(BigInteger.valueOf(antes));
        espacio.setAfter(BigInteger.valueOf(despues));
        espacio.setLine(BigInteger.valueOf(interlineado));
        espacio.setLineRule(STLineSpacingRule.AUTO);
        CTRPr rpr = ct.addNewRPr();
        configurarFuente(rpr, mediosPuntos, color, negrita);
        return new XWPFStyle(ct);
    }

    private void crearEncabezadoYPie(XWPFDocument documento) {
        XWPFHeader encabezado = documento.createHeader(HeaderFooterType.DEFAULT);
        if (!encabezado.getParagraphs().isEmpty()) compactarParrafoVacio(encabezado.getParagraphs().getFirst());
        XWPFTable banda = encabezado.createTable(1, 2);
        configurarTabla(banda, new int[]{1900, ANCHO_UTIL - 1900}, 0, 110, 160, 110, 160);
        sinBordes(banda);
        sombrear(banda.getRow(0).getCell(0), VERDE_OSCURO);
        sombrear(banda.getRow(0).getCell(1), VERDE_OSCURO);
        parrafoCabecera(banda.getRow(0).getCell(0), "CLÍNICA", true, "FFFFFF");
        parrafoCabecera(banda.getRow(0).getCell(1), "HISTORIA CLÍNICA", false, VERDE_MEDIO);

        XWPFFooter pie = documento.createFooter(HeaderFooterType.DEFAULT);
        if (!pie.getParagraphs().isEmpty()) compactarParrafoVacio(pie.getParagraphs().getFirst());
        XWPFTable tablaPie = pie.createTable(1, 2);
        configurarTabla(tablaPie, new int[]{ANCHO_UTIL / 2, ANCHO_UTIL - ANCHO_UTIL / 2},
                0, 90, 0, 40, 0);
        sinBordes(tablaPie);
        bordeSuperior(tablaPie.getRow(0).getCell(0), BORDE, 6);
        bordeSuperior(tablaPie.getRow(0).getCell(1), BORDE, 6);
        XWPFParagraph confidencial = reiniciarCelda(tablaPie.getRow(0).getCell(0));
        confidencial.setStyle("ClinicalFooter");
        confidencial.createRun().setText("Documento clínico confidencial");
        XWPFParagraph paginas = reiniciarCelda(tablaPie.getRow(0).getCell(1));
        paginas.setStyle("ClinicalFooter");
        paginas.setAlignment(ParagraphAlignment.RIGHT);
        run(paginas, "Página ", 8, GRIS, false);
        campo(paginas, "PAGE");
        run(paginas, " de ", 8, GRIS, false);
        campo(paginas, "NUMPAGES");
    }

    private void crearPortada(XWPFDocument documento, HistoriaClinicaDocumento historia) {
        XWPFParagraph etiqueta = documento.createParagraph();
        etiqueta.setSpacingBefore(170);
        etiqueta.setSpacingAfter(55);
        run(etiqueta, "REGISTRO CLÍNICO CONSOLIDADO", 9, VERDE, true);

        XWPFParagraph titulo = documento.createParagraph();
        titulo.setStyle("ClinicalTitle");
        titulo.createRun().setText("Historia clínica completa");

        XWPFParagraph subtitulo = documento.createParagraph();
        subtitulo.setStyle("ClinicalSubtitle");
        subtitulo.createRun().setText("Generada el " + FECHA.format(historia.fechaGeneracion())
                + "  ·  Documento en orden cronológico");
    }

    private void crearDatosPaciente(XWPFDocument documento, HistoriaClinicaDocumento.Paciente paciente) {
        XWPFTable tarjeta = documento.createTable(4, 2);
        configurarTabla(tarjeta, new int[]{ANCHO_UTIL / 2, ANCHO_UTIL - ANCHO_UTIL / 2},
                0, 150, 230, 150, 230);
        sinBordes(tarjeta);
        unirFila(tarjeta.getRow(0), 0, 1, ANCHO_UTIL);
        celdaTitulo(tarjeta.getRow(0).getCell(0), "DATOS IDENTIFICATORIOS DEL PACIENTE");

        datoPaciente(tarjeta.getRow(1).getCell(0), "APELLIDO Y NOMBRE",
                paciente.apellido() + ", " + paciente.nombre());
        datoPaciente(tarjeta.getRow(1).getCell(1), "DNI", paciente.dni());
        datoPaciente(tarjeta.getRow(2).getCell(0), "FECHA DE NACIMIENTO", paciente.fechaNacimiento().toString());
        datoPaciente(tarjeta.getRow(2).getCell(1), "SEXO", paciente.sexo());
        unirFila(tarjeta.getRow(3), 0, 1, ANCHO_UTIL);
        datoPaciente(tarjeta.getRow(3).getCell(0), "TELÉFONO",
                paciente.telefono() == null || paciente.telefono().isBlank()
                        ? "No informado" : paciente.telefono());

        for (int fila = 1; fila < tarjeta.getRows().size(); fila++) {
            for (XWPFTableCell celda : tarjeta.getRow(fila).getTableCells()) sombrear(celda, VERDE_CLARO);
        }
        espaciador(documento, 30);
    }

    private void crearCronologia(XWPFDocument documento, HistoriaClinicaDocumento historia) {
        XWPFParagraph titulo = documento.createParagraph();
        titulo.setStyle("ClinicalHeading1");
        titulo.createRun().setText("Cronología clínica");
        XWPFParagraph descripcion = documento.createParagraph();
        descripcion.setStyle("ClinicalSubtitle");
        descripcion.setSpacingAfter(100);
        descripcion.createRun().setText("Registros presentados desde el más antiguo al más reciente");
        bordeInferior(descripcion, BORDE, 6, 5);

        if (historia.registros().isEmpty()) {
            crearAviso(documento, "No hay registros clínicos disponibles.");
            return;
        }

        for (var registro : historia.registros()) {
            XWPFTable tarjeta = documento.createTable(1, 1);
            configurarTabla(tarjeta, new int[]{ANCHO_UTIL}, 0, 150, 240, 160, 240);
            sinBordes(tarjeta);
            XWPFTableCell celda = tarjeta.getRow(0).getCell(0);
            sombrear(celda, FONDO_REGISTRO);
            bordeIzquierdo(celda, VERDE, 18);

            XWPFParagraph meta = reiniciarCelda(celda);
            meta.setStyle("ClinicalMeta");
            mantenerConSiguiente(meta);
            meta.createRun().setText(FECHA.format(registro.fecha()) + "  ·  " + registro.tipo());

            XWPFParagraph nombre = celda.addParagraph();
            nombre.setStyle("ClinicalHeading2");
            mantenerConSiguiente(nombre);
            nombre.createRun().setText(registro.titulo());

            if (registro.campos().isEmpty()) {
                XWPFParagraph vacio = celda.addParagraph();
                vacio.setStyle("ClinicalBody");
                vacio.createRun().setText("Sin datos adicionales.");
            }
            for (var campo : registro.campos()) {
                XWPFParagraph etiqueta = celda.addParagraph();
                etiqueta.setStyle("ClinicalLabel");
                mantenerConSiguiente(etiqueta);
                etiqueta.createRun().setText(campo.nombre().toUpperCase());
                XWPFParagraph valor = celda.addParagraph();
                valor.setStyle("ClinicalBody");
                valor.createRun().setText(campo.valor());
            }
            espaciador(documento, 55);
        }
    }

    private void crearAviso(XWPFDocument documento, String mensaje) {
        XWPFTable aviso = documento.createTable(1, 1);
        configurarTabla(aviso, new int[]{ANCHO_UTIL}, 0, 170, 220, 170, 220);
        sinBordes(aviso);
        XWPFTableCell celda = aviso.getRow(0).getCell(0);
        sombrear(celda, VERDE_CLARO);
        bordeIzquierdo(celda, VERDE, 14);
        XWPFParagraph p = reiniciarCelda(celda);
        p.setStyle("ClinicalBody");
        p.createRun().setText(mensaje);
    }

    private void datoPaciente(XWPFTableCell celda, String etiqueta, String valor) {
        XWPFParagraph pEtiqueta = reiniciarCelda(celda);
        pEtiqueta.setStyle("ClinicalLabel");
        mantenerConSiguiente(pEtiqueta);
        pEtiqueta.createRun().setText(etiqueta);
        XWPFParagraph pValor = celda.addParagraph();
        pValor.setStyle("ClinicalPatientValue");
        pValor.createRun().setText(valor);
    }

    private void configurarTabla(XWPFTable tabla, int[] anchos, int sangria,
            int margenSuperior, int margenInicial, int margenInferior, int margenFinal) {
        CTTblPr propiedades = tabla.getCTTbl().getTblPr();
        CTTblWidth ancho = propiedades.isSetTblW() ? propiedades.getTblW() : propiedades.addNewTblW();
        ancho.setType(STTblWidth.DXA);
        ancho.setW(BigInteger.valueOf(suma(anchos)));
        CTTblWidth indentacion = propiedades.isSetTblInd() ? propiedades.getTblInd() : propiedades.addNewTblInd();
        indentacion.setType(STTblWidth.DXA);
        indentacion.setW(BigInteger.valueOf(sangria));
        propiedades.addNewTblLayout().setType(STTblLayoutType.FIXED);
        CTTblCellMar margenes = propiedades.addNewTblCellMar();
        margenCelda(margenes.addNewTop(), margenSuperior);
        margenCelda(margenes.addNewStart(), margenInicial);
        margenCelda(margenes.addNewBottom(), margenInferior);
        margenCelda(margenes.addNewEnd(), margenFinal);

        CTTblGrid grilla = tabla.getCTTbl().addNewTblGrid();
        for (int anchoColumna : anchos) grilla.addNewGridCol().setW(BigInteger.valueOf(anchoColumna));
        for (XWPFTableRow fila : tabla.getRows()) {
            for (int i = 0; i < fila.getTableCells().size(); i++) {
                CTTcPr tcPr = propiedadesCelda(fila.getCell(i));
                CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
                tcW.setType(STTblWidth.DXA);
                tcW.setW(BigInteger.valueOf(anchos[Math.min(i, anchos.length - 1)]));
                if (!tcPr.isSetVAlign()) tcPr.addNewVAlign().setVal(STVerticalJc.CENTER);
            }
        }
    }

    private int suma(int[] valores) {
        int total = 0;
        for (int valor : valores) total += valor;
        return total;
    }

    private void unirFila(XWPFTableRow fila, int desde, int hasta, int anchoTotal) {
        CTTc celda = fila.getCell(desde).getCTTc();
        CTTcPr primero = celda.isSetTcPr() ? celda.getTcPr() : celda.addNewTcPr();
        CTTblWidth ancho = primero.isSetTcW() ? primero.getTcW() : primero.addNewTcW();
        ancho.setType(STTblWidth.DXA);
        ancho.setW(BigInteger.valueOf(anchoTotal));
        primero.addNewGridSpan().setVal(BigInteger.valueOf(hasta - desde + 1L));
        for (int i = hasta; i > desde; i--) fila.removeCell(i);
    }

    private void celdaTitulo(XWPFTableCell celda, String texto) {
        sombrear(celda, VERDE);
        XWPFParagraph p = reiniciarCelda(celda);
        p.setSpacingAfter(0);
        run(p, texto, 9, "FFFFFF", true);
    }

    private XWPFParagraph reiniciarCelda(XWPFTableCell celda) {
        XWPFParagraph primero = celda.getParagraphs().getFirst();
        while (!primero.getRuns().isEmpty()) primero.removeRun(0);
        primero.setSpacingBefore(0);
        primero.setSpacingAfter(0);
        return primero;
    }

    private void parrafoCabecera(XWPFTableCell celda, String texto, boolean negrita, String color) {
        XWPFParagraph p = reiniciarCelda(celda);
        p.setSpacingAfter(0);
        run(p, texto, 9, color, negrita);
    }

    private void run(XWPFParagraph parrafo, String texto, int puntos, String color, boolean negrita) {
        XWPFRun run = parrafo.createRun();
        run.setText(texto);
        run.setFontFamily(FUENTE);
        run.setFontSize(puntos);
        run.setColor(color);
        run.setBold(negrita);
    }

    private void campo(XWPFParagraph parrafo, String instruccion) {
        CTSimpleField campo = parrafo.getCTP().addNewFldSimple();
        campo.setInstr(instruccion);
        CTR run = campo.addNewR();
        configurarFuente(run.addNewRPr(), 16, GRIS, false);
        CTText texto = run.addNewT();
        texto.setStringValue("1");
    }

    private void configurarFuente(CTRPr propiedades, int mediosPuntos, String color, boolean negrita) {
        CTFonts fuentes = propiedades.addNewRFonts();
        fuentes.setAscii(FUENTE);
        fuentes.setHAnsi(FUENTE);
        fuentes.setEastAsia(FUENTE);
        propiedades.addNewSz().setVal(BigInteger.valueOf(mediosPuntos));
        propiedades.addNewSzCs().setVal(BigInteger.valueOf(mediosPuntos));
        propiedades.addNewColor().setVal(color);
        if (negrita) propiedades.addNewB();
    }

    private CTTcPr propiedadesCelda(XWPFTableCell celda) {
        return celda.getCTTc().isSetTcPr() ? celda.getCTTc().getTcPr() : celda.getCTTc().addNewTcPr();
    }

    private void sombrear(XWPFTableCell celda, String color) {
        CTTcPr tcPr = propiedadesCelda(celda);
        CTShd sombreado = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        sombreado.setFill(color);
    }

    private void sinBordes(XWPFTable tabla) {
        CTTblPr propiedades = tabla.getCTTbl().getTblPr();
        CTTblBorders bordes = propiedades.isSetTblBorders()
                ? propiedades.getTblBorders() : propiedades.addNewTblBorders();
        bordeNulo(bordes.addNewTop());
        bordeNulo(bordes.addNewStart());
        bordeNulo(bordes.addNewBottom());
        bordeNulo(bordes.addNewEnd());
        bordeNulo(bordes.addNewInsideH());
        bordeNulo(bordes.addNewInsideV());
        for (XWPFTableRow fila : tabla.getRows()) {
            for (XWPFTableCell celda : fila.getTableCells()) sinBordes(celda);
        }
    }

    private void sinBordes(XWPFTableCell celda) {
        CTTcPr tcPr = propiedadesCelda(celda);
        CTTcBorders bordes = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        bordeNulo(bordes.isSetTop() ? bordes.getTop() : bordes.addNewTop());
        bordeNulo(bordes.isSetStart() ? bordes.getStart() : bordes.addNewStart());
        bordeNulo(bordes.isSetBottom() ? bordes.getBottom() : bordes.addNewBottom());
        bordeNulo(bordes.isSetEnd() ? bordes.getEnd() : bordes.addNewEnd());
    }

    private void bordeNulo(CTBorder borde) {
        borde.setVal(STBorder.NIL);
        borde.setSz(BigInteger.ZERO);
    }

    private void bordeIzquierdo(XWPFTableCell celda, String color, int grosor) {
        CTTcPr tcPr = propiedadesCelda(celda);
        CTTcBorders bordes = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        CTBorder borde = bordes.isSetStart() ? bordes.getStart() : bordes.addNewStart();
        configurarBorde(borde, color, grosor, 0);
    }

    private void bordeSuperior(XWPFTableCell celda, String color, int grosor) {
        CTTcPr tcPr = propiedadesCelda(celda);
        CTTcBorders bordes = tcPr.isSetTcBorders() ? tcPr.getTcBorders() : tcPr.addNewTcBorders();
        CTBorder borde = bordes.isSetTop() ? bordes.getTop() : bordes.addNewTop();
        configurarBorde(borde, color, grosor, 0);
    }

    private void bordeInferior(XWPFParagraph parrafo, String color, int grosor, int espacio) {
        CTPPr ppr = parrafo.getCTP().isSetPPr() ? parrafo.getCTP().getPPr() : parrafo.getCTP().addNewPPr();
        var bordes = ppr.isSetPBdr() ? ppr.getPBdr() : ppr.addNewPBdr();
        CTBorder borde = bordes.isSetBottom() ? bordes.getBottom() : bordes.addNewBottom();
        configurarBorde(borde, color, grosor, espacio);
    }

    private void configurarBorde(CTBorder borde, String color, int grosor, int espacio) {
        borde.setVal(STBorder.SINGLE);
        borde.setColor(color);
        borde.setSz(BigInteger.valueOf(grosor));
        borde.setSpace(BigInteger.valueOf(espacio));
    }

    private void margenCelda(CTTblWidth margen, int valor) {
        margen.setType(STTblWidth.DXA);
        margen.setW(BigInteger.valueOf(valor));
    }

    private void mantenerConSiguiente(XWPFParagraph parrafo) {
        CTPPr ppr = parrafo.getCTP().isSetPPr() ? parrafo.getCTP().getPPr() : parrafo.getCTP().addNewPPr();
        if (!ppr.isSetKeepNext()) ppr.addNewKeepNext();
        if (!ppr.isSetKeepLines()) ppr.addNewKeepLines();
    }

    private void compactarParrafoVacio(XWPFParagraph parrafo) {
        parrafo.setSpacingBefore(0);
        parrafo.setSpacingAfter(0);
        XWPFRun run = parrafo.createRun();
        run.setText("");
        run.setFontSize(1);
    }

    private void espaciador(XWPFDocument documento, int despues) {
        XWPFParagraph espacio = documento.createParagraph();
        espacio.setSpacingBefore(0);
        espacio.setSpacingAfter(despues);
        XWPFRun run = espacio.createRun();
        run.setText("");
        run.setFontSize(1);
    }
}
