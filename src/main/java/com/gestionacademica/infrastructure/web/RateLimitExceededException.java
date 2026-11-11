package com.gestionacademica.infrastructure.web;

public final class RateLimitExceededException extends RuntimeException {
    private final long retryAfterSeconds;

    RateLimitExceededException(long retryAfterSeconds) {
        super("Se excedió el límite de solicitudes. Intenta de nuevo más tarde.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() { return retryAfterSeconds; }
}
