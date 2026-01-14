package com.sirius.api.tenant;

public final class SiriusRequestContextHolder {

    private static final ThreadLocal<SiriusRequestContext> CTX = new ThreadLocal<>();

    private SiriusRequestContextHolder() {
    }

    public static void set(SiriusRequestContext context) {
        CTX.set(context);
    }

    public static SiriusRequestContext getRequired() {
        SiriusRequestContext context = CTX.get();
        if (context == null) {
            throw new IllegalStateException("Missing SiriusRequestContext");
        }
        return context;
    }

    public static void clear() {
        CTX.remove();
    }
}
