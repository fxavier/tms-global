package pt.xavier.tms.vehicle.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.VehicleDocumentType;

public record VehicleDocumentDto(
        @NotNull VehicleDocumentType documentType,
        String documentNumber,
        LocalDate issueDate,
        LocalDate expiryDate,
        String issuingEntity,
        DocumentStatus status,
        String notes,
        @PositiveOrZero Long fileSizeBytes
) {
}
