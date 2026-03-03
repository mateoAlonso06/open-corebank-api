package com.banking.system.auth.infraestructure.adapter.out.mapper;

import com.banking.system.auth.domain.model.value_object.Email;
import com.banking.system.auth.domain.model.value_object.Password;
import com.banking.system.auth.domain.model.Role;
import com.banking.system.auth.domain.model.User;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.RoleJpaEntity;
import com.banking.system.auth.infraestructure.adapter.out.persistence.entity.UserJpaEntity;

/**
 * Manual mapper responsible for bidirectional conversion between {@link User} domain entities
 * and {@link UserJpaEntity} persistence entities.
 * <p>
 * This implementation handles the translation of Value Objects (Email, Password) to their
 * primitive representations and vice versa. Uses the domain's factory methods to ensure
 * proper object reconstruction.
 * </p>
 */
public class UserJpaMapper {

    private UserJpaMapper() {
    }

    /**
     * Converts a JPA entity from the persistence layer to a domain entity.
     * <p>
     * Uses {@link User reconsitute} factory method to rebuild the domain aggregate
     * with all its invariants. Wraps primitive String values into Value Objects
     * (Email, Password) during the conversion. The password is reconstituted from
     * its hashed form using {@link Password#fromHash}.
     * </p>
     *
     * @param entity the JPA entity from database
     * @return the reconstituted domain entity, or null if entity is null
     */
    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;

        Role role = RoleJpaMapper.toDomain(entity.getRole());

        return User.reconstitute(
                entity.getId(),
                new Email(entity.getEmail()),
                Password.fromHash(entity.getPasswordHash()),
                entity.getStatus(),
                role,
                entity.isTwoFactorEnabled()
        );
    }

    /**
     * Converts a domain entity to a JPA entity for persistence.
     * <p>
     * Extracts the primitive values from Value Objects (Email, Password) to store
     * them in the database. Audit fields like createdAt, updatedAt, and lastLoginAt
     * are managed by JPA lifecycle hooks and not set here.
     * </p>
     *
     * @param user       the domain entity to persist
     * @param roleEntity the JPA role entity to associate with the user
     * @return the JPA entity ready for persistence
     */
    public static UserJpaEntity toJpaEntity(User user, RoleJpaEntity roleEntity) {
        if (user == null) return null;

        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail().value());
        entity.setPasswordHash(user.getPassword().value());
        entity.setStatus(user.getStatus());
        entity.setRole(roleEntity);
        entity.setTwoFactorEnabled(user.isTwoFactorEnabled());

        return entity;
    }
}
