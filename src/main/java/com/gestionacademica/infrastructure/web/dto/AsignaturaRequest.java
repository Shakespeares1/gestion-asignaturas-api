package com.gestionacademica.infrastructure.web.dto;

import com.gestionacademica.domain.model.Asignatura;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record AsignaturaRequest(
        @Schema(description = "Nombre corto", example = "Bases de datos") @NotBlank @Size(max = Asignatura.MAX_NOMBRE) String nombre,
        @Schema(description = "Nombre completo de la asignatura", example = "Fundamentos de bases de datos") @NotBlank @Size(max = Asignatura.MAX_NOMBRE_COMPLETO) String nombreCompleto,
        String descripcion,
        @Schema(example = "Ingeniería de software") @Size(max = Asignatura.MAX_AREA_CONOCIMIENTO) String areaConocimiento,
        @Schema(example = "Ingeniería de Sistemas") @Size(max = Asignatura.MAX_CARRERA) String carrera,
        @Schema(description = "Debe ser positivo", example = "4") @NotNull @Min(1) Integer numeroCreditos,
        String contenidoTematico,
        @Schema(description = "Debe ser positivo", example = "5") @NotNull @Min(1) Integer semestre,
        @Schema(example = "Ada Lovelace") @Size(max = Asignatura.MAX_PROFESOR) String profesor) {
    public Asignatura toDomain(Long id) {
        return new Asignatura(id, nombre, nombreCompleto, descripcion, areaConocimiento, carrera,
                numeroCreditos, contenidoTematico, semestre, profesor);
    }
}
