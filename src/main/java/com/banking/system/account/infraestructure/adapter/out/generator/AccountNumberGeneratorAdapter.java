package com.banking.system.account.infraestructure.adapter.out.generator;

import com.banking.system.account.domain.model.value_object.AccountNumber;
import com.banking.system.account.domain.model.enums.AccountType;
import com.banking.system.account.domain.port.out.AccountNumberGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccountNumberGeneratorAdapter implements AccountNumberGenerator {

    @Override
    public AccountNumber generate(AccountType accountType) {
        String typePrefix = accountType.getNumericCode();
        String uuidDigits = extractDigitsFromUuid(UUID.randomUUID(), 18);
        String baseNumber = typePrefix + uuidDigits;
        String verifier = AccountNumber.calculateVerifierForBase(baseNumber);
        String fullAcountNumber = baseNumber + verifier;
        return new AccountNumber(fullAcountNumber);
    }

    /**
     * Extracts the specified number of digits from a UUID.
     * Takes hex chars and converts to numeric representation.
     * <p>
     *
     * Ejemplo:
     * Si el UUID es 550e8400-e29b-41d4-a716-446655440000 y necesitas 18 dígitos:
     * UUID sin guiones: 550e8400e29b41d4a716446655440000
     * Procesamiento: 5, 5, 0, e→14%10=4, 8, 4, 0, 0, etc.
     * Resultado: primeros 18 dígitos procesados
     */
    private String extractDigitsFromUuid(UUID uuid, int digitCount) {
        String uuidString = uuid.toString().replace("-", "");
        StringBuilder digits = new StringBuilder();

        for (int i = 0; i < uuidString.length() && digits.length() < digitCount; i++) {
            char c = uuidString.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                // Convert hex char to numeric (a=10, b=11, etc., mod 10)
                int value = Character.digit(c, 16) % 10;
                digits.append(value);
            }
        }

        return digits.toString();
    }
}
