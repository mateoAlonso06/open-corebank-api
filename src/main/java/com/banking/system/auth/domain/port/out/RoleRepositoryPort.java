package com.banking.system.auth.domain.port.out;

import com.banking.system.auth.domain.model.Role;
import com.banking.system.auth.domain.model.enums.RoleName;

import java.util.Optional;
import java.util.UUID;

/**
 * Output port for Role persistence operations.
 */
public interface RoleRepositoryPort {

    /**
     * Finds a role by its unique identifier.
     *
     * @param id the role's UUID
     * @return an Optional containing the role if found, empty otherwise
     */
    Optional<Role> findById(UUID id);

    /**
     * Finds a role by its name.
     *
     * @param name the role name enum
     * @return an Optional containing the role if found, empty otherwise
     */
    Optional<Role> findByName(RoleName name);

    /**
     * Gets the default role for new users (CUSTOMER).
     *
     * @return the default CUSTOMER role with its permissions
     */
    default Role getDefaultRole() {
        return findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Default CUSTOMER role not found in database"));
    }
}
