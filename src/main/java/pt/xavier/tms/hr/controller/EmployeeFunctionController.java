package pt.xavier.tms.hr.controller;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.hr.dto.EmployeeFunctionCreateDto;
import pt.xavier.tms.hr.dto.EmployeeFunctionResponseDto;
import pt.xavier.tms.hr.dto.EmployeeFunctionUpdateDto;
import pt.xavier.tms.hr.mapper.EmployeeFunctionMapper;
import pt.xavier.tms.hr.service.EmployeeFunctionService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;

@RestController
@RequestMapping("/api/v1/hr/functions")
@ConditionalOnProperty(name = "tms.hr.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class EmployeeFunctionController {

    private final EmployeeFunctionService service;
    private final EmployeeFunctionMapper mapper;

    public EmployeeFunctionController(EmployeeFunctionService service, EmployeeFunctionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ResponseEntity<ApiResponse<EmployeeFunctionResponseDto>> create(@Valid @RequestBody EmployeeFunctionCreateDto request) {
        EmployeeFunctionResponseDto response = mapper.toResponse(service.createFunction(request));
        return ResponseEntity.created(URI.create("/api/v1/hr/functions/" + response.id())).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<PagedResponse<EmployeeFunctionResponseDto>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(service.listFunctions(Pageables.of(page, size)).map(mapper::toResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','GESTOR_FROTA','OPERADOR','AUDITOR')")
    public ApiResponse<EmployeeFunctionResponseDto> getById(@PathVariable UUID id) {
        return ApiResponse.success(mapper.toResponse(service.getFunction(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<EmployeeFunctionResponseDto> update(@PathVariable UUID id, @Valid @RequestBody EmployeeFunctionUpdateDto request) {
        return ApiResponse.success(mapper.toResponse(service.updateFunction(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<EmployeeFunctionResponseDto> activate(@PathVariable UUID id) {
        return ApiResponse.success(mapper.toResponse(service.activate(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<EmployeeFunctionResponseDto> deactivate(@PathVariable UUID id) {
        return ApiResponse.success(mapper.toResponse(service.deactivate(id)));
    }
}
