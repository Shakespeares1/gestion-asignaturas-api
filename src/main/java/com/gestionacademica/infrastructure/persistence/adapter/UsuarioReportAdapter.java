package com.gestionacademica.infrastructure.persistence.adapter;

import com.gestionacademica.application.dto.ReportePage;
import com.gestionacademica.application.port.output.UsuarioReportPort;
import com.gestionacademica.domain.model.EstadoCuenta;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.infrastructure.persistence.entity.UsuarioEntity;
import com.gestionacademica.infrastructure.persistence.mapper.UsuarioMapper;
import com.gestionacademica.infrastructure.persistence.repository.SpringDataUsuarioRepository;
import com.gestionacademica.infrastructure.persistence.repository.UsuarioReportSpecifications;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class UsuarioReportAdapter implements UsuarioReportPort {
    private final SpringDataUsuarioRepository repository;

    public UsuarioReportAdapter(SpringDataUsuarioRepository repository) { this.repository = repository; }

    @Override
    public ReportePage<Usuario> buscarPorRolEstado(RolUsuario role, EstadoCuenta status, int page, int size) {
        return map(repository.findAll(UsuarioReportSpecifications.rolEstado(role, status), pageable(page, size)));
    }

    @Override
    public ReportePage<Usuario> buscarPorFechaCreacion(Instant desde, Instant hastaExclusivo, int page, int size) {
        return map(repository.findAll(UsuarioReportSpecifications.fechaCreacion(desde, hastaExclusivo), pageable(page, size)));
    }

    private static PageRequest pageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    private static ReportePage<Usuario> map(Page<UsuarioEntity> page) {
        return new ReportePage<>(page.getContent().stream().map(UsuarioMapper::toDomain).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
