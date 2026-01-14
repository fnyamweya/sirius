package com.sirius.core.exception;

public class SiriusException extends RuntimeException {
    
    public SiriusException(String message) {
        super(message);
    }
    
    public SiriusException(String message, Throwable cause) {
        super(message, cause);
    }
}
