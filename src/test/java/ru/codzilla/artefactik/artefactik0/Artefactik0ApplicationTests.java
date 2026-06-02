package ru.codzilla.artefactik.artefactik0;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import ru.codzilla.artefactik.artefactik0.service.MinioStorageService;


@SpringBootTest
@ActiveProfiles("test")
class Artefactik0ApplicationTests {

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private MinioStorageService minioStorageService;   // ← добавляем

    @Test
    void contextLoads() {
    }
}