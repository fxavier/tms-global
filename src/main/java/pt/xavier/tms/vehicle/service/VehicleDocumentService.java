package pt.xavier.tms.vehicle.service;

import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.enums.DocumentStatus;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.dto.VehicleDocumentDto;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.entity.VehicleDocument;
import pt.xavier.tms.vehicle.repository.VehicleDocumentRepository;

@Service
@ConditionalOnBean(VehicleDocumentRepository.class)
@Transactional(readOnly = true)
public class VehicleDocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final String VEHICLE_DOCUMENT_ENTITY_TYPE = "VEHICLE_DOCUMENT";

    private final VehicleService vehicleService;
    private final VehicleDocumentRepository vehicleDocumentRepository;

    public VehicleDocumentService(VehicleService vehicleService, VehicleDocumentRepository vehicleDocumentRepository) {
        this.vehicleService = vehicleService;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
    }

    @Auditable(entityType = VEHICLE_DOCUMENT_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public VehicleDocument addDocument(UUID vehicleId, VehicleDocumentDto dto) {
        validateFileSize(dto.fileSizeBytes());
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);

        VehicleDocument document = new VehicleDocument();
        document.setVehicle(vehicle);
        applyDto(document, dto);
        vehicle.getDocuments().add(document);
        return vehicleDocumentRepository.save(document);
    }

    @Auditable(entityType = VEHICLE_DOCUMENT_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public VehicleDocument updateDocument(UUID vehicleId, UUID documentId, VehicleDocumentDto dto) {
        validateFileSize(dto.fileSizeBytes());
        VehicleDocument document = getDocument(vehicleId, documentId);
        applyDto(document, dto);
        return document;
    }

    @Auditable(entityType = VEHICLE_DOCUMENT_ENTITY_TYPE, operation = AuditOperation.ELIMINACAO)
    @Transactional
    public void deleteDocument(UUID vehicleId, UUID documentId) {
        getDocument(vehicleId, documentId).softDelete("system");
    }

    public List<VehicleDocument> listDocuments(UUID vehicleId) {
        vehicleService.getVehicle(vehicleId);
        return vehicleDocumentRepository.findByVehicleId(vehicleId);
    }

    private VehicleDocument getDocument(UUID vehicleId, UUID documentId) {
        VehicleDocument document = vehicleDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("VEHICLE_DOCUMENT_NOT_FOUND", "Vehicle document not found"));
        if (!document.getVehicle().getId().equals(vehicleId)) {
            throw new ResourceNotFoundException("VEHICLE_DOCUMENT_NOT_FOUND", "Vehicle document not found");
        }
        return document;
    }

    private static void applyDto(VehicleDocument document, VehicleDocumentDto dto) {
        document.setDocumentType(dto.documentType());
        document.setDocumentNumber(dto.documentNumber());
        document.setIssueDate(dto.issueDate());
        document.setExpiryDate(dto.expiryDate());
        document.setIssuingEntity(dto.issuingEntity());
        document.setStatus(dto.status() == null ? DocumentStatus.VALIDO : dto.status());
        document.setNotes(dto.notes());
    }

    private static void validateFileSize(Long fileSizeBytes) {
        if (fileSizeBytes != null && fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("FILE_TOO_LARGE", "Vehicle document file cannot exceed 10 MB");
        }
    }
}
