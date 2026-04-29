package pt.xavier.tms.shared.enums;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import pt.xavier.tms.shared.exception.BusinessException;

public enum ActivityStatus {
    PLANEADA,
    EM_CURSO,
    SUSPENSA,
    CONCLUIDA,
    CANCELADA;

    private static final Map<ActivityStatus, Set<ActivityStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ActivityStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PLANEADA, EnumSet.of(EM_CURSO, CANCELADA));
        ALLOWED_TRANSITIONS.put(EM_CURSO, EnumSet.of(SUSPENSA, CONCLUIDA, CANCELADA));
        ALLOWED_TRANSITIONS.put(SUSPENSA, EnumSet.of(EM_CURSO, CANCELADA));
        ALLOWED_TRANSITIONS.put(CONCLUIDA, EnumSet.noneOf(ActivityStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELADA, EnumSet.noneOf(ActivityStatus.class));
    }

    public boolean canTransitionTo(ActivityStatus target) {
        if (target == null) {
            return false;
        }
        return this == target || ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public void validateTransition(ActivityStatus target) {
        if (!canTransitionTo(target)) {
            throw new BusinessException(
                    "INVALID_STATUS_TRANSITION",
                    "Invalid activity status transition from " + this + " to " + target
            );
        }
    }
}
