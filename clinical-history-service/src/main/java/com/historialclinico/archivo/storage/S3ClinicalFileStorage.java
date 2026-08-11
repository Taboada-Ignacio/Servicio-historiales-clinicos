package com.historialclinico.archivo.storage;

import com.historialclinico.excepcion.ExcepcionServicioArchivosNoDisponible;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.io.InputStream;

@Component
public class S3ClinicalFileStorage implements ClinicalFileStorage {
    private final S3Client cliente;
    private final String bucket;
    private final AtomicBoolean bucketVerificado = new AtomicBoolean();

    public S3ClinicalFileStorage(S3Client cliente, @Value("${app.archivos.storage.bucket:clinical-files}") String bucket) {
        this.cliente = cliente;
        this.bucket = bucket;
    }

    @Override
    public void almacenar(String storageKey, byte[] contenido, String mimeType) {
        try {
            asegurarBucket();
            cliente.putObject(PutObjectRequest.builder().bucket(bucket).key(storageKey)
                    .contentType(mimeType).contentLength((long) contenido.length).build(), RequestBody.fromBytes(contenido));
        } catch (RuntimeException ex) {
            throw traducir("No fue posible almacenar el archivo clínico", ex);
        }
    }

    @Override
    public byte[] leer(String storageKey) {
        try {
            return cliente.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(storageKey).build()).asByteArray();
        } catch (RuntimeException ex) {
            throw traducir("No fue posible recuperar el archivo clínico", ex);
        }
    }

    @Override
    public InputStream abrirLectura(String storageKey) {
        try {
            return cliente.getObject(GetObjectRequest.builder().bucket(bucket).key(storageKey).build());
        } catch (RuntimeException ex) {
            throw traducir("No fue posible recuperar el archivo clínico", ex);
        }
    }

    @Override
    public boolean existe(String storageKey) {
        try {
            cliente.headObject(HeadObjectRequest.builder().bucket(bucket).key(storageKey).build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) return false;
            throw traducir("No fue posible verificar el archivo clínico", ex);
        } catch (RuntimeException ex) {
            throw traducir("No fue posible verificar el archivo clínico", ex);
        }
    }

    @Override
    public void eliminar(String storageKey) {
        try {
            cliente.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        } catch (RuntimeException ex) {
            throw traducir("No fue posible compensar el archivo almacenado", ex);
        }
    }

    private void asegurarBucket() {
        if (bucketVerificado.get()) return;
        synchronized (bucketVerificado) {
            if (bucketVerificado.get()) return;
            try {
                cliente.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (S3Exception ex) {
                if (ex.statusCode() != 404) throw ex;
                try { cliente.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); }
                catch (S3Exception creacion) {
                    if (creacion.statusCode() != 409) throw creacion;
                }
            }
            bucketVerificado.set(true);
        }
    }

    private ExcepcionServicioArchivosNoDisponible traducir(String mensaje, RuntimeException causa) {
        if (causa instanceof ExcepcionServicioArchivosNoDisponible propia) return propia;
        return new ExcepcionServicioArchivosNoDisponible(mensaje, causa);
    }
}
