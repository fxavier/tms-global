package pt.xavier.tms.alert.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.alert.dto.AlertConfigurationResponseDto;
import pt.xavier.tms.alert.dto.AlertConfigurationUpdateDto;
import pt.xavier.tms.alert.entity.AlertConfiguration;
import pt.xavier.tms.alert.repository.AlertConfigurationRepository;
import pt.xavier.tms.shared.dto.ApiResponse;
import pt.xavier.tms.shared.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/v1/alert-configurations")
@ConditionalOnProperty(name = "tms.alert.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class AlertConfigurationController {

    private final AlertConfigurationRepository alertConfigurationRepository;

    public AlertConfigurationController(AlertConfigurationRepository alertConfigurationRepository) {
        this.alertConfigurationRepository = alertConfigurationRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<List<AlertConfigurationResponseDto>> list() {
        return ApiResponse.success(alertConfigurationRepository.findAll().stream()
                .map(AlertConfigurationController::toResponse)
                .toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR_FROTA')")
    public ApiResponse<AlertConfigurationResponseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody AlertConfigurationUpdateDto request
    ) {
        AlertConfiguration configuration = alertConfigurationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ALERT_CONFIGURATION_NOT_FOUND",
                        "Alert configuration not found"
                ));

        configuration.setDaysBeforeWarning(request.daysBeforeWarning());
        configuration.setDaysBeforeCritical(request.daysBeforeCritical());
        configuration.setActive(request.active());

        return ApiResponse.success(toResponse(alertConfigurationRepository.save(configuration)));
    }

    private static AlertConfigurationResponseDto toResponse(AlertConfiguration configuration) {
        return new AlertConfigurationResponseDto(
                configuration.getId(),
                configuration.getAlertType(),
                configuration.getEntityType(),
                configuration.getDaysBeforeWarning(),
                configuration.getDaysBeforeCritical(),
                configuration.isActive()
        );
    }
}
