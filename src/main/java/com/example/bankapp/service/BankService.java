package com.example.bankapp.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bankapp.dto.CustomerRequest;
import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.BankException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)          // default for every method: read-only transaction
public class BankService {

   private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;

    // ==========================================
    // BANK TRANSFER
    // ==========================================

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, BigDecimal amount) {
        validateMoneyOperation(accountNumber, amount);
        customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Could not credit account: " + accountNumber);
        }

        return saveMoneyTransaction("DEPOSIT", accountNumber, amount, "Cash deposit");
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, BigDecimal amount) {
        validateMoneyOperation(accountNumber, amount);
        customerRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + accountNumber + ".");
        }

        return saveMoneyTransaction(accountNumber, "WITHDRAWAL", amount, "Cash withdrawal");
    }

    private void validateMoneyOperation(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
    }

    private BankTransaction saveMoneyTransaction(String from, String to, BigDecimal amount, String description) {
        BankTransaction transaction = new BankTransaction();
        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction transfer(TransferRequest request) {

        String from = request.getFromAccount();
        String to = request.getToAccount();
        BigDecimal amount = request.getAmount();

        // Check same account
        if (from.equals(to)) {
            throw new BankException(
                    "Sender and receiver accounts cannot be the same."
            );
        }

        // Check positive amount
        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero."
            );
        }

        // Find sender
        Customer sender = customerRepository
                .findByAccountNumber(from)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Sender account not found: " + from
                ));

        // Find receiver
        customerRepository.findByAccountNumber(to)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Receiver account not found: " + to
                ));

        // Debit sender
        int debited = customerRepository.debit(from, amount);

        if (debited == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in sender account."
            );
        }

        // Simulate a failure to test rollback
        if (request.isSimulateFailure()) {
            throw new RuntimeException(
                    "SIMULATED FAILURE: Testing transaction rollback."
            );
        }

        // Credit receiver
        int credited = customerRepository.credit(to, amount);

        if (credited == 0) {
            throw new AccountNotFoundException(
                    "Could not credit receiver account."
            );
        }

        // Create successful transaction record
        BankTransaction transaction = new BankTransaction();

        transaction.setFromAccount(from);
        transaction.setToAccount(to);
        transaction.setAmount(amount);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Bank transfer");

        return transactionRepository.save(transaction);
    }

    // ==========================================
    // CREATE CUSTOMER
    // ==========================================

    @Transactional
    public Customer createCustomer(CustomerRequest request) {

        if (customerRepository.existsByAccountNumber(
                request.getAccountNumber())) {
            throw new IllegalArgumentException(
                    "Account number already exists."
            );
        }

        if (customerRepository.existsByEmail(
                request.getEmail())) {
            throw new IllegalArgumentException(
                    "Email already exists."
            );
        }

        Customer customer = new Customer();

        customer.setAccountNumber(request.getAccountNumber());
        customer.setFullName(request.getFullName());
        customer.setEmail(request.getEmail());
        customer.setBalance(request.getBalance());

        return customerRepository.save(customer);
    }

    // ==========================================
    // GET ALL CUSTOMERS
    // ==========================================

    public List<Customer> getCustomers() {
        return customerRepository.findAllOrdered();
    }

    // ==========================================
    // GET CUSTOMER
    // ==========================================

    public Customer getCustomer(String accountNumber) {

        return customerRepository
                .findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(
                        "Account not found: " + accountNumber
                ));
    }

    // ==========================================
    // GET TRANSACTION HISTORY
    // ==========================================

    public List<BankTransaction> getHistory(
            String accountNumber) {

        return transactionRepository
                .findByFromAccountOrToAccountOrderByCreatedAtDesc(
                        accountNumber,
                        accountNumber
                );
    }

    // ==========================================
    // GET LATEST TRANSACTIONS
    // ==========================================

    public List<BankTransaction> getLatestTransactions() {

        return transactionRepository
                .findTop10ByOrderByCreatedAtDesc();
    }

    // ==========================================
    // TOTAL BANK BALANCE
    // ==========================================

    public BigDecimal getTotalBalance() {
        return customerRepository.getTotalBalance();
    }

    // ==========================================
    // CUSTOMER COUNT
    // ==========================================

    public long getCustomerCount() {
        return customerRepository.countAccounts();
    }

    // ==========================================
    // SUCCESSFUL TRANSACTION COUNT
    // ==========================================

    public long getSuccessfulTransactionCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.SUCCESS
        );
    }

    // ==========================================
    // FAILED TRANSACTION COUNT
    // ==========================================

    public long getFailedTransactionCount() {
        return transactionRepository.countByStatus(
                TransactionStatus.FAILED
        );
    }

    // ==========================================
    // SEARCH CUSTOMERS
    // ==========================================

    public List<Customer> searchCustomers(String name) {
        return customerRepository.searchByName(name);
    }

    // ==========================================
    // LARGE TRANSFERS - TASK 12
    // ==========================================

    public List<BankTransaction> getLargeTransfers(BigDecimal minimumAmount) {

        return transactionRepository.findLargeTransfers(
                minimumAmount
        );
    }

    // ==========================================
    // TOTAL MONEY SENT BY ACCOUNT - TASK 12
    // ==========================================

    public BigDecimal getTotalSent(String accountNumber) {

        return transactionRepository.getTotalSentByAccount(
                accountNumber,
                TransactionStatus.SUCCESS
        );
    } 
//
}
