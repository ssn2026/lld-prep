package command;

import exceptions.InvalidAmountException;
import model.Account;
import model.TransactionReceipt;
import model.TransactionType;

public class DepositCommand implements TransactionCommand {

    private final Account account;
    private final int amount;

    public DepositCommand(Account account, int amount) {
        this.account = account;
        this.amount = amount;
    }

    @Override
    public TransactionReceipt execute() {
        if (amount <= 0) {
            throw new InvalidAmountException("Deposit amount must be positive");
        }
        account.credit(amount);
        return new TransactionReceipt(TransactionType.DEPOSIT, amount, account.getBalance(), null);
    }
}
