package com.sky.exception;

public class StatusNotExistException extends RuntimeException {
    public StatusNotExistException(String message) {
        super(message);
    }
}
