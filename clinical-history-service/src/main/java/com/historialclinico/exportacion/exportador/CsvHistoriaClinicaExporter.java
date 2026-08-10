package com.historialclinico.exportacion.exportador;

import com.historialclinico.exportacion.dto.HistoriaClinicaDocumento;
import com.historialclinico.exportacion.modelo.FormatoExportacion;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CsvHistoriaClinicaExporter implements HistoriaClinicaExporter {
    @Override public FormatoExportacion formato() { return FormatoExportacion.CSV; }

    @Override
    public byte[] exportar(HistoriaClinicaDocumento documento) {
        StringBuilder csv = new StringBuilder("\uFEFFSección,Fecha,Registro,Campo,Valor\r\n");
        FilasHistoriaClinica.crear(documento).forEach(fila -> csv
                .append(escapar(fila.seccion())).append(',')
                .append(escapar(fila.fecha())).append(',')
                .append(escapar(fila.registro())).append(',')
                .append(escapar(fila.campo())).append(',')
                .append(escapar(fila.valor())).append("\r\n"));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapar(String valor) {
        String seguro = valor == null ? "" : valor;
        if (!seguro.isEmpty() && "=+-@".indexOf(seguro.charAt(0)) >= 0) seguro = "'" + seguro;
        return '"' + seguro.replace("\"", "\"\"") + '"';
    }
}
