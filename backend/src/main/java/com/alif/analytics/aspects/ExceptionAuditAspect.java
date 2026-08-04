package com.alif.analytics.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class ExceptionAuditAspect {

    @AfterThrowing(
            pointcut = "execution(* com.alif.analytics..*.*(..))",
            throwing = "ex"
    )
    public void logAfterException(JoinPoint joinPoint, Exception ex) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] methodArgs = joinPoint.getArgs();

        log.error("âŒ Exception occurred in method: {}", methodName);
        log.error("ðŸ“¥ Arguments: {}", Arrays.toString(methodArgs));
        log.error("ðŸ’¥ Exception type: {}", ex.getClass().getSimpleName());
        log.error("ðŸ§¾ Exception message: {}", ex.getMessage());

        // Here you could also:
        // - Send metrics
        // - Push audit events
        // - Trigger alerts
    }

}

