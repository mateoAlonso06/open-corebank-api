package com.banking.system.auth.infraestructure.adapter.out.persistence.repository;

import com.banking.system.auth.domain.model.enums.RoleName;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {

    Optional<RoleJpaEntity> findByName(RoleName name);
}
