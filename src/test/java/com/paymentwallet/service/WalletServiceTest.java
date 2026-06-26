package com.paymentwallet.service;

import com.paymentwallet.dto.response.WalletResponse;
import com.paymentwallet.entity.User;
import com.paymentwallet.entity.Wallet;
import com.paymentwallet.exception.ResourceNotFoundException;
import com.paymentwallet.exception.WalletInactiveException;
import com.paymentwallet.repository.UserRepository;
import com.paymentwallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService Unit Tests")
class WalletServiceTest {

    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .fullName("Jane Doe")
                .email("jane@example.com")
                .phone("+9876543210")
                .enabled(true)
                .build();

        wallet = Wallet.builder()
                .id(1L)
                .walletNumber("WLTXYZ987654321")
                .balance(new BigDecimal("500.00"))
                .active(true)
                .user(user)
                .build();
    }

    @Test
    @DisplayName("Should return wallet for authenticated user's email")
    void getWalletByEmail_Success() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWalletByEmail("jane@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getWalletNumber()).isEqualTo("WLTXYZ987654321");
        assertThat(response.getBalance()).isEqualByComparingTo("500.00");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user not found")
    void getWalletByEmail_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWalletByEmail("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should deactivate wallet successfully")
    void deactivateWallet_Success() {
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.deactivateWallet("jane@example.com");

        assertThat(response.isActive()).isFalse();
        verify(walletRepository).save(argThat(w -> !w.isActive()));
    }

    @Test
    @DisplayName("Should activate wallet successfully")
    void activateWallet_Success() {
        wallet.setActive(false);
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletResponse response = walletService.activateWallet("jane@example.com");

        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw WalletInactiveException when validating inactive wallet")
    void validateWalletActive_InactiveWallet_ThrowsException() {
        wallet.setActive(false);

        assertThatThrownBy(() -> walletService.validateWalletActive(wallet))
                .isInstanceOf(WalletInactiveException.class);
    }

    @Test
    @DisplayName("Should not throw when validating active wallet")
    void validateWalletActive_ActiveWallet_NoException() {
        walletService.validateWalletActive(wallet);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when balance is too low")
    void validateSufficientBalance_InsufficientFunds_ThrowsException() {
        assertThatThrownBy(() ->
                walletService.validateSufficientBalance(wallet, new BigDecimal("1000.00")))
                .isInstanceOf(com.paymentwallet.exception.InsufficientBalanceException.class);
    }

    @Test
    @DisplayName("Should not throw when balance is sufficient")
    void validateSufficientBalance_SufficientFunds_NoException() {
        walletService.validateSufficientBalance(wallet, new BigDecimal("100.00"));
    }
}
