package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.port.output.AdminBootstrapLockPort;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("admin-bootstrap")
public final class AdminBootstrapLockAdapter implements AdminBootstrapLockPort {
    private static final long LOCK_KEY = 5_713_202_611_110_001L;
    private final JdbcTemplate jdbc;

    public AdminBootstrapLockAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void bloquearInicializacion() {
        jdbc.execute("select pg_advisory_xact_lock(" + LOCK_KEY + ")");
    }
}
