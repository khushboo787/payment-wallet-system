package com.paymentwallet.service;

import com.paymentwallet.dto.response.WalletResponse;
import com.paymentwallet.entity.User;
import com.paymentwallet.entity.Wallet;
import com.paymentwallet.exception.ResourceNotFoundException;
import com.paymentwallet.exception.WalletInactiveException;
import com.paymentwallet.repository.UserRepository;
import com.paymentwallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "walletBalance", key = "#email")
    @Transactional(readOnly = true)
    public WalletResponse getWalletByEmail(String email) {
        log.debug("Fetching wallet for user: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        return mapToResponse(wallet);
    }

    @Cacheable(value = "walletBalance", key = "#walletNumber")
    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumber(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "walletNumber", walletNumber));
        return mapToResponse(wallet);
    }

    @CacheEvict(value = "walletBalance", allEntries = true)
    @Transactional
    public WalletResponse deactivateWallet(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        wallet.setActive(false);
        wallet = walletRepository.save(wallet);
        log.info("Wallet deactivated: {}", wallet.getWalletNumber());
        return mapToResponse(wallet);
    }

    @CacheEvict(value = "walletBalance", allEntries = true)
    @Transactional
    public WalletResponse activateWallet(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userId", user.getId()));

        wallet.setActive(true);
        wallet = walletRepository.save(wallet);
        log.info("Wallet activated: {}", wallet.getWalletNumber());
        return mapToResponse(wallet);
    }

    public void validateWalletActive(Wallet wallet) {
        if (!wallet.isActive()) {
            throw new WalletInactiveException(wallet.getWalletNumber());
        }
    }

    public void validateSufficientBalance(Wallet wallet, BigDecimal amount) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new com.paymentwallet.exception.InsufficientBalanceException(wallet.getBalance(), amount);
        }
    }

    public WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .walletNumber(wallet.getWalletNumber())
                .balance(wallet.getBalance())
                .active(wallet.isActive())
                .userId(wallet.getUser().getId())
                .userFullName(wallet.getUser().getFullName())
                .userEmail(wallet.getUser().getEmail())
                .createdAt(wallet.getCreatedAt())
                .build();
    }
}
