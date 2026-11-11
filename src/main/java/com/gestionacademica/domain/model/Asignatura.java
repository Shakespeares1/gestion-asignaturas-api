
package com.gestionacademica.domain.model;

public class Asignatura {

    public static final int MAX_NOMBRE = 120;
    public static final int MAX_NOMBRE_COMPLETO = 200;
    public static final int MAX_AREA_CONOCIMIENTO = 120;
    public static final int MAX_CARRERA = 120;
    public static final int MAX_PROFESOR = 160;

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

        validarLongitud(nombre, MAX_NOMBRE, "El nombre");
        validarLongitud(nombreCompleto, MAX_NOMBRE_COMPLETO, "El nombre completo");
        validarLongitud(areaConocimiento, MAX_AREA_CONOCIMIENTO, "El área de conocimiento");
        validarLongitud(carrera, MAX_CARRERA, "La carrera");
        validarLongitud(profesor, MAX_PROFESOR, "El profesor");

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

    private static void validarLongitud(String valor, int maxima, String campo) {
        if (valor != null && valor.length() > maxima) {
            throw new IllegalArgumentException(campo + " no puede superar " + maxima + " caracteres");
        }
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
