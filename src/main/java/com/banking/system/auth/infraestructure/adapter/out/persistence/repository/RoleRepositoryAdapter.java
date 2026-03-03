package com.banking.system.auth.infraestructure.adapter.out.persistence.repository;

import com.banking.system.auth.domain.model.Role;
import com.banking.system.auth.domain.model.enums.RoleName;
import com.banking.system.auth.domain.port.out.RoleRepositoryPort;
import com.banking.system.auth.infraestructure.adapter.out.mapper.RoleJpaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepositoryPort {

    private final SpringDataRoleRepository springDataRoleRepository;

    @Override
    public Optional<Role> findById(UUID id) {
        return springDataRoleRepository.findById(id)
                .map(RoleJpaMapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(RoleName name) {
        return springDataRoleRepository.findByName(name)
                .map(RoleJpaMapper::toDomain);
    }
}
