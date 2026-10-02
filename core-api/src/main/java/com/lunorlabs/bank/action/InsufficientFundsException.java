package com.lunorlabs.bank.action;

/** Thrown inside the ledger transaction when the atomic debit fails; the transaction rolls back. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException() { super("Insufficient funds"); }
}
