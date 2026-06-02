package ru.codzilla.artefactik.artefactik0.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.codzilla.artefactik.artefactik0.dto.MinioProperties;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(MinioProperties.class)
@Profile("!test")
public class MinioConfig {

    private final MinioProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.getUrl())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
