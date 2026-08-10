package command;

import exceptions.InsufficientFundsException;
import exceptions.InvalidAmountException;
import java.util.Map;
import model.Account;
import model.CashDispenser;
import model.TransactionReceipt;
import model.TransactionType;

public class WithdrawCommand implements TransactionCommand {

    private final Account account;
    private final int amount;
    private final CashDispenser cashDispenser;

    public WithdrawCommand(Account account, int amount, CashDispenser cashDispenser) {
        this.account = account;
        this.amount = amount;
        this.cashDispenser = cashDispenser;
    }

    @Override
    public TransactionReceipt execute() {
        if (amount <= 0) {
            throw new InvalidAmountException("Withdrawal amount must be positive");
        }
        if (amount > account.getBalance()) {
            throw new InsufficientFundsException(
                    "Insufficient balance: requested " + amount + ", available " + account.getBalance());
        }
        Map<Integer, Integer> breakdown = cashDispenser.dispense(amount);
        account.debit(amount);
        return new TransactionReceipt(TransactionType.WITHDRAWAL, amount, account.getBalance(), breakdown);
    }
}
