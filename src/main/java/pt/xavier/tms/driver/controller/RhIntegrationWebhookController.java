package pt.xavier.tms.driver.controller;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.xavier.tms.integration.dto.DriverAvailabilityDto;
import pt.xavier.tms.shared.dto.ApiResponse;

@RestController
@RequestMapping("/api/v1/integration/rh")
@ConditionalOnProperty(name = "tms.driver.controllers.enabled", havingValue = "true", matchIfMissing = true)
public class RhIntegrationWebhookController {

    @PostMapping("/availability")
    @PreAuthorize("hasRole('RH_INTEGRADOR')")
    public ApiResponse<DriverAvailabilityDto> availabilityWebhook(@Valid @RequestBody DriverAvailabilityDto payload) {
        return ApiResponse.success(payload);
    }
}
