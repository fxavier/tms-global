package pt.xavier.tms.vehicle.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.vehicle.dto.VehicleDocumentDto;
import pt.xavier.tms.vehicle.dto.VehicleDocumentResponseDto;
import pt.xavier.tms.vehicle.service.VehicleDocumentService;

@RestController
@RequestMapping("/api/v1/vehicles/{id}/documents")
@ConditionalOnProperty(name = "tms.vehicle.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class VehicleDocumentController {

    private final VehicleDocumentService vehicleDocumentService;

    public VehicleDocumentController(VehicleDocumentService vehicleDocumentService) {
        this.vehicleDocumentService = vehicleDocumentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<List<VehicleDocumentResponseDto>> listDocuments(@PathVariable UUID id) {
        return ApiResponse.success(vehicleDocumentService.listDocuments(id).stream()
                .map(VehicleDtoMapper::toDocumentResponse)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<VehicleDocumentResponseDto>> addDocument(
            @PathVariable UUID id,
            @Valid @RequestBody VehicleDocumentDto request
    ) {
        VehicleDocumentResponseDto response = VehicleDtoMapper.toDocumentResponse(
                vehicleDocumentService.addDocument(id, request)
        );
        return ResponseEntity.created(URI.create("/api/v1/vehicles/" + id + "/documents/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<VehicleDocumentResponseDto> updateDocument(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            @Valid @RequestBody VehicleDocumentDto request
    ) {
        return ApiResponse.success(VehicleDtoMapper.toDocumentResponse(
                vehicleDocumentService.updateDocument(id, docId, request)
        ));
    }

    @DeleteMapping("/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id, @PathVariable UUID docId) {
        vehicleDocumentService.deleteDocument(id, docId);
        return ApiResponse.success(null);
    }
}
