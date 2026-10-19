package com.imagerecognitioner.exception;

import com.imagerecognitioner.config.TraceIdFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

import static com.imagerecognitioner.exception.ImageExceptions.*;

/**
 * Global handler for AWS and custom exceptions.
 * GlobalExceptionHandler
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ImageMetadataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ImageMetadataNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(DuplicateImageException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateImage(DuplicateImageException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ErrorResponse> handleInvalid(InvalidImageException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(InvalidModerationConfidenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidModerationConfidence(InvalidModerationConfidenceException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(ImageModerationException.class)
    public ResponseEntity<ErrorResponse> handleImageModeration(ImageModerationException ex, WebRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, message, request, ex);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ErrorResponse> handleS3(S3Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.statusCode());
        
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        return build(status, "Image storage error: " + ex.awsErrorDetails().errorMessage(), request, ex);
    }

    @ExceptionHandler(DynamoDbException.class)
    public ResponseEntity<ErrorResponse> handleDynamoDb(DynamoDbException ex, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.statusCode());
        
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }

        return build(status, "Image metadata storage error: " + ex.awsErrorDetails().errorMessage(), request, ex);
    }

    @ExceptionHandler(AwsServiceException.class)
    public ResponseEntity<ErrorResponse> handleAwsService(AwsServiceException ex, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.statusCode());
        
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        
        return build(status, "AWS error: " + ex.awsErrorDetails().errorMessage(), request, ex);
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<ErrorResponse> handleSdkClient(SdkClientException ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Could not reach AWS: " + ex.getMessage(), request, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, ex);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request, Exception ex) {
        String path = request.getDescription(false).replace("uri=", "");
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);

        if (status.is5xxServerError()) {
            log.error("[{}] {} - {}", traceId, status, ex.toString(), ex);
        } else {
            log.warn("[{}] {} - {}", traceId, status, ex.toString());
        }

        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message, path, traceId));
    }
}