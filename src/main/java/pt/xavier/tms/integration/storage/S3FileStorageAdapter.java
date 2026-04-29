package pt.xavier.tms.integration.storage;

import java.io.IOException;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.integration.config.FileStorageConfig;
import pt.xavier.tms.integration.dto.FileUploadResultDto;
import pt.xavier.tms.integration.port.FileStoragePort;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@ConditionalOnProperty(name = "tms.storage.type", havingValue = "s3")
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final FileStorageConfig config;

    public S3FileStorageAdapter(S3Client s3Client, FileStorageConfig config) {
        this.s3Client = s3Client;
        this.config = config;
    }

    @Override
    public FileUploadResultDto upload(MultipartFile file) {
        FileValidation.validate(file, config.maxFileSizeBytes());
        validateBucket();

        UUID fileId = UUID.randomUUID();
        String storageKey = storageKey(fileId, file.getOriginalFilename());
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(config.s3().bucket())
                .key(storageKey)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException | S3Exception ex) {
            throw new BusinessException("FILE_STORAGE_ERROR", "Could not store file");
        }

        return new FileUploadResultDto(
                fileId,
                file.getOriginalFilename(),
                storageKey,
                file.getContentType(),
                file.getSize()
        );
    }

    @Override
    public Resource download(String storageKey) {
        validateBucket();
        try {
            ResponseInputStream<GetObjectResponse> object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(config.s3().bucket())
                    .key(storageKey)
                    .build());
            return new InputStreamResource(object);
        } catch (NoSuchKeyException ex) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND", "File not found");
        } catch (S3Exception ex) {
            throw new BusinessException("FILE_STORAGE_ERROR", "Could not download file");
        }
    }

    private String storageKey(UUID fileId, String originalFilename) {
        String prefix = config.s3().prefix();
        String normalizedPrefix = prefix.isBlank() || prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + fileId + extensionFrom(originalFilename);
    }

    private void validateBucket() {
        if (config.s3().bucket() == null || config.s3().bucket().isBlank()) {
            throw new BusinessException("S3_BUCKET_NOT_CONFIGURED", "S3 bucket is not configured");
        }
    }

    private static String extensionFrom(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int extensionIndex = filename.lastIndexOf('.');
        if (extensionIndex < 0 || extensionIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(extensionIndex).toLowerCase();
    }
}
