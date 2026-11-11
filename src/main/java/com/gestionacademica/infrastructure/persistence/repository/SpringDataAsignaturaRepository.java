package com.gestionacademica.infrastructure.persistence.repository;

import com.gestionacademica.infrastructure.persistence.entity.AsignaturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SpringDataAsignaturaRepository extends JpaRepository<AsignaturaEntity, Long>,
        JpaSpecificationExecutor<AsignaturaEntity> {}
