package com.banking.system.account.domain.port.out;

import com.banking.system.account.domain.model.value_object.AccountAlias;

/**
 * Port for generating random account aliases.
 * Implementation resides in infrastructure layer.
 */
public interface AccountAliasGenerator {
    /**
     * Generates a random, human-readable account alias.
     * Format: {adjective}.{noun}.{2-3 digits}
     * Example: "happy.tree.42", "blue.sky.789"
     */
    AccountAlias generate();
}