package pt.xavier.tms.vehicle.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.vehicle.dto.ChecklistInspectionDto;
import pt.xavier.tms.vehicle.dto.ChecklistInspectionResponseDto;
import pt.xavier.tms.vehicle.dto.ChecklistTemplateDto;
import pt.xavier.tms.vehicle.dto.ChecklistTemplateResponseDto;
import pt.xavier.tms.vehicle.service.ChecklistService;

@RestController
@ConditionalOnProperty(name = "tms.vehicle.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping("/api/v1/vehicles/{id}/checklists")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<ChecklistInspectionResponseDto>> listChecklists(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(
                checklistService.listChecklists(id, Pageables.of(page, size)).map(VehicleDtoMapper::toChecklistResponse)
        ));
    }

    @PostMapping("/api/v1/vehicles/{id}/checklists")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','MOTORISTA')")
    public ResponseEntity<ApiResponse<ChecklistInspectionResponseDto>> submitChecklist(
            @PathVariable UUID id,
            @Valid @RequestBody ChecklistInspectionDto request
    ) {
        ChecklistInspectionResponseDto response = VehicleDtoMapper.toChecklistResponse(
                checklistService.submitChecklist(id, request)
        );
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + id + "/checklists/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/checklist-templates")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR','MOTORISTA')")
    public ApiResponse<List<ChecklistTemplateResponseDto>> listTemplates(
            @RequestParam(required = false) String vehicleType
    ) {
        return ApiResponse.success(checklistService.listTemplates(vehicleType).stream()
                .map(VehicleDtoMapper::toTemplateResponse)
                .toList());
    }

    @PostMapping("/api/v1/checklist-templates")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<ChecklistTemplateResponseDto>> createTemplate(
            @Valid @RequestBody ChecklistTemplateDto request
    ) {
        ChecklistTemplateResponseDto response = VehicleDtoMapper.toTemplateResponse(checklistService.createTemplate(request));
        return ResponseEntity.created(URI.create("/api/v1/checklist-templates/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PutMapping("/api/v1/checklist-templates/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<ChecklistTemplateResponseDto> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody ChecklistTemplateDto request
    ) {
        return ApiResponse.success(VehicleDtoMapper.toTemplateResponse(checklistService.updateTemplate(id, request)));
    }
}
