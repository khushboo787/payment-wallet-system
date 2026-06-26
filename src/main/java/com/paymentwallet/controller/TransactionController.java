package com.paymentwallet.controller;

import com.paymentwallet.dto.request.AddMoneyRequest;
import com.paymentwallet.dto.request.TransferRequest;
import com.paymentwallet.dto.response.ApiResponse;
import com.paymentwallet.dto.response.TransactionResponse;
import com.paymentwallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction and money transfer endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/add-money")
    @Operation(summary = "Add money to wallet", description = "Credits money to the authenticated user's wallet")
    public ResponseEntity<ApiResponse<TransactionResponse>> addMoney(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddMoneyRequest request) {
        TransactionResponse response = transactionService.addMoney(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Money added successfully", response));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money", description = "Transfers money from the authenticated user's wallet to another wallet")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TransferRequest request) {
        TransactionResponse response = transactionService.transfer(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Transfer completed successfully", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get transaction history", description = "Returns paginated transaction history for the authenticated user's wallet")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<TransactionResponse> history = transactionService.getTransactionHistory(
                userDetails.getUsername(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved", history));
    }

    @GetMapping("/{referenceId}")
    @Operation(summary = "Get transaction by reference ID", description = "Returns a single transaction by its reference ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable String referenceId) {
        TransactionResponse transaction = transactionService.getTransactionByReferenceId(referenceId);
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully", transaction));
    }
}
