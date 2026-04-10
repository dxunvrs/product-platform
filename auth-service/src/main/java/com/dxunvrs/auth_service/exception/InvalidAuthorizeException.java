package com.dxunvrs.auth_service.exception;

public class InvalidAuthorizeException extends RuntimeException {
    public InvalidAuthorizeException(String message) {
        super(message);
    }
}
