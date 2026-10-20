package com.imagerecognitioner.exception;

import com.imagerecognitioner.config.TraceIdFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Map;

import static com.imagerecognitioner.exception.ImageExceptions.ImageMetadataNotFoundException;
import static com.imagerecognitioner.exception.ImageExceptions.InvalidImageException;
import static com.imagerecognitioner.exception.ImageExceptions.DuplicateImageException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.slf4j.MDC.put;
import static org.slf4j.MDC.remove;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest request;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearTraceId() {
        remove(TraceIdFilter.TRACE_ID_MDC_KEY);
    }

    @Test
    void handleNotFound_returnsNotFoundResponse() {
        runTestCases(Map.of("metadata missing", () -> assertResponse(
            handler.handleNotFound(new ImageMetadataNotFoundException("img-1"), request),
            404, "ImageMetadata not found with id: img-1")));
    }

    @Test
    void handleDuplicateImage_returnsConflictResponse() {
        runTestCases(Map.of("duplicate image", () -> assertResponse(
            handler.handleDuplicateImage(new DuplicateImageException("hash"), request),
            409, "An image with this content hash already exists: hash")));
    }

    @Test
    void handleInvalid_returnsBadRequestResponse() {
        runTestCases(Map.of("invalid image", () -> assertResponse(
            handler.handleInvalid(new InvalidImageException("unsupported type"), request),
            400, "unsupported type")));
    }

    @Test
    void handleValidation_formatsFieldErrorsAndFallbackMessage() {
        Map<String, ExceptionTestCase> testCases = Map.of(
            "multiple field errors", () -> {
                BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "image");
                bindingResult.addError(new FieldError("image", "fileName", "must be present"));
                bindingResult.addError(new FieldError("image", "owner", "must be present"));
                assertResponse(handler.handleValidation(validationException(bindingResult), request), 400,
                    "fileName: must be present; owner: must be present");
            },
            "no field errors", () -> assertResponse(handler.handleValidation(
                validationException(new BeanPropertyBindingResult(new Object(), "image")), request), 400,
                "Validation failed"));
        runTestCases(testCases);
    }

    @Test
    void handleS3_mapsKnownAndUnknownStatuses() {
        Map<String, ExceptionTestCase> testCases = Map.of(
            "known status", () -> assertResponse(handler.handleS3(s3Exception(404, "object missing"), request),
                404, "Image storage error: object missing"),
            "unknown status", () -> assertResponse(handler.handleS3(s3Exception(599, "storage unavailable"), request),
                502, "Image storage error: storage unavailable"));
        runTestCases(testCases);
    }

    @Test
    void handleDynamoDb_mapsKnownAndUnknownStatuses() {
        Map<String, ExceptionTestCase> testCases = Map.of(
            "known status", () -> assertResponse(handler.handleDynamoDb(dynamoException(409, "conflict"), request),
                409, "Image metadata storage error: conflict"),
            "unknown status", () -> assertResponse(handler.handleDynamoDb(dynamoException(599, "database unavailable"), request),
                502, "Image metadata storage error: database unavailable"));
        runTestCases(testCases);
    }

    @Test
    void handleAwsService_mapsKnownAndUnknownStatuses() {
        Map<String, ExceptionTestCase> testCases = Map.of(
            "known status", () -> assertResponse(handler.handleAwsService(awsException(429, "throttled"), request),
                429, "AWS error: throttled"),
            "unknown status", () -> assertResponse(handler.handleAwsService(awsException(599, "service unavailable"), request),
                502, "AWS error: service unavailable"));
        runTestCases(testCases);
    }

    @Test
    void handleSdkClient_returnsServiceUnavailableResponse() {
        runTestCases(Map.of("client unavailable", () -> assertResponse(
            handler.handleSdkClient(SdkClientException.create("connection refused"), request),
            503, "Could not reach AWS: connection refused")));
    }

    @Test
    void handleGeneric_returnsInternalServerErrorResponse() {
        runTestCases(Map.of("unexpected failure", () -> assertResponse(
            handler.handleGeneric(new IllegalStateException("unexpected"), request),
            500, "An unexpected error occurred")));
    }

    private void assertResponse(org.springframework.http.ResponseEntity<ErrorResponse> response,
                                int status, String message) {
        assertEquals(status, response.getStatusCode().value());
        assertEquals(status, response.getBody().getStatus());
        assertEquals(message, response.getBody().getMessage());
        assertEquals("/api/images", response.getBody().getPath());
        assertEquals("trace-id", response.getBody().getTraceId());
    }

    private MethodArgumentNotValidException validationException(BeanPropertyBindingResult bindingResult) {
        try {
            MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("validationMarker", String.class), -1);
            return new MethodArgumentNotValidException(parameter, bindingResult);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    @SuppressWarnings("unused")
    private void validationMarker(String value) {
    }

    private S3Exception s3Exception(int status, String message) {
        S3Exception exception = mock(S3Exception.class);
        when(exception.statusCode()).thenReturn(status);
        when(exception.awsErrorDetails()).thenReturn(AwsErrorDetails.builder().errorMessage(message).build());
        return exception;
    }

    private DynamoDbException dynamoException(int status, String message) {
        DynamoDbException exception = mock(DynamoDbException.class);
        when(exception.statusCode()).thenReturn(status);
        when(exception.awsErrorDetails()).thenReturn(AwsErrorDetails.builder().errorMessage(message).build());
        return exception;
    }

    private AwsServiceException awsException(int status, String message) {
        AwsServiceException exception = mock(AwsServiceException.class);
        when(exception.statusCode()).thenReturn(status);
        when(exception.awsErrorDetails()).thenReturn(AwsErrorDetails.builder().errorMessage(message).build());
        return exception;
    }

    private void runTestCases(Map<String, ExceptionTestCase> testCases) {
        for (Map.Entry<String, ExceptionTestCase> testCase : testCases.entrySet()) {
            reset(request);
            put(TraceIdFilter.TRACE_ID_MDC_KEY, "trace-id");
            when(request.getDescription(false)).thenReturn("uri=/api/images");
            try {
                testCase.getValue().run();
            } catch (AssertionError assertionError) {
                throw new AssertionError("Test case failed: " + testCase.getKey(), assertionError);
            }
        }
    }

    @FunctionalInterface
    private interface ExceptionTestCase {
        void run();
    }
}