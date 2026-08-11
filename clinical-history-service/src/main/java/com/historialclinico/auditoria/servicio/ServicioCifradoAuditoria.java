package com.historialclinico.auditoria.servicio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class ServicioCifradoAuditoria {
    private static final int LONGITUD_IV = 12;
    private final SecretKeySpec claveCifrado;
    private final SecretKeySpec claveIntegridad;
    private final SecureRandom aleatorio = new SecureRandom();

    public ServicioCifradoAuditoria(@Value("${app.auditoria.clave-base64}") String claveBase64) {
        byte[] clave;
        try { clave = Base64.getDecoder().decode(claveBase64); }
        catch (IllegalArgumentException ex) { throw new IllegalStateException("AUDIT_ENCRYPTION_KEY debe estar en Base64", ex); }
        if (clave.length != 32) throw new IllegalStateException("AUDIT_ENCRYPTION_KEY debe contener exactamente 32 bytes");
        this.claveCifrado = new SecretKeySpec(clave, "AES");
        this.claveIntegridad = new SecretKeySpec(sha256Bytes(concatenar(clave, "integridad-auditoria".getBytes(StandardCharsets.UTF_8))), "HmacSHA256");
    }

    public ContenidoCifrado cifrar(String texto) {
        try {
            byte[] iv = new byte[LONGITUD_IV]; aleatorio.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, claveCifrado, new GCMParameterSpec(128, iv));
            return new ContenidoCifrado(cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8)), iv);
        } catch (Exception ex) { throw new IllegalStateException("No fue posible cifrar la auditoría", ex); }
    }

    public String descifrar(byte[] contenido, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, claveCifrado, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(contenido), StandardCharsets.UTF_8);
        } catch (Exception ex) { throw new IllegalStateException("La auditoría no superó la validación de integridad", ex); }
    }

    public String hash(String texto) { return HexFormat.of().formatHex(sha256Bytes(texto.getBytes(StandardCharsets.UTF_8))); }
    public String hash(byte[] contenido) { return HexFormat.of().formatHex(sha256Bytes(contenido)); }
    public String hash(InputStream contenido) throws IOException {
        return copiarYHash(contenido, OutputStream.nullOutputStream());
    }
    public String copiarYHash(InputStream entrada, OutputStream salida) throws IOException {
        MessageDigest digest = sha256();
        byte[] buffer = new byte[16 * 1024];
        int leidos;
        while ((leidos = entrada.read(buffer)) != -1) {
            digest.update(buffer, 0, leidos);
            salida.write(buffer, 0, leidos);
        }
        return HexFormat.of().formatHex(digest.digest());
    }
    public String firmaCadena(String texto) {
        try { Mac mac = Mac.getInstance("HmacSHA256"); mac.init(claveIntegridad); return HexFormat.of().formatHex(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("No fue posible firmar la cadena de auditoría", ex); }
    }
    private static byte[] sha256Bytes(byte[] valor) {
        return sha256().digest(valor);
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
    private static byte[] concatenar(byte[] a, byte[] b) { byte[] r = new byte[a.length + b.length]; System.arraycopy(a, 0, r, 0, a.length); System.arraycopy(b, 0, r, a.length, b.length); return r; }
    public record ContenidoCifrado(byte[] contenido, byte[] iv) {}
}
