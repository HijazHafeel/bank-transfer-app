package com.example.bankapp.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.bankapp.model.Customer;

import jakarta.transaction.Transactional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

     // Find a customer using account number
    Optional<Customer> findByAccountNumber(String accountNumber);

    // Check whether an account number already exists
    boolean existsByAccountNumber(String accountNumber);

    // Check whether an email already exists
    boolean existsByEmail(String email);

    // Count all customer accounts
    @Query("SELECT COUNT(c) FROM Customer c")
    long countAccounts();

    // Count customers using their email
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.email = :email")
    long countEmails(@Param("email") String email);

    // Get customers ordered by name
    @Query("SELECT c FROM Customer c ORDER BY c.fullName ASC")
    List<Customer> findAllOrdered();

    // Search customers by name
    @Query("SELECT c FROM Customer c " +
           "WHERE LOWER(c.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    // Calculate total money in all customer accounts
    @Query("SELECT COALESCE(SUM(c.balance), 0) FROM Customer c")
    BigDecimal getTotalBalance();

    // Native SQL query to count customers
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // Debit money from an account safely
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Customer c " +
           "SET c.balance = c.balance - :amount " +
           "WHERE c.accountNumber = :accountNumber " +
           "AND c.balance >= :amount")
    int debit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );

    // Credit money to an account
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Customer c " +
           "SET c.balance = c.balance + :amount " +
           "WHERE c.accountNumber = :accountNumber")
    int credit(
            @Param("accountNumber") String accountNumber,
            @Param("amount") BigDecimal amount
    );
}
