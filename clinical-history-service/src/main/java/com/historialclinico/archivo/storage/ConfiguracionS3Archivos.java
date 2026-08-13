package com.historialclinico.archivo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

@Configuration
public class ConfiguracionS3Archivos {
    @Bean
    S3Client clinicalFilesS3Client(@Value("${app.archivos.storage.region:us-east-1}") String region,
            @Value("${app.archivos.storage.endpoint:}") String endpoint,
            @Value("${app.archivos.storage.access-key}") String accessKey,
            @Value("${app.archivos.storage.secret-key}") String secretKey,
            @Value("${app.archivos.storage.path-style:true}") boolean pathStyle) {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
        if (endpoint != null && !endpoint.isBlank()) builder.endpointOverride(URI.create(endpoint));
        return builder.build();
    }
}
