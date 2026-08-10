package com.historialclinico.exportacion.modelo;

public enum FormatoExportacion {
    PDF("application/pdf", "pdf"),
    CSV("text/csv; charset=UTF-8", "csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx");

    private final String tipoContenido;
    private final String extension;

    FormatoExportacion(String tipoContenido, String extension) {
        this.tipoContenido = tipoContenido;
        this.extension = extension;
    }

    public String getTipoContenido() { return tipoContenido; }
    public String getExtension() { return extension; }
}
