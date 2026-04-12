package com.siddharth.tradesim_backend.common.exceptions;

import com.siddharth.tradesim_backend.common.dto.ApiError;
import com.siddharth.tradesim_backend.common.error.ApiErrorResponseWriter;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final ApiErrorResponseWriter apiErrorResponseWriter;

    public GlobalExceptionHandler(ApiErrorResponseWriter apiErrorResponseWriter) {
        this.apiErrorResponseWriter = apiErrorResponseWriter;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(apiErrorResponseWriter.build(ex.getStatus(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() == null ? "Invalid value." : fieldError.getDefaultMessage(),
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Validation failed for request body.",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "Validation failed for request.",
                        request.getRequestURI(),
                        fieldErrors
                ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        String fieldName = ex.getName();
        Class<?> requiredType = ex.getRequiredType();
        String detail = buildExpectedTypeDetail(requiredType);
        String message = "Invalid value for '" + fieldName + "'. " + detail;
        String errorCode = isPathVariable(ex) ? "INVALID_PATH_VARIABLE" : "INVALID_REQUEST_PARAMETER";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        errorCode,
                        message,
                        request.getRequestURI(),
                        Map.of(fieldName, detail)
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        Throwable mostSpecificCause = ex.getMostSpecificCause();

        if (mostSpecificCause instanceof InvalidFormatException invalidFormatException) {
            String fieldName = extractJsonFieldPath(invalidFormatException);
            String detail = buildExpectedTypeDetail(invalidFormatException.getTargetType());
            String message = "Invalid value for '" + fieldName + "'. " + detail;

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(apiErrorResponseWriter.build(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_REQUEST_BODY",
                            message,
                            request.getRequestURI(),
                            Map.of(fieldName, detail)
                    ));
        }

        if (mostSpecificCause instanceof MismatchedInputException mismatchedInputException) {
            String fieldName = extractJsonFieldPath(mismatchedInputException);
            String detail = "Request body structure is invalid.";
            String message = "Invalid value for '" + fieldName + "'. " + detail;

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(apiErrorResponseWriter.build(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_REQUEST_BODY",
                            message,
                            request.getRequestURI(),
                            Map.of(fieldName, detail)
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_REQUEST_BODY",
                        "Malformed JSON request body.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.NOT_FOUND,
                        "ENDPOINT_NOT_FOUND",
                        "Endpoint not found.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.";

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.METHOD_NOT_ALLOWED,
                        "METHOD_NOT_ALLOWED",
                        message,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "UNSUPPORTED_MEDIA_TYPE",
                        "Unsupported content type. Use application/json.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String message = "Missing required request parameter '" + ex.getParameterName() + "'.";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        "MISSING_REQUEST_PARAMETER",
                        message,
                        request.getRequestURI(),
                        Map.of(ex.getParameterName(), "Parameter is required.")
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.FORBIDDEN,
                        "ACCESS_DENIED",
                        "Access denied.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication required.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(
            JwtException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.UNAUTHORIZED,
                        "JWT_ERROR",
                        "Invalid or expired token.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.CONFLICT,
                        "DATA_INTEGRITY_ERROR",
                        "Database constraint violation.",
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error occurred", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(apiErrorResponseWriter.build(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "Internal server error.",
                        request.getRequestURI()
                ));
    }

    @SuppressWarnings("ConstantConditions")
    private boolean isPathVariable(MethodArgumentTypeMismatchException ex) {
        return ex.getParameter() != null && ex.getParameter().hasParameterAnnotation(PathVariable.class);
    }

    private String extractJsonFieldPath(MismatchedInputException ex) {
        String pathReference = ex.getPathReference();
        if (pathReference == null || pathReference.isBlank()) {
            return "requestBody";
        }

        Matcher matcher = Pattern.compile("\\[\"([^\"]+)\"]|\\[(\\d+)]").matcher(pathReference);
        String fieldName = null;
        while (matcher.find()) {
            fieldName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        }

        return fieldName == null || fieldName.isBlank() ? "requestBody" : fieldName;
    }

    private String buildExpectedTypeDetail(Class<?> requiredType) {
        if (requiredType == null) {
            return "Invalid value.";
        }

        if (requiredType == java.util.UUID.class) {
            return "Expected UUID format.";
        }

        if (requiredType == Integer.class || requiredType == int.class) {
            return "Expected integer.";
        }

        if (requiredType == Long.class || requiredType == long.class) {
            return "Expected long.";
        }

        if (requiredType == Boolean.class || requiredType == boolean.class) {
            return "Expected boolean.";
        }

        if (requiredType == java.math.BigDecimal.class) {
            return "Expected decimal number.";
        }

        if (requiredType.isEnum()) {
            String values = Arrays.stream(requiredType.getEnumConstants())
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            return "Allowed values are: " + values + ".";
        }

        return "Expected value of type " + requiredType.getSimpleName() + ".";
    }
}