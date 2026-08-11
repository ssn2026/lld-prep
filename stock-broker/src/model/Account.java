package model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Account {

    private final String accountId;
    private double cashBalance;
    private final Map<String, Integer> holdings = new LinkedHashMap<>();

    public Account(String accountId, double initialCash) {
        this.accountId = accountId;
        this.cashBalance = initialCash;
    }

    public String getAccountId() {
        return accountId;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void debit(double amount) {
        cashBalance -= amount;
    }

    public void credit(double amount) {
        cashBalance += amount;
    }

    public void addHolding(String symbol, int quantity) {
        holdings.merge(symbol, quantity, Integer::sum);
    }

    public void removeHolding(String symbol, int quantity) {
        int remaining = holdings.getOrDefault(symbol, 0) - quantity;
        if (remaining == 0) {
            holdings.remove(symbol);
        } else {
            holdings.put(symbol, remaining);
        }
    }

    public int getHoldingQuantity(String symbol) {
        return holdings.getOrDefault(symbol, 0);
    }

    public Map<String, Integer> getHoldings() {
        return holdings;
    }
}
