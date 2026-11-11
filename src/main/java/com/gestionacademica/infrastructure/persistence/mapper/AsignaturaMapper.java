package com.gestionacademica.infrastructure.persistence.mapper;

import com.gestionacademica.domain.model.Asignatura;
import com.gestionacademica.infrastructure.persistence.entity.AsignaturaEntity;

public final class AsignaturaMapper {
    private AsignaturaMapper() {}
    public static Asignatura toDomain(AsignaturaEntity e) {
        return new Asignatura(e.getId(), e.getNombre(), e.getNombreCompleto(), e.getDescripcion(),
                e.getAreaConocimiento(), e.getCarrera(), e.getNumeroCreditos(), e.getContenidoTematico(),
                e.getSemestre(), e.getProfesor());
    }
    public static AsignaturaEntity toEntity(Asignatura a) {
        return new AsignaturaEntity(a.getId(), a.getNombre(), a.getNombreCompleto(), a.getDescripcion(),
                a.getAreaConocimiento(), a.getCarrera(), a.getNumeroCreditos(), a.getContenidoTematico(),
                a.getSemestre(), a.getProfesor());
    }
}
