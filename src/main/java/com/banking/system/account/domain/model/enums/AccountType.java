package com.banking.system.account.domain.model.enums;

public enum AccountType {
    SAVINGS,
    CHECKING,
    INVESTMENT;

    public String getNumericCode() {
        return switch (this) {
            case SAVINGS -> "01";
            case CHECKING -> "02";
            case INVESTMENT -> "03";
        };
    }
}
