package pt.xavier.tms.driver.dto;

import java.time.LocalDate;
import java.util.UUID;

import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;

public record DriverDocumentResponseDto(
        UUID id,
        UUID driverId,
        DriverDocumentType documentType,
        String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        String issuingEntity,
        String category,
        DocumentStatus status,
        String notes,
        UUID fileId
) {
}
