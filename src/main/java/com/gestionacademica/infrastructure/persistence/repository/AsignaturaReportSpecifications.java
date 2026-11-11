package com.gestionacademica.infrastructure.persistence.repository;

import com.gestionacademica.infrastructure.persistence.entity.AsignaturaEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

public final class AsignaturaReportSpecifications {
    private AsignaturaReportSpecifications() {}

    public static Specification<AsignaturaEntity> carreraSemestreArea(String carrera, Integer semestre, String area) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (carrera != null) predicates.add(cb.equal(cb.lower(root.get("carrera")), carrera.toLowerCase(Locale.ROOT)));
            if (semestre != null) predicates.add(cb.equal(root.get("semestre"), semestre));
            if (area != null) predicates.add(contains(cb, root.get("areaConocimiento"), area));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static Specification<AsignaturaEntity> creditosProfesorNombre(
            Integer min, Integer max, String profesor, String nombre) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (min != null) predicates.add(cb.greaterThanOrEqualTo(root.get("numeroCreditos"), min));
            if (max != null) predicates.add(cb.lessThanOrEqualTo(root.get("numeroCreditos"), max));
            if (profesor != null) predicates.add(contains(cb, root.get("profesor"), profesor));
            if (nombre != null) predicates.add(contains(cb, root.get("nombre"), nombre));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate contains(jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Expression<String> field, String value) {
        String escaped = value.toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
        return cb.like(cb.lower(field), "%" + escaped + "%", '\\');
    }
}
