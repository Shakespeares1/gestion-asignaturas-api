package com.gestionacademica.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Resolves client IPs without trusting X-Forwarded-For supplied by clients. */
@Component
public final class ClientIpResolver {
    private final Environment environment;

    public ClientIpResolver(Environment environment) {
        this.environment = environment;
    }

    public String resolve(HttpServletRequest request) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            Optional<String> renderClientIp = parseIpLiteral(request.getHeader("CF-Connecting-IP"));
            if (renderClientIp.isPresent()) return renderClientIp.get();
        }
        return request.getRemoteAddr();
    }

    private static Optional<String> parseIpLiteral(String header) {
        if (header == null || header.isBlank()) return Optional.empty();
        String value = header.strip();
        if (value.length() > 64 || value.contains(",") || value.contains(" ") || value.contains("\t")) {
            return Optional.empty();
        }
        boolean ipv4 = value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}");
        boolean ipv6 = value.indexOf(':') >= 0 && value.matches("[0-9a-fA-F:.]+");
        if (!ipv4 && !ipv6) return Optional.empty();
        if (ipv4 && Arrays.stream(value.split("\\."))
                .mapToInt(Integer::parseInt).anyMatch(octet -> octet > 255)) return Optional.empty();
        try {
            InetAddress parsed = InetAddress.getByName(value);
            return Optional.of(parsed.getHostAddress());
        } catch (UnknownHostException invalidAddress) {
            return Optional.empty();
        }
    }
}
