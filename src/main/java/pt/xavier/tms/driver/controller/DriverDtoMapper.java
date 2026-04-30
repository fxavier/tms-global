package pt.xavier.tms.driver.controller;

import pt.xavier.tms.driver.dto.DriverDocumentResponseDto;
import pt.xavier.tms.driver.dto.DriverResponseDto;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.entity.DriverDocument;

final class DriverDtoMapper {

    private DriverDtoMapper() {
    }

    static DriverResponseDto toDriverResponse(Driver driver) {
        return new DriverResponseDto(
                driver.getId(),
                driver.getFullName(),
                driver.getPhone(),
                driver.getAddress(),
                driver.getIdNumber(),
                driver.getLicenseNumber(),
                driver.getLicenseCategory(),
                driver.getLicenseIssueDate(),
                driver.getLicenseExpiryDate(),
                driver.getActivityLocation(),
                driver.getEmployee() != null ? driver.getEmployee().getId() : null,
                driver.getStatus(),
                driver.getNotes()
        );
    }

    static DriverDocumentResponseDto toDocumentResponse(DriverDocument document) {
        return new DriverDocumentResponseDto(
                document.getId(),
                document.getDriver().getId(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getIssuingEntity(),
                document.getCategory(),
                document.getStatus(),
                document.getNotes(),
                document.getFileId()
        );
    }
}
