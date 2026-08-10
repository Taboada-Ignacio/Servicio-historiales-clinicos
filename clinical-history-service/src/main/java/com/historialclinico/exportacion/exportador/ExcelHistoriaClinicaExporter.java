package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class ExcelHistoriaClinicaExporter implements HistoriaClinicaExporter {
    @Override public FormatoExportacion formato() { return FormatoExportacion.XLSX; }

    @Override
    public byte[] exportar(HistoriaClinicaDocumento documento) {
        try (var libro = new XSSFWorkbook(); var salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Historia clínica");
            CellStyle cabecera = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            cabecera.setFont(fuente);
            cabecera.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row encabezado = hoja.createRow(0);
            String[] titulos = {"Sección", "Fecha", "Registro", "Campo", "Valor"};
            for (int i = 0; i < titulos.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(titulos[i]);
                celda.setCellStyle(cabecera);
            }
            int nroFila = 1;
            for (var fila : FilasHistoriaClinica.crear(documento)) {
                Row row = hoja.createRow(nroFila++);
                row.createCell(0).setCellValue(fila.seccion());
                row.createCell(1).setCellValue(fila.fecha());
                row.createCell(2).setCellValue(fila.registro());
                row.createCell(3).setCellValue(fila.campo());
                row.createCell(4).setCellValue(fila.valor());
            }
            hoja.createFreezePane(0, 1);
            hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, nroFila - 1), 0, 4));
            int[] anchos = {18, 18, 34, 30, 70};
            for (int i = 0; i < anchos.length; i++) hoja.setColumnWidth(i, anchos[i] * 256);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible generar la historia clínica en XLSX", ex);
        }
    }
}
