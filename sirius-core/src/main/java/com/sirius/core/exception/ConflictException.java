package com.sirius.core.exception;

import java.util.Map;

public class ConflictException extends SiriusException {

    public ConflictException(String message) {
        super(SiriusErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(SiriusErrorCode.CONFLICT, message, details);
    }
}
