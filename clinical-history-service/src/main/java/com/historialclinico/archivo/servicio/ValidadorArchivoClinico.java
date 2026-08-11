package com.historialclinico.archivo.servicio;

import com.historialclinico.excepcion.ExcepcionArchivoInvalido;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ValidadorArchivoClinico {
    static final long MAXIMO_GENERAL = 20L * 1024 * 1024;
    static final long MAXIMO_IMAGEN = 15L * 1024 * 1024;
    private static final long MAXIMO_DOCX_DESCOMPRIMIDO = 100L * 1024 * 1024;
    private static final Set<String> EXTENSIONES = Set.of("pdf", "docx", "jpg", "jpeg", "png");

    public ArchivoClinicoValidado validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new ExcepcionArchivoInvalido("Debe adjuntar un archivo no vacío");
        String nombre = sanitizarNombre(archivo.getOriginalFilename());
        String extension = extension(nombre);
        if (!EXTENSIONES.contains(extension))
            throw new ExcepcionArchivoInvalido("La extensión del archivo no está permitida");
        long limite = esImagen(extension) ? MAXIMO_IMAGEN : MAXIMO_GENERAL;
        if (archivo.getSize() > limite)
            throw new ExcepcionArchivoInvalido("El archivo supera el máximo permitido de "
                    + (limite / 1024 / 1024) + " MB para su formato");
        byte[] contenido;
        try { contenido = archivo.getBytes(); }
        catch (IOException ex) { throw new ExcepcionArchivoInvalido("No fue posible leer el archivo", ex); }
        if (contenido.length == 0 || contenido.length > limite)
            throw new ExcepcionArchivoInvalido("El tamaño real del archivo no es válido");
        String mime = detectarMime(extension, contenido);
        return new ArchivoClinicoValidado(contenido, nombre, extension, mime, contenido.length);
    }

    String sanitizarNombre(String original) {
        if (original == null || original.isBlank()) throw new ExcepcionArchivoInvalido("El archivo debe tener nombre");
        String normalizado = Normalizer.normalize(original.trim(), Normalizer.Form.NFKC);
        if (normalizado.contains("/") || normalizado.contains("\\") || normalizado.contains("..")
                || normalizado.chars().anyMatch(c -> c == 0 || Character.isISOControl(c)))
            throw new ExcepcionArchivoInvalido("El nombre del archivo contiene una ruta o caracteres peligrosos");
        String seguro = normalizado.replaceAll("[^\\p{L}\\p{N}._() -]", "_")
                .replaceAll("\\s+", " ");
        if (seguro.length() > 255) seguro = seguro.substring(seguro.length() - 255);
        if (seguro.isBlank() || seguro.startsWith("."))
            throw new ExcepcionArchivoInvalido("El nombre del archivo no es válido");
        return seguro;
    }

    private String extension(String nombre) {
        int punto = nombre.lastIndexOf('.');
        if (punto <= 0 || punto == nombre.length() - 1)
            throw new ExcepcionArchivoInvalido("El archivo debe tener una extensión permitida");
        return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    private boolean esImagen(String extension) {
        return extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png");
    }

    private String detectarMime(String extension, byte[] contenido) {
        return switch (extension) {
            case "pdf" -> {
                if (!empiezaCon(contenido, new byte[]{'%', 'P', 'D', 'F', '-'})) contenidoFalso();
                yield "application/pdf";
            }
            case "png" -> {
                if (!empiezaCon(contenido, new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}))
                    contenidoFalso();
                yield "image/png";
            }
            case "jpg", "jpeg" -> {
                if (contenido.length < 4 || (contenido[0] & 0xff) != 0xff || (contenido[1] & 0xff) != 0xd8
                        || (contenido[2] & 0xff) != 0xff || (contenido[contenido.length - 2] & 0xff) != 0xff
                        || (contenido[contenido.length - 1] & 0xff) != 0xd9) contenidoFalso();
                yield "image/jpeg";
            }
            case "docx" -> {
                if (!esDocx(contenido)) contenidoFalso();
                yield "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
            default -> throw new ExcepcionArchivoInvalido("La extensión del archivo no está permitida");
        };
    }

    private boolean esDocx(byte[] contenido) {
        if (!empiezaCon(contenido, new byte[]{'P', 'K'})) return false;
        boolean contentTypes = false;
        boolean documentoWord = false;
        int entradas = 0;
        long descomprimido = 0;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entrada;
            byte[] buffer = new byte[8192];
            while ((entrada = zip.getNextEntry()) != null) {
                if (++entradas > 10_000) return false;
                String nombre = entrada.getName();
                if (nombre.equals("[Content_Types].xml")) contentTypes = true;
                if (nombre.equals("word/document.xml")) documentoWord = true;
                int leidos;
                while ((leidos = zip.read(buffer)) != -1) {
                    descomprimido += leidos;
                    if (descomprimido > MAXIMO_DOCX_DESCOMPRIMIDO) return false;
                }
            }
            return contentTypes && documentoWord;
        } catch (IOException ex) {
            return false;
        }
    }

    private boolean empiezaCon(byte[] contenido, byte[] firma) {
        if (contenido.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) if (contenido[i] != firma[i]) return false;
        return true;
    }

    private void contenidoFalso() {
        throw new ExcepcionArchivoInvalido("El contenido real no coincide con el formato declarado");
    }
}
