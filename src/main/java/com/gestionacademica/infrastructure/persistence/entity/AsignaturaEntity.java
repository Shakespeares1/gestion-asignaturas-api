package com.gestionacademica.infrastructure.persistence.entity;

import jakarta.persistence.*;
import com.gestionacademica.domain.model.Asignatura;

@Entity
@Table(name = "asignaturas", indexes = {
        @Index(name = "idx_asignaturas_semestre", columnList = "semestre"),
        @Index(name = "idx_asignaturas_numero_creditos", columnList = "numero_creditos")
})
@org.hibernate.annotations.Check(name = "ck_asignaturas_numero_creditos_positivo", constraints = "numero_creditos > 0")
@org.hibernate.annotations.Check(name = "ck_asignaturas_semestre_positivo", constraints = "semestre > 0")
public class AsignaturaEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = Asignatura.MAX_NOMBRE) private String nombre;
    @Column(name = "nombre_completo", nullable = false, length = Asignatura.MAX_NOMBRE_COMPLETO) private String nombreCompleto;
    @Column(columnDefinition = "text") private String descripcion;
    @Column(name = "area_conocimiento", length = Asignatura.MAX_AREA_CONOCIMIENTO) private String areaConocimiento;
    @Column(length = Asignatura.MAX_CARRERA) private String carrera;
    @Column(name = "numero_creditos", nullable = false) private int numeroCreditos;
    @Column(name = "contenido_tematico", columnDefinition = "text") private String contenidoTematico;
    @Column(nullable = false) private int semestre;
    @Column(length = Asignatura.MAX_PROFESOR) private String profesor;

    protected AsignaturaEntity() {}

    public AsignaturaEntity(Long id, String nombre, String nombreCompleto, String descripcion,
            String areaConocimiento, String carrera, int numeroCreditos, String contenidoTematico,
            int semestre, String profesor) {
        this.id = id; this.nombre = nombre; this.nombreCompleto = nombreCompleto;
        this.descripcion = descripcion; this.areaConocimiento = areaConocimiento; this.carrera = carrera;
        this.numeroCreditos = numeroCreditos; this.contenidoTematico = contenidoTematico;
        this.semestre = semestre; this.profesor = profesor;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getDescripcion() { return descripcion; }
    public String getAreaConocimiento() { return areaConocimiento; }
    public String getCarrera() { return carrera; }
    public int getNumeroCreditos() { return numeroCreditos; }
    public String getContenidoTematico() { return contenidoTematico; }
    public int getSemestre() { return semestre; }
    public String getProfesor() { return profesor; }
}
