package pt.xavier.tms.activity.service;

import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import pt.xavier.tms.activity.repository.ActivityRepository;

@Service("activitySecurityService")
@ConditionalOnProperty(name = "tms.activity.services.enabled", havingValue = "true", matchIfMissing = true)
public class ActivitySecurityService {

    private final ActivityRepository activityRepository;

    public ActivitySecurityService(ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
    }

    public boolean isAssignedDriver(UUID activityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return false;
        }

        UUID currentDriverId;
        try {
            currentDriverId = UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        return activityRepository.findById(activityId)
                .map(activity -> activity.getDriver() != null && currentDriverId.equals(activity.getDriver().getId()))
                .orElse(false);
    }
}
