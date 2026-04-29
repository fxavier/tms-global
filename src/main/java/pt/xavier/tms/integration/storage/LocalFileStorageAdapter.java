package pt.xavier.tms.integration.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import pt.xavier.tms.integration.config.FileStorageConfig;
import pt.xavier.tms.integration.dto.FileUploadResultDto;
import pt.xavier.tms.integration.port.FileStoragePort;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Component
@ConditionalOnProperty(name = "tms.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageAdapter implements FileStoragePort {

    private final FileStorageConfig config;

    public LocalFileStorageAdapter(FileStorageConfig config) {
        this.config = config;
    }

    @Override
    public FileUploadResultDto upload(MultipartFile file) {
        FileValidation.validate(file, config.maxFileSizeBytes());

        UUID fileId = UUID.randomUUID();
        String storageKey = fileId + extensionFrom(file.getOriginalFilename());
        Path basePath = config.local().basePath().toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(storageKey).normalize();

        try {
            Files.createDirectories(basePath);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
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
        Path basePath = config.local().basePath().toAbsolutePath().normalize();
        Path filePath = basePath.resolve(storageKey).normalize();
        if (!filePath.startsWith(basePath)) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND", "File not found");
        }

        FileSystemResource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new ResourceNotFoundException("FILE_NOT_FOUND", "File not found");
        }
        return resource;
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
