package model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Expense {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1);

    private final String id;
    private final String description;
    private final double amount;
    private final User paidBy;
    private final SplitType splitType;
    private final List<Split> splits;

    public Expense(String description, double amount, User paidBy, SplitType splitType, List<Split> splits) {
        this.id = "E" + SEQUENCE.getAndIncrement();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splitType = splitType;
        this.splits = splits;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public List<Split> getSplits() {
        return splits;
    }
}
