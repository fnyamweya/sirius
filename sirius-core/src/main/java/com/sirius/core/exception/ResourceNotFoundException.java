package com.sirius.core.exception;

public class ResourceNotFoundException extends SiriusException {
    
    public ResourceNotFoundException(String message) {
        super(SiriusErrorCode.NOT_FOUND, message);
    }

    public ResourceNotFoundException(String message, java.util.Map<String, Object> details) {
        super(SiriusErrorCode.NOT_FOUND, message, details);
    }
}
