package com.gestionacademica.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RequestRateLimiterTest {
    @Test
    void limitaPorIpYPorParIpCuentaSinPermitirBloqueoDesdeOtraIp() {
        RequestRateLimiter limiter = new RequestRateLimiter(new MutableClock());

        for (int i = 0; i < 8; i++) limiter.check(RateLimitedOperation.LOGIN, "192.0.2.10", "User@Example.org");
        RateLimitExceededException accountLimit = assertThrows(RateLimitExceededException.class,
                () -> limiter.check(RateLimitedOperation.LOGIN, "192.0.2.10", " user@example.org "));
        org.junit.jupiter.api.Assertions.assertTrue(accountLimit.getRetryAfterSeconds() > 0);

        assertDoesNotThrow(() -> limiter.check(RateLimitedOperation.LOGIN, "192.0.2.11", "user@example.org"));
    }

    @Test
    void limitaPorIpYPermiteNuevaVentanaAlExpirarElPeriodo() {
        MutableClock clock = new MutableClock();
        RequestRateLimiter limiter = new RequestRateLimiter(clock);

        for (int i = 0; i < 30; i++) limiter.check(RateLimitedOperation.LOGIN, "192.0.2.20", null);
        assertThrows(RateLimitExceededException.class,
                () -> limiter.check(RateLimitedOperation.LOGIN, "192.0.2.20", null));

        clock.advance(Duration.ofMinutes(10).plusMillis(1));
        assertDoesNotThrow(() -> limiter.check(RateLimitedOperation.LOGIN, "192.0.2.20", null));
    }

    @Test
    void aplicaVentanasConfiguradasARegistroRecuperacionYRestablecimiento() {
        for (RateLimitedOperation operation : RateLimitedOperation.values()) {
            RequestRateLimiter limiter = new RequestRateLimiter(new MutableClock());
            for (int i = 0; i < operation.ipLimit; i++) limiter.checkIp(operation, "198.51.100.8");
            assertThrows(RateLimitExceededException.class,
                    () -> limiter.checkIp(operation, "198.51.100.8"), operation.name());
        }

        for (RateLimitedOperation operation : new RateLimitedOperation[] {
                RateLimitedOperation.FORGOT_PASSWORD, RateLimitedOperation.REGISTER }) {
            RequestRateLimiter limiter = new RequestRateLimiter(new MutableClock());
            for (int i = 0; i < operation.ipAccountLimit; i++) {
                limiter.checkAccount(operation, "198.51.100.9", "person@example.org");
            }
            assertThrows(RateLimitExceededException.class,
                    () -> limiter.checkAccount(operation, "198.51.100.9", "person@example.org"), operation.name());
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis = new AtomicLong(Instant.parse("2026-11-11T00:00:00Z").toEpochMilli());
        void advance(Duration duration) { millis.addAndGet(duration.toMillis()); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(millis.get()); }
        @Override public long millis() { return millis.get(); }
    }
}
