package pt.xavier.tms.activity.dto;

import jakarta.validation.constraints.NotNull;
import pt.xavier.tms.shared.enums.ActivityStatus;

public record StatusTransitionDto(
        @NotNull ActivityStatus newStatus,
        String notes
) {
}
