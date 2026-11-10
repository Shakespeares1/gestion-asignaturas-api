
package com.gestionacademica.domain.model;

public class Asignatura {

    private Long id;
    private String nombre;
    private String nombreCompleto;
    private String descripcion;
    private String areaConocimiento;
    private String carrera;
    private int numeroCreditos;
    private String contenidoTematico;
    private int semestre;
    private String profesor;

    public Asignatura(
            Long id,
            String nombre,
            String nombreCompleto,
            String descripcion,
            String areaConocimiento,
            String carrera,
            int numeroCreditos,
            String contenidoTematico,
            int semestre,
            String profesor) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre es obligatorio");
        }

        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre completo es obligatorio");
        }

        if (numeroCreditos <= 0) {
            throw new IllegalArgumentException(
                    "Los créditos deben ser positivos");
        }

        if (semestre <= 0) {
            throw new IllegalArgumentException(
                    "El semestre debe ser positivo");
        }

        this.id = id;
        this.nombre = nombre;
        this.nombreCompleto = nombreCompleto;
        this.descripcion = descripcion;
        this.areaConocimiento = areaConocimiento;
        this.carrera = carrera;
        this.numeroCreditos = numeroCreditos;
        this.contenidoTematico = contenidoTematico;
        this.semestre = semestre;
        this.profesor = profesor;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getAreaConocimiento() {
        return areaConocimiento;
    }

    public String getCarrera() {
        return carrera;
    }

    public int getNumeroCreditos() {
        return numeroCreditos;
    }

    public String getContenidoTematico() {
        return contenidoTematico;
    }

    public int getSemestre() {
        return semestre;
    }

    public String getProfesor() {
        return profesor;
    }
}
