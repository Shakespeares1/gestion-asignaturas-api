package com.gestionacademica.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Resolves forwarded client addresses only when the immediate peer belongs to
 * an explicitly configured trusted proxy network. Otherwise it uses the socket
 * peer address and ignores all client-supplied forwarding headers.
 */
@Component
public final class ClientIpResolver {
    private static final int MAX_FORWARDED_HEADER_LENGTH = 512;
    private static final int MAX_FORWARDED_HOPS = 20;

    private final Environment environment;
    private final List<Cidr> trustedProxyCidrs;

    public ClientIpResolver(Environment environment) {
        this.environment = environment;
        this.trustedProxyCidrs = environment.acceptsProfiles(Profiles.of("prod"))
                ? parseCidrs(environment.getProperty("app.proxy.trusted-cidrs", ""))
                : List.of();
    }

    public String resolve(HttpServletRequest request) {
        String peerText = parseIpLiteral(request.getRemoteAddr())
                .map(InetAddress::getHostAddress)
                .orElse("unknown");

        if (!environment.acceptsProfiles(Profiles.of("prod"))) return peerText;

        Optional<InetAddress> peer = parseIpLiteral(request.getRemoteAddr());
        if (peer.isEmpty() || !isTrustedProxy(peer.get())) return peerText;

        List<InetAddress> forwarded = parseForwardedChain(request.getHeader("X-Forwarded-For"));
        if (forwarded == null) return peerText;

        // Proxies append addresses to X-Forwarded-For. Walk from the trusted
        // side and stop at the first untrusted hop, ignoring any spoofed prefix.
        for (int index = forwarded.size() - 1; index >= 0; index--) {
            InetAddress address = forwarded.get(index);
            if (!isTrustedProxy(address)) return address.getHostAddress();
        }
        return peerText;
    }

    private boolean isTrustedProxy(InetAddress address) {
        return trustedProxyCidrs.stream().anyMatch(cidr -> cidr.contains(address));
    }

    private static List<Cidr> parseCidrs(String value) {
        if (value == null || value.isBlank()) return List.of();
        List<Cidr> cidrs = new ArrayList<>();
        for (String item : value.split(",", -1)) {
            if (item.isBlank()) continue;
            cidrs.add(Cidr.parse(item.strip()));
        }
        return List.copyOf(cidrs);
    }

    private static List<InetAddress> parseForwardedChain(String header) {
        if (header == null || header.isBlank() || header.length() > MAX_FORWARDED_HEADER_LENGTH) return null;
        String[] values = header.split(",", -1);
        if (values.length == 0 || values.length > MAX_FORWARDED_HOPS) return null;
        List<InetAddress> addresses = new ArrayList<>(values.length);
        for (String value : values) {
            Optional<InetAddress> address = parseIpLiteral(value);
            if (address.isEmpty()) return null;
            addresses.add(address.get());
        }
        return addresses;
    }

    private static Optional<InetAddress> parseIpLiteral(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String candidate = value.strip();
        if (candidate.length() > 64 || candidate.indexOf('%') >= 0
                || !candidate.matches("[0-9a-fA-F:.]+")) return Optional.empty();
        if (candidate.indexOf(':') < 0) {
            String[] octets = candidate.split("\\.", -1);
            if (octets.length != 4) return Optional.empty();
            byte[] bytes = new byte[4];
            try {
                for (int index = 0; index < octets.length; index++) {
                    int octet = Integer.parseInt(octets[index]);
                    if (octet < 0 || octet > 255) return Optional.empty();
                    bytes[index] = (byte) octet;
                }
                return Optional.of(InetAddress.getByAddress(bytes));
            } catch (NumberFormatException | UnknownHostException invalidAddress) {
                return Optional.empty();
            }
        }
        try {
            InetAddress parsed = InetAddress.getByName(candidate);
            return parsed.getAddress().length == 16 ? Optional.of(parsed) : Optional.empty();
        } catch (UnknownHostException invalidAddress) {
            return Optional.empty();
        }
    }

    private record Cidr(byte[] network, int prefixLength) {
        private static Cidr parse(String value) {
            String[] parts = value.split("/", -1);
            if (parts.length > 2) throw new IllegalArgumentException("CIDR de proxy inválido");
            InetAddress address = parseIpLiteral(parts[0])
                    .orElseThrow(() -> new IllegalArgumentException("CIDR de proxy inválido"));
            int maxPrefix = address.getAddress().length * 8;
            int prefix = maxPrefix;
            if (parts.length == 2) {
                try {
                    prefix = Integer.parseInt(parts[1]);
                } catch (NumberFormatException invalidPrefix) {
                    throw new IllegalArgumentException("Prefijo CIDR de proxy inválido", invalidPrefix);
                }
            }
            if (prefix < 0 || prefix > maxPrefix) {
                throw new IllegalArgumentException("Prefijo CIDR de proxy fuera de rango");
            }
            return new Cidr(address.getAddress(), prefix);
        }

        private boolean contains(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) return false;
            int remaining = prefixLength;
            for (int index = 0; index < network.length; index++) {
                int bits = Math.min(remaining, 8);
                int mask = bits == 0 ? 0 : (0xff << (8 - bits)) & 0xff;
                if (((candidate[index] & 0xff) & mask) != ((network[index] & 0xff) & mask)) return false;
                remaining -= bits;
            }
            return true;
        }
    }
}
