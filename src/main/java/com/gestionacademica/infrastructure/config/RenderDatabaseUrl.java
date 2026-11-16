package com.gestionacademica.infrastructure.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Converts Render's native Postgres URL into the JDBC URL required by Spring JDBC. */
public final class RenderDatabaseUrl {
    private RenderDatabaseUrl() {}

    public static String toJdbcUrl(String configuredUrl) {
        if (configuredUrl == null || configuredUrl.isBlank()) {
            throw new IllegalArgumentException("DB_URL es obligatorio");
        }
        if (configuredUrl.startsWith("jdbc:postgresql:")) return configuredUrl;

        final URI uri;
        try {
            uri = URI.create(configuredUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("DB_URL de Render no es una URL PostgreSQL válida");
        }
        if (!("postgresql".equalsIgnoreCase(uri.getScheme()) || "postgres".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null || uri.getRawPath() == null
                || uri.getRawPath().length() < 2) {
            throw new IllegalArgumentException("DB_URL de Render debe incluir host y base de datos");
        }
        int port = uri.getPort() < 0 ? 5432 : uri.getPort();
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("DB_URL de Render contiene un puerto PostgreSQL inválido");
        }

        List<String> query = new ArrayList<>();
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            for (String parameter : uri.getRawQuery().split("&")) {
                if (!parameter.toLowerCase(java.util.Locale.ROOT).startsWith("sslmode=")) {
                    query.add(parameter);
                }
            }
        }
        query.add("sslmode=require");
        return "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath()
                + "?" + String.join("&", query);
    }
}
