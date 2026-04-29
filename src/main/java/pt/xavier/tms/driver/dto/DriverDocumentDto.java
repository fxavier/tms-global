package pt.xavier.tms.driver.dto;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;

public record DriverDocumentDto(
        @NotNull DriverDocumentType documentType,
        @Size(max = 100) String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        @Size(max = 200) String issuingEntity,
        @Size(max = 30) String category,
        DocumentStatus status,
        String notes,
        UUID fileId
) {
}
