package com.paymentwallet.service;

import com.paymentwallet.dto.request.AddMoneyRequest;
import com.paymentwallet.dto.request.TransferRequest;
import com.paymentwallet.dto.response.TransactionResponse;
import com.paymentwallet.entity.Transaction;
import com.paymentwallet.entity.User;
import com.paymentwallet.entity.Wallet;
import com.paymentwallet.enums.TransactionStatus;
import com.paymentwallet.enums.TransactionType;
import com.paymentwallet.exception.ResourceNotFoundException;
import com.paymentwallet.kafka.PaymentEvent;
import com.paymentwallet.kafka.PaymentEventProducer;
import com.paymentwallet.repository.TransactionRepository;
import com.paymentwallet.repository.UserRepository;
import com.paymentwallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PaymentEventProducer eventProducer;

    @CacheEvict(value = "walletBalance", allEntries = true)
    @Transactional
    public TransactionResponse addMoney(String email, AddMoneyRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        walletService.validateWalletActive(wallet);

        String referenceId = generateReferenceId();
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        Transaction transaction = Transaction.builder()
                .referenceId(referenceId)
                .amount(request.getAmount())
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet top-up")
                .destinationWallet(wallet)
                .balanceAfterTransaction(wallet.getBalance())
                .build();
        transaction = transactionRepository.save(transaction);

        PaymentEvent event = buildEvent(transaction, null, wallet.getWalletNumber());
        eventProducer.sendPaymentNotification(event);
        eventProducer.sendPaymentCompleted(event);

        log.info("Money added to wallet: {} | amount: {} | referenceId: {}",
                wallet.getWalletNumber(), request.getAmount(), referenceId);

        return mapToResponse(transaction);
    }

    @CacheEvict(value = "walletBalance", allEntries = true)
    @Transactional
    public TransactionResponse transfer(String email, TransferRequest request) {
        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet sourceWallet = walletRepository.findByUserId(sender.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", sender.getId()));

        Wallet destinationWallet = walletRepository.findByWalletNumber(request.getDestinationWalletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "walletNumber", request.getDestinationWalletNumber()));

        walletService.validateWalletActive(sourceWallet);
        walletService.validateWalletActive(destinationWallet);
        walletService.validateSufficientBalance(sourceWallet, request.getAmount());

        if (sourceWallet.getWalletNumber().equals(destinationWallet.getWalletNumber())) {
            throw new IllegalArgumentException("Cannot transfer to the same wallet");
        }

        String referenceId = generateReferenceId();

        sourceWallet.setBalance(sourceWallet.getBalance().subtract(request.getAmount()));
        destinationWallet.setBalance(destinationWallet.getBalance().add(request.getAmount()));
        walletRepository.save(sourceWallet);
        walletRepository.save(destinationWallet);

        Transaction transaction = Transaction.builder()
                .referenceId(referenceId)
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .description(request.getDescription() != null ? request.getDescription() : "Wallet transfer")
                .sourceWallet(sourceWallet)
                .destinationWallet(destinationWallet)
                .balanceAfterTransaction(sourceWallet.getBalance())
                .build();
        transaction = transactionRepository.save(transaction);

        PaymentEvent event = buildEvent(transaction, sourceWallet.getWalletNumber(), destinationWallet.getWalletNumber());
        eventProducer.sendPaymentNotification(event);
        eventProducer.sendPaymentCompleted(event);

        log.info("Transfer completed: {} -> {} | amount: {} | referenceId: {}",
                sourceWallet.getWalletNumber(), destinationWallet.getWalletNumber(),
                request.getAmount(), referenceId);

        return mapToResponse(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        return transactionRepository.findAllByWalletId(wallet.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReferenceId(String referenceId) {
        Transaction transaction = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "referenceId", referenceId));
        return mapToResponse(transaction);
    }

    private String generateReferenceId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private PaymentEvent buildEvent(Transaction txn, String source, String destination) {
        return PaymentEvent.builder()
                .referenceId(txn.getReferenceId())
                .sourceWalletNumber(source)
                .destinationWalletNumber(destination)
                .amount(txn.getAmount())
                .type(txn.getType())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .timestamp(LocalDateTime.now())
                .build();
    }

    public TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .referenceId(txn.getReferenceId())
                .amount(txn.getAmount())
                .type(txn.getType())
                .status(txn.getStatus())
                .description(txn.getDescription())
                .sourceWalletNumber(txn.getSourceWallet() != null ? txn.getSourceWallet().getWalletNumber() : null)
                .destinationWalletNumber(txn.getDestinationWallet() != null ? txn.getDestinationWallet().getWalletNumber() : null)
                .balanceAfterTransaction(txn.getBalanceAfterTransaction())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
