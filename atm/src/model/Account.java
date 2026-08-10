package model;

import java.util.ArrayList;
import java.util.List;

public class Account {

    private final String accountNumber;
    private final String pin;
    private int balance;
    private final List<TransactionReceipt> transactionHistory = new ArrayList<>();

    public Account(String accountNumber, String pin, int balance) {
        this.accountNumber = accountNumber;
        this.pin = pin;
        this.balance = balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public boolean isPinCorrect(String candidatePin) {
        return pin.equals(candidatePin);
    }

    public int getBalance() {
        return balance;
    }

    public void debit(int amount) {
        balance -= amount;
    }

    public void credit(int amount) {
        balance += amount;
    }

    public void addTransaction(TransactionReceipt receipt) {
        transactionHistory.add(receipt);
    }

    public List<TransactionReceipt> getTransactionHistory() {
        return transactionHistory;
    }
}
