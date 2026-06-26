package com.paymentwallet.controller;

import com.paymentwallet.dto.response.ApiResponse;
import com.paymentwallet.dto.response.WalletResponse;
import com.paymentwallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Wallet management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/me")
    @Operation(summary = "Get my wallet", description = "Returns the wallet details for the authenticated user")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse wallet = walletService.getWalletByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", wallet));
    }

    @GetMapping("/{walletNumber}")
    @Operation(summary = "Get wallet by number", description = "Returns wallet details for a given wallet number")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByNumber(
            @PathVariable String walletNumber) {
        WalletResponse wallet = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(ApiResponse.success("Wallet retrieved successfully", wallet));
    }

    @PutMapping("/deactivate")
    @Operation(summary = "Deactivate wallet", description = "Deactivates the authenticated user's wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> deactivateWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse wallet = walletService.deactivateWallet(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet deactivated successfully", wallet));
    }

    @PutMapping("/activate")
    @Operation(summary = "Activate wallet", description = "Activates the authenticated user's wallet")
    public ResponseEntity<ApiResponse<WalletResponse>> activateWallet(
            @AuthenticationPrincipal UserDetails userDetails) {
        WalletResponse wallet = walletService.activateWallet(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Wallet activated successfully", wallet));
    }
}
