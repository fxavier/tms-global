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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.hr.dto.EmployeePaymentStatusDto;
import pt.xavier.tms.hr.dto.PaymentStatusFilter;
import pt.xavier.tms.hr.dto.SalaryPaymentCreateDto;
import pt.xavier.tms.hr.dto.SalaryPaymentResponseDto;
import pt.xavier.tms.hr.mapper.SalaryPaymentMapper;
import pt.xavier.tms.hr.service.SalaryPaymentService;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.dto.PagedResponse;

@RestController
@RequestMapping("/api/v1/hr/salary-payments")
@ConditionalOnProperty(name = "tms.hr.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class SalaryPaymentController {

    private final SalaryPaymentService service;
    private final SalaryPaymentMapper mapper;

    public SalaryPaymentController(SalaryPaymentService service, SalaryPaymentMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ResponseEntity<ApiResponse<SalaryPaymentResponseDto>> create(@Valid @RequestBody SalaryPaymentCreateDto request) {
        SalaryPaymentResponseDto response = mapper.toResponse(service.registerPayment(request));
        return ResponseEntity.created(URI.create("/api/v1/hr/salary-payments/" + response.id())).body(ApiResponse.success(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','AUDITOR')")
    public ApiResponse<PagedResponse<SalaryPaymentResponseDto>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(service.listPayments(year, month, employeeId, Pageables.of(page, size))
                .map(mapper::toResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','AUDITOR')")
    public ApiResponse<SalaryPaymentResponseDto> getById(@PathVariable UUID id) {
        return ApiResponse.success(mapper.toResponse(service.getPayment(id)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH')")
    public ApiResponse<SalaryPaymentResponseDto> cancel(@PathVariable UUID id, @RequestBody CancelPaymentRequest request) {
        return ApiResponse.success(mapper.toResponse(service.cancelPayment(id, request.reason())));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_RH','AUDITOR')")
    public ApiResponse<PagedResponse<EmployeePaymentStatusDto>> status(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(defaultValue = "ALL") PaymentStatusFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(PagedResponse.from(service.getPaymentStatus(year, month, filter, Pageables.of(page, size))));
    }

    private record CancelPaymentRequest(String reason) {
    }
}
