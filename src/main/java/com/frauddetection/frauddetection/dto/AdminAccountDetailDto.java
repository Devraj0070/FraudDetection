package com.frauddetection.frauddetection.dto;

import java.math.BigDecimal;

import com.frauddetection.frauddetection.entity.Account;

/**
 * Public administrative DTO for account details.
 * Embeds sanitized user details without sensitive credentials.
 */
public record AdminAccountDetailDto(
        Long id,
        String accountNumber,
        String accountType,
        BigDecimal balance,
        String status,
        AdminUserDetailDto user
) {
    public static AdminAccountDetailDto fromEntity(Account account) {
        if (account == null) {
            return null;
        }
        return new AdminAccountDetailDto(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance(),
                account.getStatus(),
                AdminUserDetailDto.fromEntity(account.getUser())
        );
    }
}
