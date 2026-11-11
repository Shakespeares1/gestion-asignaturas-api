package com.gestionacademica.infrastructure.web;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/** Per-process fixed-window limiter. Stored keys are digests, never raw emails. */
@Component
public final class RequestRateLimiter {
    private static final int MAX_BUCKETS = 50_000;
    private final AtomicLong calls = new AtomicLong();
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Semaphore bucketCapacity = new Semaphore(MAX_BUCKETS);
    private final Clock clock;
    private final byte[] digestKey;

    public RequestRateLimiter() {
        this(Clock.systemUTC(), new SecureRandom().generateSeed(32));
    }

    RequestRateLimiter(Clock clock) {
        this(clock, new SecureRandom().generateSeed(32));
    }

    private RequestRateLimiter(Clock clock, byte[] digestKey) {
        this.clock = clock;
        this.digestKey = digestKey.clone();
    }

    public void check(RateLimitedOperation operation, String remoteAddress, String account) {
        checkIp(operation, remoteAddress);
        checkAccount(operation, remoteAddress, account);
    }

    public void checkIp(RateLimitedOperation operation, String remoteAddress) {
        long now = clock.millis();
        if (buckets.size() >= MAX_BUCKETS) {
            removeExpired(now);
        }
        String ip = digest(valueOrUnknown(remoteAddress));
        checkWindow(operation.name() + ":IP:" + ip, operation.ipLimit, operation.ipWindow.toMillis(), now);
        cleanupIfNeeded(now);
    }

    public void checkAccount(RateLimitedOperation operation, String remoteAddress, String account) {
        long now = clock.millis();
        if (operation.ipAccountLimit > 0 && account != null && !account.isBlank()) {
            String normalized = account.strip().toLowerCase(Locale.ROOT);
            String pair = digest(valueOrUnknown(remoteAddress) + "\0" + normalized);
            checkWindow(operation.name() + ":PAIR:" + pair, operation.ipAccountLimit,
                    operation.ipAccountWindow.toMillis(), now);
        }
        cleanupIfNeeded(now);
    }

    private void cleanupIfNeeded(long now) {
        if ((calls.incrementAndGet() & 255L) == 0L || buckets.size() > MAX_BUCKETS) {
            removeExpired(now);
        }
    }

    private void checkWindow(String scope, int limit, long windowMillis, long now) {
        long start = Math.floorDiv(now, windowMillis) * windowMillis;
        long expiresAt = start + windowMillis;
        String key = scope + ':' + start;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> newBucket(expiresAt, now));
        int observed = bucket.count.incrementAndGet();
        if (observed > limit) {
            long retry = Math.max(1, (bucket.expiresAtMillis - now + 999) / 1000);
            throw new RateLimitExceededException(retry);
        }
    }

    private Bucket newBucket(long expiresAt, long now) {
        if (!bucketCapacity.tryAcquire()) {
            removeExpired(now);
            if (!bucketCapacity.tryAcquire()) throw new RateLimitExceededException(60);
        }
        return new Bucket(expiresAt);
    }

    private void removeExpired(long now) {
        buckets.forEach((key, bucket) -> {
            if (bucket.expiresAtMillis <= now && buckets.remove(key, bucket)) bucketCapacity.release();
        });
    }

    private static String valueOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value.strip();
    }

    private String digest(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(digestKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException impossible) {
            throw new IllegalStateException("HMAC-SHA256 no está disponible", impossible);
        }
    }

    private static final class Bucket {
        private final AtomicInteger count = new AtomicInteger();
        private final long expiresAtMillis;
        private Bucket(long expiresAtMillis) { this.expiresAtMillis = expiresAtMillis; }
    }
}
