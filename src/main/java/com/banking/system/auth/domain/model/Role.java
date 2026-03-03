package com.banking.system.auth.domain.model;

import com.banking.system.auth.domain.model.enums.RoleName;
import lombok.Getter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain entity representing a role in the system.
 * A role groups a set of permissions that define what actions a user can perform.
 */
@Getter
public class Role {
    private final UUID id;
    private final RoleName name;
    private final String description;
    private final Set<Permission> permissions;

    private Role(UUID id, RoleName name, String description, Set<Permission> permissions) {
        Objects.requireNonNull(name, "Role name cannot be null");
        Objects.requireNonNull(description, "Role description cannot be null");
        Objects.requireNonNull(permissions, "Role permissions cannot be null");

        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = Collections.unmodifiableSet(new HashSet<>(permissions));
    }

    /**
     * Reconstitutes a role from persistence with its associated permissions.
     */
    public static Role reconstitute(UUID id, RoleName name, String description, Set<Permission> permissions) {
        Objects.requireNonNull(id, "Role id cannot be null when reconstituting");
        return new Role(id, name, description, permissions);
    }

    /**
     * Checks if this role has a specific permission by its code.
     *
     * @param permissionCode the permission code to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(String permissionCode) {
        return permissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    /**
     * Returns all permission codes associated with this role.
     *
     * @return an unmodifiable set of permission codes
     */
    public Set<String> getPermissionCodes() {
        return permissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(name, role.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name.name();
    }
}