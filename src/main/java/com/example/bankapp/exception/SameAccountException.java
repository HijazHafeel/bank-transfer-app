package com.example.bankapp.exception;

public class SameAccountException extends BankException {

    public SameAccountException(String message) {
        super(message);
    }
}