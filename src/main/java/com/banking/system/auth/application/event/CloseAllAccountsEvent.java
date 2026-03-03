package com.banking.system.auth.application.event;

import java.util.UUID;

public record CloseAllAccountsEvent(
        UUID userId
) {
}
