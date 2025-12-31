package com.college.bookmyslot.exception;

import java.time.LocalDateTime;

public class ApiError {
    private boolean success;
    private String message;

    public ApiError(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
