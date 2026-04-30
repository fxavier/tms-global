package pt.xavier.tms.hr.controller;

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

import pt.xavier.tms.hr.dto.EmployeeCreateDto;
import pt.xavier.tms.hr.dto.EmployeeResponseDto;
import pt.xavier.tms.hr.dto.EmployeeUpdateDto;
import pt.xavier.tms.hr.mapper.EmployeeMapper;
import pt.xavier.tms.hr.service.EmployeeService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;
import pt.xavier.tms.shared.enums.EmployeeStatus;

@RestController
@RequestMapping("/api/v1/hr/employees")
@ConditionalOnProperty(name = "tms.hr.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class EmployeeController {

    private final EmployeeService service;
    private final EmployeeMapper mapper;

    public EmployeeController(EmployeeService service, EmployeeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ResponseEntity<ApiResponse<EmployeeResponseDto>> create(@Valid @RequestBody EmployeeCreateDto request) {
        EmployeeResponseDto response = mapper.toResponse(service.createEmployee(request));
        return ResponseEntity.created(URI.create("/api/v1/hr/employees/" + response.id())).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<EmployeeResponseDto>> list(
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) UUID functionId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(service.listEmployees(status, functionId, q, Pageables.of(page, size))
                .map(mapper::toResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<EmployeeResponseDto> getById(@PathVariable UUID id) {
        return ApiResponse.success(mapper.toResponse(service.getEmployee(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<EmployeeResponseDto> update(@PathVariable UUID id, @Valid @RequestBody EmployeeUpdateDto request) {
        return ApiResponse.success(mapper.toResponse(service.updateEmployee(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<EmployeeResponseDto> updateStatus(@PathVariable UUID id, @RequestBody EmployeeStatusUpdateRequest request) {
        return ApiResponse.success(mapper.toResponse(service.updateStatus(id, request.status())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        service.deleteEmployee(id);
        return ApiResponse.success(null);
    }

    private record EmployeeStatusUpdateRequest(EmployeeStatus status) {
    }
}
