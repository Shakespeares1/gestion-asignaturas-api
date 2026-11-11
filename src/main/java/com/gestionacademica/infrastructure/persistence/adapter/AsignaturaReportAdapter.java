package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.application.port.output.AsignaturaReportPort;
import com.gestionacademica.domain.model.Asignatura;
import com.gestionacademica.infrastructure.persistence.mapper.AsignaturaMapper;
import com.gestionacademica.infrastructure.persistence.repository.AsignaturaReportSpecifications;
import com.gestionacademica.infrastructure.persistence.repository.SpringDataAsignaturaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class AsignaturaReportAdapter implements AsignaturaReportPort {
    private final SpringDataAsignaturaRepository repository;

    public AsignaturaReportAdapter(SpringDataAsignaturaRepository repository) { this.repository = repository; }

    @Override
    public ReportePage<Asignatura> buscarPorCarreraSemestreArea(String carrera, Integer semestre,
            String area, int page, int size) {
        return map(repository.findAll(AsignaturaReportSpecifications.carreraSemestreArea(carrera, semestre, area), pageable(page, size)));
    }

    @Override
    public ReportePage<Asignatura> buscarPorCreditosProfesorNombre(Integer min, Integer max,
            String profesor, String nombre, int page, int size) {
        return map(repository.findAll(AsignaturaReportSpecifications.creditosProfesorNombre(min, max, profesor, nombre), pageable(page, size)));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private static ReportePage<Asignatura> map(Page<com.gestionacademica.infrastructure.persistence.entity.AsignaturaEntity> page) {
        return new ReportePage<>(page.getContent().stream().map(AsignaturaMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
