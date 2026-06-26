package com.paymentwallet.repository;

import com.paymentwallet.entity.Transaction;
import com.paymentwallet.enums.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByWalletId(Long walletId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE (t.sourceWallet.id = :walletId OR t.destinationWallet.id = :walletId) AND t.status = :status ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByWalletIdAndStatus(Long walletId, TransactionStatus status, Pageable pageable);
}
