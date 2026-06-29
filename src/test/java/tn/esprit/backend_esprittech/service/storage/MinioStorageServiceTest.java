package tn.esprit.backend_esprittech.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.backend_esprittech.config.MinioProperties;
import tn.esprit.backend_esprittech.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioStorageService service;

    @BeforeEach
    void setUp() {
        MinioProperties properties = new MinioProperties(
                "http://localhost:9000",
                "minio",
                "minio123",
                "test-bucket",
                false
        );
        service = new MinioStorageService(minioClient, properties);
    }

    @Test
    void ensureBucketExists_existingBucket_shouldNotCreateBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        service.ensureBucketExists();

        verify(minioClient).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExists_missingBucket_shouldCreateBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        service.ensureBucketExists();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExists_minioFailure_shouldWrapBusinessException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("offline"));

        BusinessException ex = assertThrows(BusinessException.class, service::ensureBucketExists);

        assertEquals("Impossible d'initialiser le bucket MinIO: offline", ex.getMessage());
    }

    @Test
    void initializeBucketAtStartup_minioFailure_shouldOnlyLogWarning() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("offline"));

        assertDoesNotThrow(() -> service.initializeBucketAtStartup());
    }

    @Test
    void upload_validFile_shouldEnsureBucketPutObjectAndReturnStoredMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.pdf",
                "application/pdf",
                "abc".getBytes()
        );
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        MinioStorageService.StoredObject stored = service.upload("livrables/42/rapport.pdf", file);

        assertEquals("livrables/42/rapport.pdf", stored.objectName());
        assertEquals("application/pdf", stored.contentType());
        assertEquals(3L, stored.size());
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_putObjectFailure_shouldWrapBusinessException() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "rapport.pdf",
                "application/pdf",
                "abc".getBytes()
        );
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new RuntimeException("disk full")).when(minioClient).putObject(any(PutObjectArgs.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.upload("livrables/42/rapport.pdf", file));

        assertEquals("Echec de l'upload MinIO: disk full", ex.getMessage());
    }

    @Test
    void download_existingObject_shouldReturnStreamAndMetadata() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        GetObjectResponse objectResponse = mock(GetObjectResponse.class);
        when(stat.contentType()).thenReturn("application/pdf");
        when(stat.size()).thenReturn(128L);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(objectResponse);

        MinioStorageService.DownloadedObject downloaded = service.download("livrables/42/rapport.pdf");

        assertSame(objectResponse, downloaded.inputStream());
        assertEquals("application/pdf", downloaded.contentType());
        assertEquals(128L, downloaded.size());
    }

    @Test
    void download_missingObject_shouldWrapBusinessException() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("not found"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.download("livrables/42/missing.pdf"));

        assertEquals("Impossible de telecharger le fichier MinIO: not found", ex.getMessage());
    }

    @Test
    void delete_blankObjectName_shouldIgnoreRequest() throws Exception {
        service.delete("  ");

        verifyNoInteractions(minioClient);
    }

    @Test
    void delete_existingObject_shouldRemoveObject() throws Exception {
        service.delete("livrables/42/rapport.pdf");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    void delete_minioFailure_shouldWrapBusinessException() throws Exception {
        doThrow(new RuntimeException("permission denied")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.delete("livrables/42/rapport.pdf"));

        assertEquals("Impossible de supprimer l'objet MinIO: permission denied", ex.getMessage());
    }
}
