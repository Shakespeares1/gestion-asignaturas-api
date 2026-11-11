package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.domain.model.Asignatura;

public record AsignaturaResponse(Long id, String nombre, String nombreCompleto, String descripcion,
        String areaConocimiento, String carrera, int numeroCreditos, String contenidoTematico,
        int semestre, String profesor) {
    public static AsignaturaResponse from(Asignatura a) {
        return new AsignaturaResponse(a.getId(), a.getNombre(), a.getNombreCompleto(), a.getDescripcion(),
                a.getAreaConocimiento(), a.getCarrera(), a.getNumeroCreditos(), a.getContenidoTematico(),
                a.getSemestre(), a.getProfesor());
    }
}
