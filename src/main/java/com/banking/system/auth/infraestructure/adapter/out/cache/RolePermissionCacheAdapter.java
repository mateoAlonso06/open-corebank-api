package com.banking.system.auth.infraestructure.adapter.out.cache;

import com.banking.system.auth.domain.model.Role;
import com.banking.system.auth.domain.model.enums.RoleName;
import com.banking.system.auth.domain.port.out.RolePermissionCachePort;
import com.banking.system.auth.domain.port.out.RoleRepositoryPort;
import com.banking.system.auth.infraestructure.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RolePermissionCacheAdapter implements RolePermissionCachePort {

    private final RoleRepositoryPort roleRepository;

    @Override
    @Cacheable(value = CacheConfig.ROLE_PERMISSIONS_CACHE, key = "#roleName")
    public Set<String> getPermissionsForRole(String roleName) {
        log.debug("Loading permissions for role: {} (cache miss)", roleName);
        try {
            RoleName roleNameEnum = RoleName.valueOf(roleName);
            return roleRepository.findByName(roleNameEnum)
                    .map(Role::getPermissionCodes) // Eager fetch permissions when loading the role
                    .orElse(Collections.emptySet());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role name: {}", roleName);
            return Collections.emptySet();
        }
    }

    @Override
    @CacheEvict(value = CacheConfig.ROLE_PERMISSIONS_CACHE, key = "#roleName")
    public void evictRole(String roleName) {
        log.info("Evicting cache for role: {}", roleName);
    }

    @Override
    @CacheEvict(value = CacheConfig.ROLE_PERMISSIONS_CACHE, allEntries = true)
    public void evictAll() {
        log.info("Evicting all role permissions cache");
    }
}