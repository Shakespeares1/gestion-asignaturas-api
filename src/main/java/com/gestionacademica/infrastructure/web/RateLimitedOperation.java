package com.gestionacademica.infrastructure.web;

import java.time.Duration;

enum RateLimitedOperation {
    LOGIN(30, Duration.ofMinutes(10), 8, Duration.ofMinutes(10)),
    FORGOT_PASSWORD(10, Duration.ofHours(1), 3, Duration.ofHours(1)),
    RESET_PASSWORD(12, Duration.ofMinutes(15), 0, Duration.ZERO),
    REGISTER(10, Duration.ofHours(1), 3, Duration.ofHours(1));

    final int ipLimit;
    final Duration ipWindow;
    final int ipAccountLimit;
    final Duration ipAccountWindow;

    RateLimitedOperation(int ipLimit, Duration ipWindow, int ipAccountLimit, Duration ipAccountWindow) {
        this.ipLimit = ipLimit;
        this.ipWindow = ipWindow;
        this.ipAccountLimit = ipAccountLimit;
        this.ipAccountWindow = ipAccountWindow;
    }
}
