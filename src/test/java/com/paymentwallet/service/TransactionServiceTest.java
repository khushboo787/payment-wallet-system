package com.paymentwallet.service;

import com.paymentwallet.dto.request.AddMoneyRequest;
import com.paymentwallet.dto.request.TransferRequest;
import com.paymentwallet.dto.response.TransactionResponse;
import com.paymentwallet.entity.Transaction;
import com.paymentwallet.entity.User;
import com.paymentwallet.entity.Wallet;
import com.paymentwallet.enums.TransactionStatus;
import com.paymentwallet.enums.TransactionType;
import com.paymentwallet.exception.InsufficientBalanceException;
import com.paymentwallet.exception.ResourceNotFoundException;
import com.paymentwallet.kafka.PaymentEventProducer;
import com.paymentwallet.repository.TransactionRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Unit Tests")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserRepository userRepository;
    @Mock private WalletService walletService;
    @Mock private PaymentEventProducer eventProducer;

    @InjectMocks
    private TransactionService transactionService;

    private User senderUser;
    private User receiverUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;
    private Transaction savedTransaction;

    @BeforeEach
    void setUp() {
        senderUser = User.builder().id(1L).email("sender@example.com").fullName("Sender").build();
        receiverUser = User.builder().id(2L).email("receiver@example.com").fullName("Receiver").build();

        senderWallet = Wallet.builder()
                .id(1L).walletNumber("WLTSENDER1234567")
                .balance(new BigDecimal("1000.00")).active(true).user(senderUser).build();

        receiverWallet = Wallet.builder()
                .id(2L).walletNumber("WLTRECEIVER12345")
                .balance(new BigDecimal("200.00")).active(true).user(receiverUser).build();

        savedTransaction = Transaction.builder()
                .id(1L).referenceId("TXN1234567890ABCD")
                .amount(new BigDecimal("200.00"))
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.SUCCESS)
                .sourceWallet(senderWallet)
                .destinationWallet(receiverWallet)
                .balanceAfterTransaction(new BigDecimal("800.00"))
                .build();
    }

    @Test
    @DisplayName("Should add money to wallet successfully")
    void addMoney_Success() {
        AddMoneyRequest request = new AddMoneyRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setDescription("Top-up");

        Transaction creditTxn = Transaction.builder()
                .id(2L).referenceId("TXN_CREDIT_001")
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.CREDIT)
                .status(TransactionStatus.SUCCESS)
                .destinationWallet(senderWallet)
                .balanceAfterTransaction(new BigDecimal("1500.00"))
                .build();

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        doNothing().when(walletService).validateWalletActive(senderWallet);
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(creditTxn);
        doNothing().when(eventProducer).sendPaymentNotification(any());
        doNothing().when(eventProducer).sendPaymentCompleted(any());

        TransactionResponse response = transactionService.addMoney("sender@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getAmount()).isEqualByComparingTo("500.00");
        assertThat(response.getType()).isEqualTo(TransactionType.CREDIT);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(walletRepository).save(senderWallet);
        verify(eventProducer).sendPaymentNotification(any());
    }

    @Test
    @DisplayName("Should transfer money between wallets successfully")
    void transfer_Success() {
        TransferRequest request = new TransferRequest();
        request.setDestinationWalletNumber("WLTRECEIVER12345");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Payment");

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("WLTRECEIVER12345")).thenReturn(Optional.of(receiverWallet));
        doNothing().when(walletService).validateWalletActive(any());
        doNothing().when(walletService).validateSufficientBalance(any(), any());
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);
        doNothing().when(eventProducer).sendPaymentNotification(any());
        doNothing().when(eventProducer).sendPaymentCompleted(any());

        TransactionResponse response = transactionService.transfer("sender@example.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(TransactionType.TRANSFER);
        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        verify(walletRepository, times(2)).save(any(Wallet.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when transferring to same wallet")
    void transfer_SameWallet_ThrowsException() {
        TransferRequest request = new TransferRequest();
        request.setDestinationWalletNumber("WLTSENDER1234567");
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("WLTSENDER1234567")).thenReturn(Optional.of(senderWallet));
        doNothing().when(walletService).validateWalletActive(any());
        doNothing().when(walletService).validateSufficientBalance(any(), any());

        assertThatThrownBy(() -> transactionService.transfer("sender@example.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot transfer to the same wallet");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for unknown destination wallet")
    void transfer_DestinationWalletNotFound_ThrowsException() {
        TransferRequest request = new TransferRequest();
        request.setDestinationWalletNumber("WLTNONEXISTENT000");
        request.setAmount(new BigDecimal("100.00"));

        when(userRepository.findByEmail("sender@example.com")).thenReturn(Optional.of(senderUser));
        when(walletRepository.findByUserId(1L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByWalletNumber("WLTNONEXISTENT000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.transfer("sender@example.com", request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
