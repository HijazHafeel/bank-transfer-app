
package com.example.bankapp.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bankapp.dto.CustomerRequest;
import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import com.example.bankapp.service.TransactionAuditService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;
    private final TransactionAuditService auditService;

    @GetMapping("/deposit")
    public String depositPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "deposit";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber,
                         @RequestParam BigDecimal amount,
                         RedirectAttributes redirectAttributes) {
        try {
            BankTransaction transaction = bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed. Transaction ID: " + transaction.getId());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/deposit";
    }

    @GetMapping("/withdraw")
    public String withdrawPage(Model model) {
        model.addAttribute("customers", bankService.getCustomers());
        return "withdraw";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber,
                           @RequestParam BigDecimal amount,
                           RedirectAttributes redirectAttributes) {
        try {
            BankTransaction transaction = bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed. Transaction ID: " + transaction.getId());
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/withdraw";
    }

    // DASHBOARD
    @GetMapping("/")
    public String dashboard(
            @RequestParam(required = false) String keyword,
            Model model) {

        List<Customer> allCustomers = bankService.getCustomers();

        List<Customer> customers;

        if (keyword != null && !keyword.trim().isEmpty()) {
            customers = bankService.searchCustomers(keyword);
        } else {
            customers = allCustomers;
        }

        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customers", customers);
        model.addAttribute("keyword", keyword);

        model.addAttribute(
                "transactions",
                bankService.getLatestTransactions()
        );

        model.addAttribute(
                "totalBalance",
                bankService.getTotalBalance()
        );

        model.addAttribute(
                "totalCustomers",
                bankService.getCustomerCount()
        );

        model.addAttribute(
                "successCount",
                bankService.getSuccessfulTransactionCount()
        );

        model.addAttribute(
                "failedCount",
                bankService.getFailedTransactionCount()
        );

        model.addAttribute("transferRequest", new TransferRequest());
        model.addAttribute("customer", new CustomerRequest());

        return "bank";
    }

    // ADD CUSTOMER
    @PostMapping("/customers")
    public String addCustomer(
            @Valid @ModelAttribute("customer") CustomerRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Please enter valid customer information."
            );
            return "redirect:/";
        }

        try {
            bankService.createCustomer(request);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Customer account created successfully!"
            );

        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/";
    }

    // TRANSFER MONEY
    @PostMapping("/transfer")
    public String transfer(
            @Valid @ModelAttribute("transferRequest") TransferRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Invalid transfer details. Please check your inputs."
            );
            return "redirect:/";
        }

        try {
            BankTransaction transaction = bankService.transfer(request);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Transfer completed successfully. Transaction ID: "
                            + transaction.getId()
            );

        } catch (RuntimeException e) {

            // Record the failed transfer in a separate transaction.
            if (request.getAmount() != null
                    && request.getAmount().compareTo(BigDecimal.ZERO) > 0
                    && request.getFromAccount() != null
                    && !request.getFromAccount().isBlank()
                    && request.getToAccount() != null
                    && !request.getToAccount().isBlank()) {

                try {
                    auditService.recordFailure(
                            request.getFromAccount(),
                            request.getToAccount(),
                            request.getAmount(),
                            e.getMessage()
                    );
                } catch (RuntimeException auditError) {
                    System.err.println(
                            "Audit recording failed: "
                                    + auditError.getMessage()
                    );
                }
            }

            redirectAttributes.addFlashAttribute(
                    "error",
                    e.getMessage()
            );
        }

        return "redirect:/";
    }

    // ACCOUNT TRANSACTION HISTORY
    @GetMapping("/accounts/{accountNumber}/history")
    public String accountHistory(
            @PathVariable String accountNumber,
            Model model) {

        Customer customer = bankService.getCustomer(accountNumber);

        List<BankTransaction> transactions =
                bankService.getHistory(accountNumber);

        BigDecimal totalSent = bankService.getTotalSent(accountNumber);

        model.addAttribute("customer", customer);
        model.addAttribute("transactions", transactions);
        model.addAttribute("totalSent", totalSent);

        return "history";
    }

    // LARGE TRANSFERS
    @GetMapping("/large-transfers")
    public String largeTransfers(
            @RequestParam(defaultValue = "10000") BigDecimal min,
            Model model) {

        model.addAttribute(
                "transfers",
                bankService.getLargeTransfers(min)
        );

        model.addAttribute("min", min);

        return "large-transfers";
    }
}
