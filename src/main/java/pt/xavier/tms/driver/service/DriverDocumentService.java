package pt.xavier.tms.driver.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.driver.dto.DriverDocumentDto;
import pt.xavier.tms.driver.entity.Driver;
import pt.xavier.tms.driver.entity.DriverDocument;
import pt.xavier.tms.driver.repository.DriverDocumentRepository;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.enums.DriverDocumentType;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@Service
@ConditionalOnProperty(name = "tms.driver.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class DriverDocumentService {

    private static final String DRIVER_DOCUMENT_ENTITY_TYPE = "DRIVER_DOCUMENT";

    private final DriverService driverService;
    private final DriverDocumentRepository driverDocumentRepository;

    public DriverDocumentService(DriverService driverService, DriverDocumentRepository driverDocumentRepository) {
        this.driverService = driverService;
        this.driverDocumentRepository = driverDocumentRepository;
    }

    @Auditable(entityType = DRIVER_DOCUMENT_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public DriverDocument addDocument(UUID driverId, DriverDocumentDto dto) {
        Driver driver = driverService.getDriver(driverId);

        DriverDocument document = new DriverDocument();
        document.setDriver(driver);
        applyDto(document, dto);
        driver.getDocuments().add(document);
        refreshExpiryStatus(document);
        return driverDocumentRepository.save(document);
    }

    @Auditable(entityType = DRIVER_DOCUMENT_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public DriverDocument updateDocument(UUID driverId, UUID documentId, DriverDocumentDto dto) {
        DriverDocument document = getDocument(driverId, documentId);
        applyDto(document, dto);
        refreshExpiryStatus(document);
        return document;
    }

    public List<DriverDocument> listDocuments(UUID driverId) {
        driverService.getDriver(driverId);
        List<DriverDocument> documents = driverDocumentRepository.findByDriverId(driverId);
        documents.forEach(this::refreshExpiryStatus);
        return documents;
    }

    private DriverDocument getDocument(UUID driverId, UUID documentId) {
        DriverDocument document = driverDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("DRIVER_DOCUMENT_NOT_FOUND", "Driver document not found"));
        if (!document.getDriver().getId().equals(driverId)) {
            throw new ResourceNotFoundException("DRIVER_DOCUMENT_NOT_FOUND", "Driver document not found");
        }
        return document;
    }

    private static void applyDto(DriverDocument document, DriverDocumentDto dto) {
        document.setDocumentType(dto.documentType());
        document.setDocumentNumber(dto.documentNumber());
        document.setIssueDate(dto.issueDate());
        document.setExpiryDate(dto.expiryDate());
        document.setIssuingEntity(dto.issuingEntity());
        document.setCategory(dto.category());
        document.setStatus(dto.status() == null ? DocumentStatus.VALIDO : dto.status());
        document.setNotes(dto.notes());
        document.setFileId(dto.fileId());
    }

    private void refreshExpiryStatus(DriverDocument document) {
        if (document.getDocumentType() == DriverDocumentType.CARTA_CONDUCAO
                && document.getExpiryDate() != null
                && document.getExpiryDate().isBefore(LocalDate.now())) {
            document.setStatus(DocumentStatus.EXPIRADO);
        }
    }
}
