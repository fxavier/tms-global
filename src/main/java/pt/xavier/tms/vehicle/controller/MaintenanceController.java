package pt.xavier.tms.vehicle.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.vehicle.dto.MaintenanceRecordDto;
import pt.xavier.tms.vehicle.dto.MaintenanceResponseDto;
import pt.xavier.tms.vehicle.service.MaintenanceService;

@RestController
@RequestMapping("/api/v1/vehicles/{id}/maintenance")
@ConditionalOnProperty(name = "tms.vehicle.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<MaintenanceResponseDto>> listMaintenance(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(
                maintenanceService.listMaintenance(id, Pageables.of(page, size)).map(VehicleDtoMapper::toMaintenanceResponse)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<MaintenanceResponseDto>> registerMaintenance(
            @PathVariable UUID id,
            @Valid @RequestBody MaintenanceRecordDto request
    ) {
        MaintenanceResponseDto response = VehicleDtoMapper.toMaintenanceResponse(
                maintenanceService.registerMaintenance(id, request)
        );
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + id + "/maintenance/" + response.id()))
                .body(ApiResponse.success(response));
    }
}
