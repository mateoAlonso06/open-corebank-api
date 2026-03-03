package com.banking.system.account.infraestructure.adapter.in.rest;

import com.banking.system.account.application.dto.result.AccountBalanceResult;
import com.banking.system.account.application.dto.result.AccountLimitResult;
import com.banking.system.account.application.dto.result.AccountPublicResult;
import com.banking.system.account.application.dto.result.AccountResult;
import com.banking.system.account.application.usecase.CreateAccountUseCase;
import com.banking.system.account.application.usecase.FindAccountByIdUseCase;
import com.banking.system.account.application.usecase.FindAllAccountsByUserId;
import com.banking.system.account.application.usecase.GetAccountBalanceUseCase;
import com.banking.system.account.application.usecase.SearchAccountByAliasUseCase;
import com.banking.system.transaction.application.usecase.GetAccountLimitsUseCase;
import com.banking.system.account.domain.model.enums.AccountType;
import com.banking.system.account.infraestructure.adapter.in.rest.dto.request.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/accounts")
@Tag(name = "Accounts", description = "Bank account management operations")
@SecurityRequirement(name = "Bearer Authentication")
public class  AccountRestController {

    private final CreateAccountUseCase createAccountUseCase;
    private final FindAccountByIdUseCase findAccountByIdUseCase;
    private final FindAllAccountsByUserId findAllAccountsByUserId;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;
    private final SearchAccountByAliasUseCase searchAccountByAliasUseCase;
    private final GetAccountLimitsUseCase getAccountLimitsUseCase;

    @Operation(
            summary = "Create a new bank account",
            description = "Creates a new bank account for the authenticated user. Requires KYC approval. Only one USD account is allowed per customer."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Role not authorized for this operation"),
            @ApiResponse(responseCode = "422", description = "KYC not approved or USD account limit reached")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_CREATE')")
    @PostMapping
    public ResponseEntity<AccountResult> createAccount(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid CreateAccountRequest request) {
        var command = request.toCommand();
        var result = createAccountUseCase.createAccount(command, userId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/v1/accounts/me/{id}")
                .buildAndExpand(result.id())
                .toUri();

        return ResponseEntity.created(location).body(result);
    }

    @Operation(
            summary = "Get my accounts",
            description = "Retrieves all bank accounts for the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "Customer profile not found for this user")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_OWN')")
    @GetMapping("/me")
    public ResponseEntity<List<AccountResult>> getAllAccountByUserId(
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        var results = findAllAccountsByUserId.findAll(userId);
        return ResponseEntity.ok().body(results);
    }

    @Operation(
            summary = "Get my account by ID",
            description = "Retrieves a specific bank account by ID for the authenticated user. Only returns accounts owned by the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_OWN')")
    @GetMapping("/me/{accountId}")
    public ResponseEntity<AccountResult> getAccountByIdForCustomer(
            @Parameter(description = "Account ID to retrieve", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotNull UUID accountId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        var result = findAccountByIdUseCase.findAccountByIdForCustomer(accountId, userId);
        return ResponseEntity.ok().body(result);
    }

    @Operation(
            summary = "Get account by ID",
            description = "Retrieves any bank account by ID. Only accessible by ADMIN or authorized roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Role not authorized for this operation"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_ALL')")
    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResult> getAccountById(
            @Parameter(description = "Account ID to retrieve", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotNull UUID accountId) {
        var result = findAccountByIdUseCase.findAccountById(accountId);
        return ResponseEntity.ok().body(result);
    }

    @Operation(
            summary = "Get account balance",
            description = "Retrieves the balance of a specific account owned by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Balance retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_OWN')")
    @GetMapping("/me/{accountId}/balance")
    public ResponseEntity<AccountBalanceResult> getAccountBalance(
            @Parameter(description = "Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotNull UUID accountId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        var result = getAccountBalanceUseCase.getBalance(accountId, userId);
        return ResponseEntity.ok().body(result);
    }

    @Operation(
            summary = "Get account transaction limits",
            description = "Retrieves the daily and monthly transaction limits and current usage for a specific account owned by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Limits retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Account does not belong to the authenticated user"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "422", description = "KYC not approved")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_OWN')")
    @GetMapping("/me/{accountId}/limits")
    public ResponseEntity<AccountLimitResult> getAccountLimits(
            @Parameter(description = "Account ID", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable @NotNull UUID accountId,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId) {
        var result = getAccountLimitsUseCase.getAccountLimits(accountId, userId);
        return ResponseEntity.ok().body(result);
    }

    @Operation(
            summary = "Search account by alias",
            description = "Searches for a bank account by its alias. Returns public information only (alias, owner name, currency, account type)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "404", description = "Account not found for the given alias")
    })
    @PreAuthorize("hasAuthority('ACCOUNT_VIEW_OWN')")
    @GetMapping("/search")
    public ResponseEntity<AccountPublicResult> searchByAlias(
            @Parameter(description = "Account alias to search", example = "happy.tree.42")
            @RequestParam @NotBlank String alias) {
        var result = searchAccountByAliasUseCase.searchByAlias(alias);
        return ResponseEntity.ok().body(result);
    }

    @Operation(
            summary = "Get account types",
            description = "Retrieves the list of available bank account types."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account types retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/types")
    public ResponseEntity<List<AccountType>> getAccountTypes() {
        return ResponseEntity.ok().body(List.of(AccountType.values()));
    }
}
