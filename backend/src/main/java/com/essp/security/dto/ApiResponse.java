package com.essp.security.dto;

import org.slf4j.MDC;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String correlationId;

    public ApiResponse() {
        this.correlationId = MDC.get("correlationId");
    }

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.correlationId = MDC.get("correlationId");
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.correlationId = MDC.get("correlationId");
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> ok(String message) {
        return new ApiResponse<>(true, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
