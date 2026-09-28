package com.harvesthub.desktop.api;

/** Thrown by ApiClient on network failure or a non-2xx HTTP response. */
public class ApiException extends RuntimeException {
    private final int statusCode;

    public ApiException(String message) {
        this(message, -1);
    }

    public ApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
