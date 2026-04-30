package pt.xavier.tms.driver.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

import pt.xavier.tms.driver.dto.DriverCreateDto;
import pt.xavier.tms.driver.dto.DriverResponseDto;
import pt.xavier.tms.driver.dto.DriverStatusUpdateDto;
import pt.xavier.tms.driver.dto.DriverUpdateDto;
import pt.xavier.tms.driver.service.DriverService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.shared.enums.DriverStatus;

@RestController
@RequestMapping("/api/v1/drivers")
@ConditionalOnProperty(name = "tms.driver.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ResponseEntity<ApiResponse<DriverResponseDto>> createDriver(@Valid @RequestBody DriverCreateDto request) {
        DriverResponseDto response = DriverDtoMapper.toDriverResponse(driverService.createDriver(request));
        return ResponseEntity.created(URI.create("/api/v1/drivers/" + response.id()))
                .body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<DriverResponseDto>> listDrivers(
            @RequestParam(required = false) DriverStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(
                driverService.listDrivers(status, location, Pageables.of(page, size)).map(DriverDtoMapper::toDriverResponse)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<DriverResponseDto> getDriver(@PathVariable UUID id) {
        return ApiResponse.success(DriverDtoMapper.toDriverResponse(driverService.getDriver(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<DriverResponseDto> updateDriver(@PathVariable UUID id, @Valid @RequestBody DriverUpdateDto request) {
        return ApiResponse.success(DriverDtoMapper.toDriverResponse(driverService.updateDriver(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<DriverResponseDto> updateStatus(@PathVariable UUID id, @Valid @RequestBody DriverStatusUpdateDto request) {
        return ApiResponse.success(DriverDtoMapper.toDriverResponse(driverService.updateStatus(id, request.status())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDriver(@PathVariable UUID id) {
        driverService.deleteDriver(id);
        return ApiResponse.success(null);
    }

}
