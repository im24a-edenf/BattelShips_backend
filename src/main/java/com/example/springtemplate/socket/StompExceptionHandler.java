package com.example.springtemplate.socket;

import jakarta.validation.ConstraintViolationException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.time.Instant;

@ControllerAdvice
public class StompExceptionHandler {

    @MessageExceptionHandler({
            AccessDeniedException.class,
            IllegalArgumentException.class,
            MethodArgumentNotValidException.class,
            ConstraintViolationException.class
    })
    @SendToUser("/queue/errors")
    public StompErrorPayload handle(Exception exception) {
        return new StompErrorPayload(
                resolveCode(exception),
                resolveMessage(exception),
                Instant.now()
        );
    }

    private String resolveCode(Exception exception) {
        if (exception instanceof AccessDeniedException) {
            return "CHAT_UNAUTHORIZED";
        }

        return "CHAT_VALIDATION_ERROR";
    }

    private String resolveMessage(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException
                && validationException.getBindingResult().hasFieldErrors()) {
            return validationException.getBindingResult().getFieldErrors().getFirst().getDefaultMessage();
        }

        if (exception instanceof ConstraintViolationException constraintViolationException
                && !constraintViolationException.getConstraintViolations().isEmpty()) {
            return constraintViolationException.getConstraintViolations().iterator().next().getMessage();
        }

        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "Unable to process chat message"
                : exception.getMessage();
    }
}