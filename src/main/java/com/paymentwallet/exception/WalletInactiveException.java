package com.paymentwallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class WalletInactiveException extends RuntimeException {

    public WalletInactiveException(String walletNumber) {
        super("Wallet is inactive: " + walletNumber);
    }
}
