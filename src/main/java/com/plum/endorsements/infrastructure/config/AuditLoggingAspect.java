package com.plum.endorsements.infrastructure.config;

import com.plum.endorsements.application.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;

    @Around("execution(* com.plum.endorsements.application.handler.*Handler.*(..))")
    public Object auditHandlerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String action = className + "." + methodName;

        // Extract entity ID from first UUID argument if present
        String entityId = null;
        String entityType = "Endorsement";
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof UUID uuid) {
                entityId = uuid.toString();
                break;
            }
        }

        Object result = joinPoint.proceed();

        try {
            // Extract ID from result if it has a getId() method
            if (entityId == null && result != null) {
                try {
                    var method = result.getClass().getMethod("getId");
                    Object id = method.invoke(result);
                    if (id instanceof UUID uuid) {
                        entityId = uuid.toString();
                    }
                } catch (NoSuchMethodException ignored) {
                    // Result doesn't have getId()
                }
            }

            auditLogService.log(action, entityType, entityId, "SYSTEM", null);
        } catch (Exception e) {
            log.warn("Failed to write audit log for {}: {}", action, e.getMessage());
        }

        return result;
    }
}
