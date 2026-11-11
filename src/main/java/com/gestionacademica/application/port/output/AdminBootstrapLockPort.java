package com.gestionacademica.application.port.output;

/** Bloquea transaccionalmente la operación de bootstrap para evitar carreras. */
public interface AdminBootstrapLockPort {
    void bloquearInicializacion();
}
