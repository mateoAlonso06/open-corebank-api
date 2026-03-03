package com.banking.system.transaction.domain.exception;

import com.banking.system.common.domain.exception.BusinessRuleException;

import java.math.BigDecimal;

public class MonthlyLimitExceededException extends BusinessRuleException {

    public MonthlyLimitExceededException(BigDecimal limit, BigDecimal accumulated, BigDecimal attempted) {
        super(
                String.format(
                        "Monthly limit exceeded: limit=%s, accumulated this month=%s, attempted=%s",
                        limit.toPlainString(), accumulated.toPlainString(), attempted.toPlainString()
                ),
                "MONTHLY_LIMIT_EXCEEDED",
                "Monthly transaction limit exceeded"
        );
    }
}
