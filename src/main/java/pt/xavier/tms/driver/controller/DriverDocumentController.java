package pt.xavier.tms.driver.controller;

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
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.driver.dto.DriverDocumentDto;
import pt.xavier.tms.driver.dto.DriverDocumentResponseDto;
import pt.xavier.tms.driver.service.DriverDocumentService;
import pt.xavier.tms.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/drivers/{id}/documents")
@ConditionalOnProperty(name = "tms.driver.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class DriverDocumentController {

    private final DriverDocumentService driverDocumentService;

    public DriverDocumentController(DriverDocumentService driverDocumentService) {
        this.driverDocumentService = driverDocumentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<List<DriverDocumentResponseDto>> listDocuments(@PathVariable UUID id) {
        return ApiResponse.success(driverDocumentService.listDocuments(id).stream()
                .map(DriverDtoMapper::toDocumentResponse)
                .toList());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<DriverDocumentResponseDto>> addDocument(
            @PathVariable UUID id,
            @Valid @RequestBody DriverDocumentDto request
    ) {
        DriverDocumentResponseDto response = DriverDtoMapper.toDocumentResponse(driverDocumentService.addDocument(id, request));
        return ResponseEntity.created(URI.create("/api/v1/drivers/" + id + "/documents/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{docId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<DriverDocumentResponseDto> updateDocument(
            @PathVariable UUID id,
            @PathVariable UUID docId,
            @Valid @RequestBody DriverDocumentDto request
    ) {
        return ApiResponse.success(DriverDtoMapper.toDocumentResponse(driverDocumentService.updateDocument(id, docId, request)));
    }
}
