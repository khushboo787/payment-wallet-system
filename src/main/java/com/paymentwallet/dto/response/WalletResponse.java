package com.paymentwallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {

    private Long id;
    private String walletNumber;
    private BigDecimal balance;
    private boolean active;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private LocalDateTime createdAt;
}
