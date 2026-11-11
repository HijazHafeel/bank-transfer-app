package com.example.bankapp.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;

public interface BankTransactionRepository
        extends JpaRepository<BankTransaction, Long> {

                // Get the latest 10 transactions
    List<BankTransaction> findTop10ByOrderByCreatedAtDesc();

    // Get transaction history for an account
    List<BankTransaction>
    findByFromAccountOrToAccountOrderByCreatedAtDesc(
            String fromAccount,
            String toAccount
    );

    // Find transactions by status
    List<BankTransaction>
    findByStatusOrderByCreatedAtDesc(TransactionStatus status);

    // Count transactions by status
    long countByStatus(TransactionStatus status);

    // Find large transactions
   @Query("SELECT t FROM BankTransaction t " +
       "WHERE t.amount >= :amount " +
       "AND t.status = com.example.bankapp.model.TransactionStatus.SUCCESS " +
       "ORDER BY t.amount DESC")
List<BankTransaction> findLargeTransfers(
        @Param("amount") BigDecimal amount
);
    // Calculate total amount sent by an account
    @Query("SELECT COALESCE(SUM(t.amount), 0) " +
           "FROM BankTransaction t " +
           "WHERE t.fromAccount = :account " +
           "AND t.status = :status")
    BigDecimal getTotalSentByAccount(
            @Param("account") String account,
            @Param("status") TransactionStatus status
    );
}