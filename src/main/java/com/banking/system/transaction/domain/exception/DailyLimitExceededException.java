package com.banking.system.transaction.domain.exception;

import com.banking.system.common.domain.exception.BusinessRuleException;

import java.math.BigDecimal;

public class DailyLimitExceededException extends BusinessRuleException {

    public DailyLimitExceededException(BigDecimal limit, BigDecimal accumulated, BigDecimal attempted) {
        super(
                String.format(
                        "Daily limit exceeded: limit=%s, accumulated today=%s, attempted=%s",
                        limit.toPlainString(), accumulated.toPlainString(), attempted.toPlainString()
                ),
                "DAILY_LIMIT_EXCEEDED",
                "Daily transaction limit exceeded"
        );
    }
}
