package com.example.kyc_service.storage;

import com.example.kyc_service.exception.StorageException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 15;

    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "image/jpeg",     ".jpg",
            "image/jpg",      ".jpg",
            "image/png",      ".png",
            "application/pdf", ".pdf"
    );

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Bucket '{}' created.", bucket);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket '" + bucket + "'", e);
        }
    }

    /**
     * Uploads the file to MinIO and returns the storage key.
     * The key format is: submissions/{userId}/{uuid}{extension}
     * The original filename is intentionally ignored to prevent path traversal attacks.
     */
    public String upload(MultipartFile file, Long userId) {
        String extension = resolveExtension(file.getContentType());
        String fileKey = "submissions/" + userId + "/" + UUID.randomUUID() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Upload complete. key={}", fileKey);
            return fileKey;
        } catch (Exception e) {
            throw new StorageException("Upload failed for userId=" + userId, e);
        }
    }

    /**
     * Generates a pre-signed URL for secure temporary access.
     * Expires in 15 minutes.
     */
    public String generatePresignedUrl(String fileKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileKey)
                            .expiry(PRESIGNED_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to generate pre-signed URL for key=" + fileKey, e);
        }
    }

    /**
     * Returns an InputStream for the stored file.
     * The caller is responsible for closing it — always use try-with-resources.
     */
    public InputStream download(String fileKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException("Failed to download file key=" + fileKey, e);
        }
    }

    public void delete(String fileKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            );
            log.info("File deleted. key={}", fileKey);
        } catch (Exception e) {
            log.warn("Could not delete file from MinIO. key={}: {}", fileKey, e.getMessage());
        }
    }

    private String resolveExtension(String contentType) {
        if (contentType == null) return "";
        return MIME_TO_EXTENSION.getOrDefault(contentType.toLowerCase(), "");
    }
}