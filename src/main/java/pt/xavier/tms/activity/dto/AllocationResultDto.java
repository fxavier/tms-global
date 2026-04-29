package pt.xavier.tms.activity.dto;

import java.util.List;

public record AllocationResultDto(
        boolean allocated,
        List<AllocationBlockerDto> blockers
) {
}
