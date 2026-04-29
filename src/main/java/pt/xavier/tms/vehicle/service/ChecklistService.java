package pt.xavier.tms.vehicle.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.shared.enums.AuditOperation;
import pt.xavier.tms.shared.exception.BusinessException;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;
import pt.xavier.tms.vehicle.dto.ChecklistInspectionDto;
import pt.xavier.tms.vehicle.dto.ChecklistTemplateDto;
import pt.xavier.tms.vehicle.entity.ChecklistInspection;
import pt.xavier.tms.vehicle.entity.ChecklistInspectionItem;
import pt.xavier.tms.vehicle.entity.ChecklistTemplate;
import pt.xavier.tms.vehicle.entity.ChecklistTemplateItem;
import pt.xavier.tms.vehicle.entity.Vehicle;
import pt.xavier.tms.vehicle.repository.ChecklistInspectionRepository;
import pt.xavier.tms.vehicle.repository.ChecklistTemplateRepository;

@Service
@ConditionalOnProperty(name = "tms.vehicle.services.enabled", havingValue = "true", matchIfMissing = true)
@Transactional(readOnly = true)
public class ChecklistService {

    private static final String CHECKLIST_ENTITY_TYPE = "CHECKLIST_INSPECTION";
    private static final String CHECKLIST_TEMPLATE_ENTITY_TYPE = "CHECKLIST_TEMPLATE";

    private final VehicleService vehicleService;
    private final ChecklistTemplateRepository checklistTemplateRepository;
    private final ChecklistInspectionRepository checklistInspectionRepository;

    public ChecklistService(
            VehicleService vehicleService,
            ChecklistTemplateRepository checklistTemplateRepository,
            ChecklistInspectionRepository checklistInspectionRepository
    ) {
        this.vehicleService = vehicleService;
        this.checklistTemplateRepository = checklistTemplateRepository;
        this.checklistInspectionRepository = checklistInspectionRepository;
    }

    @Auditable(entityType = CHECKLIST_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public ChecklistInspection submitChecklist(UUID vehicleId, ChecklistInspectionDto dto) {
        Vehicle vehicle = vehicleService.getVehicle(vehicleId);
        ChecklistTemplate template = getTemplate(dto.templateId());

        ChecklistInspection inspection = new ChecklistInspection();
        inspection.setVehicle(vehicle);
        inspection.setTemplate(template);
        inspection.setActivityId(dto.activityId());
        inspection.setPerformedBy(dto.performedBy());
        inspection.setPerformedAt(dto.performedAt() == null ? Instant.now() : dto.performedAt());
        inspection.setNotes(dto.notes());

        dto.items().forEach(itemDto -> inspection.getItems().add(toInspectionItem(inspection, template, itemDto)));
        if (inspection.hasCriticalFailures()) {
            throw new BusinessException("CRITICAL_CHECKLIST_FAILURE", "Checklist contains critical failures");
        }

        return checklistInspectionRepository.save(inspection);
    }

    public Page<ChecklistInspection> listChecklists(UUID vehicleId, Pageable pageable) {
        vehicleService.getVehicle(vehicleId);
        return checklistInspectionRepository.findByVehicleId(vehicleId, pageable);
    }

    public ChecklistTemplate getTemplate(UUID templateId) {
        return checklistTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("CHECKLIST_TEMPLATE_NOT_FOUND", "Checklist template not found"));
    }

    public List<ChecklistTemplate> listTemplates(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            return checklistTemplateRepository.findAll();
        }
        return checklistTemplateRepository.findByVehicleTypeAndIsActiveTrue(vehicleType);
    }

    @Auditable(entityType = CHECKLIST_TEMPLATE_ENTITY_TYPE, operation = AuditOperation.CRIACAO)
    @Transactional
    public ChecklistTemplate createTemplate(ChecklistTemplateDto dto) {
        ChecklistTemplate template = new ChecklistTemplate();
        applyTemplateDto(template, dto);
        return checklistTemplateRepository.save(template);
    }

    @Auditable(entityType = CHECKLIST_TEMPLATE_ENTITY_TYPE, operation = AuditOperation.ATUALIZACAO)
    @Transactional
    public ChecklistTemplate updateTemplate(UUID templateId, ChecklistTemplateDto dto) {
        ChecklistTemplate template = getTemplate(templateId);
        template.getItems().clear();
        applyTemplateDto(template, dto);
        return template;
    }

    private static ChecklistInspectionItem toInspectionItem(
            ChecklistInspection inspection,
            ChecklistTemplate template,
            ChecklistInspectionDto.Item dto
    ) {
        ChecklistInspectionItem item = new ChecklistInspectionItem();
        item.setInspection(inspection);
        item.setTemplateItem(resolveTemplateItem(template, dto.templateItemId()));
        item.setItemName(dto.itemName());
        item.setCritical(dto.critical());
        item.setStatus(dto.status());
        item.setNotes(dto.notes());
        return item;
    }

    private static ChecklistTemplateItem resolveTemplateItem(ChecklistTemplate template, UUID templateItemId) {
        if (templateItemId == null) {
            return null;
        }

        return template.getItems().stream()
                .filter(item -> templateItemId.equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CHECKLIST_TEMPLATE_ITEM_NOT_FOUND",
                        "Checklist template item not found"
                ));
    }

    private static void applyTemplateDto(ChecklistTemplate template, ChecklistTemplateDto dto) {
        template.setVehicleType(dto.vehicleType());
        template.setName(dto.name());
        template.setActive(dto.active());

        dto.items().forEach(itemDto -> {
            ChecklistTemplateItem item = new ChecklistTemplateItem();
            item.setTemplate(template);
            item.setItemName(itemDto.itemName());
            item.setCritical(itemDto.critical());
            item.setDisplayOrder(itemDto.displayOrder());
            template.getItems().add(item);
        });
    }
}
