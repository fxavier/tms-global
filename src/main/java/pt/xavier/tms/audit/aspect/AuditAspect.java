package pt.xavier.tms.audit.aspect;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import pt.xavier.tms.audit.annotation.Auditable;
import pt.xavier.tms.audit.event.AuditEvent;
import pt.xavier.tms.shared.enums.AuditOperation;

@Aspect
@Component
@ConditionalOnProperty(name = "tms.audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditAspect {

    private static final String SYSTEM_USER = "system";
    private static final String UNKNOWN_IP = "unknown";

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public AuditAspect(ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object around(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        UUID entityId = extractEntityIdFromArguments(joinPoint.getArgs()).orElse(null);
        Map<String, Object> previousValues = captureArgsSnapshot(joinPoint.getArgs(), auditable.operation());

        Object result = joinPoint.proceed();

        UUID resolvedEntityId = Optional.ofNullable(extractEntityId(result)).orElse(entityId);
        Map<String, Object> newValues = captureResultSnapshot(result, auditable.operation());

        eventPublisher.publishEvent(AuditEvent.of(
                auditable.entityType(),
                resolvedEntityId,
                auditable.operation(),
                resolveCurrentUser(),
                resolveCurrentIpAddress(),
                previousValues,
                newValues
        ));

        return result;
    }

    private Map<String, Object> captureArgsSnapshot(Object[] args, AuditOperation operation) {
        if (operation == AuditOperation.CRIACAO) {
            return null;
        }

        Object source = firstNonScalarArgument(args).orElse(null);
        return source == null ? null : toMap(source);
    }

    private Map<String, Object> captureResultSnapshot(Object result, AuditOperation operation) {
        if (operation == AuditOperation.ELIMINACAO || result == null) {
            return null;
        }

        return toMap(result);
    }

    private Optional<Object> firstNonScalarArgument(Object[] args) {
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (!(arg instanceof UUID) && !(arg instanceof CharSequence) && !(arg instanceof Number)
                    && !(arg instanceof Enum<?>)) {
                return Optional.of(arg);
            }
        }
        return Optional.empty();
    }

    private Optional<UUID> extractEntityIdFromArguments(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof UUID id) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }

    private UUID extractEntityId(Object result) {
        if (result == null) {
            return null;
        }

        if (result instanceof UUID id) {
            return id;
        }

        try {
            Method method = result.getClass().getMethod("getId");
            Object id = method.invoke(result);
            return id instanceof UUID uuid ? uuid : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private Map<String, Object> toMap(Object source) {
        return objectMapper.convertValue(source, new TypeReference<>() {
        });
    }

    private String resolveCurrentUser() {
        return Optional.ofNullable(org.springframework.security.core.context.SecurityContextHolder.getContext())
                .map(org.springframework.security.core.context.SecurityContext::getAuthentication)
                .filter(authentication -> authentication.isAuthenticated())
                .map(authentication -> authentication.getName())
                .filter(name -> name != null && !name.isBlank())
                .orElse(SYSTEM_USER);
    }

    private String resolveCurrentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return UNKNOWN_IP;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr() == null ? UNKNOWN_IP : request.getRemoteAddr();
    }
}
