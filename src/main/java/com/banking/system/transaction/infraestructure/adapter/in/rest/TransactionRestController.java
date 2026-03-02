package com.banking.system.transaction.infraestructure.adapter.in.rest;

import com.banking.system.common.domain.PageRequest;
import com.banking.system.common.domain.dto.PagedResult;
import com.banking.system.transaction.application.dto.result.TransactionResult;
import com.banking.system.transaction.application.usecase.*;
import com.banking.system.transaction.infraestructure.adapter.in.rest.dto.request.DepositMoneyRequest;
import com.banking.system.transaction.infraestructure.adapter.in.rest.dto.request.WithdrawMoneyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transactions", description = "Operations for deposits, withdrawals, and transaction history")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionRestController {
    private final DepositUseCase depositUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetTransactionByIdUseCase getTransactionByIdUseCase;
    private final GetAllTransactionsByAccountUseCase getAllTransactionsByAccountUseCase;
    private final GetAllTransactionsByCustomerUseCase getAllTransactionsByCustomerUseCase;
    private final GetTransactionByReferenceNumber getTransactionByReferenceNumber;

    @Operation(
            summary = "Create deposit",
            description = "Deposits money into the specified bank account. The authenticated user must be the account owner and have approved KYC status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deposit completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "Account or customer not found"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (inactive account, KYC not approved, or currency mismatch)")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_DEPOSIT')")
    @PostMapping("/accounts/{accountId}/deposits")
    public ResponseEntity<Void> createDeposit(
            @RequestBody @Valid DepositMoneyRequest request,
            @Parameter(description = "Target account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID accountId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId) {
        var command = request.toCommand();
        depositUseCase.deposit(command, accountId, userId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get transaction history for customer",
            description = "Retrieves the paginated transaction history only for the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Authenticated user not authorized to view these transactions"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (KYC not approved)")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW_OWN')")
    @GetMapping("/me")
    public ResponseEntity<PagedResult<TransactionResult>> getAllTransactionsByCustomer(@AuthenticationPrincipal UUID userId, Pageable pageable) {
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        var result = getAllTransactionsByCustomerUseCase.getAllTransactionsByCustomer(userId, pageRequest);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Withdraw money",
            description = "Withdraws money from the specified bank account. Validates sufficient funds, active account status, and KYC approval."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Withdrawal completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (validation failed)"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "Account or customer not found"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (insufficient funds, inactive account, KYC not approved, or currency mismatch)")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_WITHDRAW')")
    @PostMapping("/accounts/{accountId}/withdrawals")
    public ResponseEntity<Void> withdrawMoney(
            @RequestBody @Valid WithdrawMoneyRequest request,
            @Parameter(description = "Source account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID accountId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId) {
        var command = request.toCommand();
        withdrawUseCase.withdraw(command, accountId, userId);

        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get transaction history",
            description = "Retrieves the paginated transaction history for the specified account. Only the account owner can access this information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Account or customer not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (KYC not approved)")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW_OWN')")
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<PagedResult<TransactionResult>> getAllTransactionsByAccount(
            @Parameter(description = "Account ID to retrieve transactions from", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID accountId,
            @Parameter(hidden = true)
            @AuthenticationPrincipal UUID userId,
            @ParameterObject Pageable pageable) {
        var pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        var result = getAllTransactionsByAccountUseCase.getAllTransactionsByAccountId(accountId, userId, pageRequest);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get my transaction by ID",
            description = "Retrieves the details of a specific transaction by its ID. Only returns transactions where the authenticated user is the account owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Transaction does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (KYC not approved)")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW_OWN')")
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResult> getTransactionByIdForCustomer(@PathVariable @NotNull UUID transactionId,
                                                                           @AuthenticationPrincipal UUID userId) {
        var transactionResult = getTransactionByIdUseCase.getTransactionById(transactionId, userId);
        return ResponseEntity.ok(transactionResult);
    }

    @Operation(
            summary = "Get transaction by reference number",
            description = "Retrieves the details of a specific transaction by its reference number. Only returns transactions where the authenticated user is the account owner."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Transaction does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Transaction not found")
    })
    @PreAuthorize("hasAuthority('TRANSACTION_VIEW_OWN')")
    @GetMapping("/reference/{referenceNumber}")
    public ResponseEntity<TransactionResult> getTransactionByReferenceNumberForCustomer(@PathVariable @NotNull String referenceNumber,
                                                                                        @AuthenticationPrincipal UUID userId) {
        var transactionResult = getTransactionByReferenceNumber.getTransactionByReferenceNumber(referenceNumber, userId);
        return ResponseEntity.ok(transactionResult);
    }
}
