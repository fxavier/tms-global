package pt.xavier.tms.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

public record FileResponseDto(
        UUID id,
        String originalFilename,
        String storageKey,
        String contentType,
        Long sizeBytes,
        String uploadedBy,
        Instant uploadedAt
) {
}
