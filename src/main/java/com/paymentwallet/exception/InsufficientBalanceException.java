package com.paymentwallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.math.BigDecimal;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(BigDecimal available, BigDecimal requested) {
        super(String.format("Insufficient balance. Available: %.2f, Requested: %.2f", available, requested));
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
