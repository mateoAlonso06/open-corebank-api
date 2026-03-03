package com.banking.system.auth.infraestructure.adapter.in.rest;

import com.banking.system.auth.application.usecase.BlockUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Management", description = "Admin operations for managing user accounts")
@SecurityRequirement(name = "Bearer Authentication")
public class UserManagementRestController {

    private final BlockUserUseCase blockUserUseCase;

    @Operation(
            summary = "Block user",
            description = "Suspends a user account, preventing login. All active sessions are revoked immediately. " +
                    "Can be reversed with the unblock operation. Requires USER_BLOCK permission."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User blocked successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "422", description = "User is not in a blockable state")
    })
    @PreAuthorize("hasAuthority('USER_BLOCK')")
    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockUser(@PathVariable UUID id) {
        blockUserUseCase.blockUser(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Unblock user",
            description = "Restores access to a previously blocked user account. Requires USER_BLOCK permission."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User unblocked successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired JWT token"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "422", description = "User is not in a blocked state")
    })
    @PreAuthorize("hasAuthority('USER_BLOCK')")
    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockUser(@PathVariable UUID id) {
        blockUserUseCase.unblockUser(id);
        return ResponseEntity.noContent().build();
    }
}
