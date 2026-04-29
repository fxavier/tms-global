package pt.xavier.tms.integration.dto;

import java.util.UUID;

public record FileUploadResultDto(
        UUID id,
        String originalFilename,
        String storageKey,
        String contentType,
        long sizeBytes
) {
}
