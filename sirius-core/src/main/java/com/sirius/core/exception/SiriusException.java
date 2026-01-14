package com.sirius.core.exception;

import java.util.Map;

public class SiriusException extends RuntimeException {

    private final SiriusErrorCode code;
    private final Map<String, Object> details;

    public SiriusException(SiriusErrorCode code, String message) {
        super(message);
        this.code = code;
        this.details = Map.of();
    }

    public SiriusException(SiriusErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public SiriusException(SiriusErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = Map.of();
    }

    public SiriusErrorCode code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
