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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.backend_esprittech.config.MinioProperties;
import tn.esprit.backend_esprittech.exception.BusinessException;

import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBucketAtStartup() {
        try {
            ensureBucketExists();
        } catch (Exception ex) {
            log.warn("MinIO bucket initialization skipped: {}", ex.getMessage());
        }
    }

    public void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.bucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
            }
        } catch (Exception ex) {
            throw new BusinessException("Impossible d'initialiser le bucket MinIO: " + ex.getMessage());
        }
    }

    public StoredObject upload(String objectName, MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return new StoredObject(objectName, file.getContentType(), file.getSize());
        } catch (Exception ex) {
            throw new BusinessException("Echec de l'upload MinIO: " + ex.getMessage());
        }
    }

    public DownloadedObject download(String objectName) {
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .build()
            );
            GetObjectResponse inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .build()
            );
            return new DownloadedObject(inputStream, stat.contentType(), stat.size());
        } catch (Exception ex) {
            throw new BusinessException("Impossible de telecharger le fichier MinIO: " + ex.getMessage());
        }
    }

    public void delete(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.bucket())
                            .object(objectName)
                            .build()
            );
        } catch (Exception ex) {
            throw new BusinessException("Impossible de supprimer l'objet MinIO: " + ex.getMessage());
        }
    }

    public record StoredObject(String objectName, String contentType, long size) {
    }

    public record DownloadedObject(InputStream inputStream, String contentType, long size) {
    }
}
