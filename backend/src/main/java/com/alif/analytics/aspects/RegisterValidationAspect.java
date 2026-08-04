package com.alif.analytics.aspects;

import com.alif.analytics.dto.RegisterRequestDto;
import com.alif.analytics.entity.JobPortalUser;
import com.alif.analytics.exception.RegistrationValidationException;
import com.alif.analytics.repository.JobPortalUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.authentication.password.CompromisedPasswordChecker;
import org.springframework.security.authentication.password.CompromisedPasswordDecision;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RegisterValidationAspect {

    private final CompromisedPasswordChecker compromisedPasswordChecker;
    private final JobPortalUserRepository jobPortalUserRepository;

    @Before("""
        execution(* com.alif.analytics.auth.AuthController
        .registerUser(..))
        """)
    public void validateBeforeRegister(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        RegisterRequestDto request = (RegisterRequestDto) args[0];
        log.info("ðŸ” Validating user registration request");
        Map<String, String> errors = new HashMap<>();
        // 1ï¸âƒ£ Compromised password check
        CompromisedPasswordDecision decision =
                compromisedPasswordChecker.check(request.password());
        if (decision.isCompromised()) {
            errors.put("password", "Choose a strong password");
        }
        // 2ï¸âƒ£ Existing user check
        Optional<JobPortalUser> existingUser =
                jobPortalUserRepository.readUserByEmailOrMobileNumber(
                        request.email(), request.mobileNumber());

        if (existingUser.isPresent()) {
            JobPortalUser user = existingUser.get();

            if (user.getEmail().equalsIgnoreCase(request.email())) {
                errors.put("email", "Email is already registered");
            }

            if (user.getMobileNumber().equals(request.mobileNumber())) {
                errors.put("mobileNumber", "Mobile number is already registered");
            }
        }

        // 3ï¸âƒ£ Stop execution if validation fails
        if (!errors.isEmpty()) {
            log.warn("âŒ Registration validation failed: {}", errors);
            throw new RegistrationValidationException(errors);
        }

        log.info("âœ… Registration validation passed");
    }

}

