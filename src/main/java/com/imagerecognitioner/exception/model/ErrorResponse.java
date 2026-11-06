package com.imagerecognitioner.exception.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * ErrorResponse model for errornous HTTP responses.
 * ErrorResponse
 */
@Schema(description = "Standard error response returned when an API request fails.")
public class ErrorResponse {
    @Schema(description = "Timestamp when the error occurred.", format = "date-time")
    private final Instant timestamp = Instant.now();
    @Schema(description = "HTTP status code.", example = "404")
    private final int status;
    @Schema(description = "HTTP status reason phrase.", example = "Not Found")
    private final String error;
    @Schema(description = "Detailed explanation of the error.")
    private final String message;
    @Schema(description = "Request path that produced the error.", example = "/api/images/img-123")
    private final String path;
    @Schema(description = "Trace identifier for correlating the request in application logs.")
    private final String traceId;

    public ErrorResponse(int status, String error, String message, String path, String traceId) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.traceId = traceId;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public String getTraceId() { return traceId; }
}