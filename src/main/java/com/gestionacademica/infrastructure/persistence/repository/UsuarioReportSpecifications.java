package com.gestionacademica.infrastructure.persistence.repository;

import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.infrastructure.persistence.entity.UsuarioEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

public final class UsuarioReportSpecifications {
    private UsuarioReportSpecifications() {}

    public static Specification<UsuarioEntity> rolEstado(RolUsuario role, EstadoCuenta status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) predicates.add(cb.equal(root.get("role"), role));
            if (status != null) predicates.add(cb.equal(root.get("accountStatus"), status));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<UsuarioEntity> fechaCreacion(Instant desdeInclusive, Instant hastaExclusivo) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (desdeInclusive != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desdeInclusive));
            if (hastaExclusivo != null) predicates.add(cb.lessThan(root.get("fechaCreacion"), hastaExclusivo));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
