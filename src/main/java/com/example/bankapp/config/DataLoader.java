
package com.example.bankapp.config;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) {

        // Avoid inserting duplicate sample customers
        if (customerRepository.countAccounts() == 0) {

            // Customer 1
            Customer customer1 = new Customer();

            customer1.setAccountNumber("100001");
            customer1.setFullName("Kamal Perera");
            customer1.setEmail("kamal@gmail.com");
            customer1.setBalance(
                    new BigDecimal("50000.00")
            );

            // Customer 2
            Customer customer2 = new Customer();

            customer2.setAccountNumber("100002");
            customer2.setFullName("Nimali Silva");
            customer2.setEmail("nimali@gmail.com");
            customer2.setBalance(
                    new BigDecimal("25000.00")
            );

            // Customer 3
            Customer customer3 = new Customer();

            customer3.setAccountNumber("100003");
            customer3.setFullName("Saman Fernando");
            customer3.setEmail("saman@gmail.com");
            customer3.setBalance(
                    new BigDecimal("10000.00")
            );

            // Save all customers
            customerRepository.save(customer1);
            customerRepository.save(customer2);
            customerRepository.save(customer3);

            System.out.println(
                    "Sample bank customers inserted successfully!"
            );
        } else {

            System.out.println(
                    "Customer records already exist. Skipping sample data."
            );
        }
    }
}
