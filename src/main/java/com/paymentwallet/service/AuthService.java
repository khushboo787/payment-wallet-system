package com.paymentwallet.service;

import com.paymentwallet.dto.request.LoginRequest;
import com.paymentwallet.dto.request.RegisterRequest;
import com.paymentwallet.dto.response.AuthResponse;
import com.paymentwallet.entity.User;
import com.paymentwallet.entity.Wallet;
import com.paymentwallet.exception.DuplicateResourceException;
import com.paymentwallet.repository.UserRepository;
import com.paymentwallet.repository.WalletRepository;
import com.paymentwallet.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone already registered: " + request.getPhone());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .enabled(true)
                .build();
        user = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .walletNumber(generateWalletNumber())
                .balance(BigDecimal.ZERO)
                .active(true)
                .user(user)
                .build();
        wallet = walletRepository.save(wallet);

        String token = tokenProvider.generateTokenFromEmail(user.getEmail());

        log.info("New user registered: {} with wallet: {}", user.getEmail(), wallet.getWalletNumber());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .walletNumber(wallet.getWalletNumber())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow();

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .walletNumber(wallet.getWalletNumber())
                .build();
    }

    private String generateWalletNumber() {
        String walletNumber;
        do {
            walletNumber = "WLT" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        } while (walletRepository.existsByWalletNumber(walletNumber));
        return walletNumber;
    }
}
