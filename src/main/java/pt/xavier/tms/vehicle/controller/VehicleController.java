package pt.xavier.tms.vehicle.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.shared.enums.VehicleStatus;
import pt.xavier.tms.vehicle.dto.VehicleConsolidatedDto;
import pt.xavier.tms.vehicle.dto.VehicleCreateDto;
import pt.xavier.tms.vehicle.dto.VehicleResponseDto;
import pt.xavier.tms.vehicle.dto.VehicleStatusUpdateDto;
import pt.xavier.tms.vehicle.dto.VehicleUpdateDto;
import pt.xavier.tms.vehicle.service.VehicleService;

@RestController
@RequestMapping("/api/v1/vehicles")
@ConditionalOnProperty(name = "tms.vehicle.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<VehicleResponseDto>> createVehicle(@Valid @RequestBody VehicleCreateDto request) {
        VehicleResponseDto response = VehicleDtoMapper.toVehicleResponse(vehicleService.createVehicle(request));
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<VehicleResponseDto>> listVehicles(
            @RequestParam(required = false) VehicleStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = Pageables.of(page, size);
        return ApiResponse.success(PagedResponse.from(
                vehicleService.listVehicles(status, location, pageable).map(VehicleDtoMapper::toVehicleResponse)
        ));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR','MOTORISTA','RH_INTEGRADOR')")
    public ApiResponse<PagedResponse<VehicleResponseDto>> searchVehicles(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(
                vehicleService.searchByPlate(q, Pageables.of(page, size)).map(VehicleDtoMapper::toVehicleResponse)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<VehicleResponseDto> getVehicle(@PathVariable UUID id) {
        return ApiResponse.success(VehicleDtoMapper.toVehicleResponse(vehicleService.getVehicle(id)));
    }

    @GetMapping("/{id}/consolidated")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<VehicleConsolidatedDto> getConsolidated(@PathVariable UUID id) {
        return ApiResponse.success(vehicleService.getConsolidated(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<VehicleResponseDto> updateVehicle(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleUpdateDto request
    ) {
        return ApiResponse.success(VehicleDtoMapper.toVehicleResponse(vehicleService.updateVehicle(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<VehicleResponseDto> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleStatusUpdateDto request
    ) {
        return ApiResponse.success(VehicleDtoMapper.toVehicleResponse(vehicleService.updateStatus(id, request.status())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVehicle(@PathVariable UUID id) {
        vehicleService.deleteVehicle(id);
        return ApiResponse.success(null);
    }
}
