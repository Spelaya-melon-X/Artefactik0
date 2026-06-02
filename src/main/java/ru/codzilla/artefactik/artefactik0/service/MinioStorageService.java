package ru.codzilla.artefactik.artefactik0.service;

import io.minio.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.codzilla.artefactik.artefactik0.dto.MinioProperties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties props;


    public static String generatorKey(Long problemId) {
        return "problems/" + problemId + "/generator/Generator.java";
    }

    @PostConstruct
    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(props.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(props.getBucket()).build()
                );
                log.info("MinIO bucket '{}' created", props.getBucket());
            } else {
                log.info("MinIO bucket '{}' already exists", props.getBucket());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to init MinIO bucket: " + props.getBucket(), e);
        }
    }

    public void save(String key, String content) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(key)
                            .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                            .contentType("text/plain")
                            .build()
            );
            log.debug("Saved → MinIO key: {}", key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save to MinIO: " + key, e);
        }
    }

    public String get(String key) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(props.getBucket())
                        .object(key)
                        .build()
        )) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get from MinIO: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(props.getBucket())
                            .object(key)
                            .build()
            );
            log.debug("Deleted from MinIO: {}", key);
        } catch (Exception e) {
            log.warn("Failed to delete from MinIO key {}: {}", key, e.getMessage());
        }
    }

    public static String inputKey(Long problemId, int testIndex) {
        return "problems/" + problemId + "/tests/" + testIndex + "/input.txt";
    }

    public static String outputKey(Long problemId, int testIndex) {
        return "problems/" + problemId + "/tests/" + testIndex + "/output.txt";
    }

    public static String statementKey(Long problemId) {
        return "problems/" + problemId + "/statement.md";
    }
}
