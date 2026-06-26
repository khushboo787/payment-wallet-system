package com.paymentwallet.dto.response;

import com.paymentwallet.enums.TransactionStatus;
import com.paymentwallet.enums.TransactionType;
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
public class TransactionResponse {

    private Long id;
    private String referenceId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String sourceWalletNumber;
    private String destinationWalletNumber;
    private BigDecimal balanceAfterTransaction;
    private LocalDateTime createdAt;
}
