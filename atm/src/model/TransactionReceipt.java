package model;

import java.util.Map;

public class TransactionReceipt {

    private final TransactionType type;
    private final int amount;
    private final int balanceAfter;
    private final Map<Integer, Integer> denominationBreakdown;

    public TransactionReceipt(TransactionType type, int amount, int balanceAfter,
            Map<Integer, Integer> denominationBreakdown) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.denominationBreakdown = denominationBreakdown;
    }

    public TransactionType getType() {
        return type;
    }

    public int getAmount() {
        return amount;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    /** Null for deposits -- only withdrawals go through the denomination chain. */
    public Map<Integer, Integer> getDenominationBreakdown() {
        return denominationBreakdown;
    }
}
