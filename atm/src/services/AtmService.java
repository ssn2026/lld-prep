package services;

import command.DepositCommand;
import command.TransactionCommand;
import command.WithdrawCommand;
import exceptions.InvalidPinException;
import java.util.List;
import model.Account;
import model.AtmStatus;
import model.CashDispenser;
import model.TransactionReceipt;
import repository.AccountRepository;
import state.AtmState;
import state.IdleState;

/**
 * Single public entry point for the ATM. All caller-facing operations
 * (account setup, cash loading, card session, transactions) go through this
 * class; model/state/chain/command/repository classes are internal
 * collaborators.
 */
public class AtmService {

    private static final int MAX_PIN_ATTEMPTS = 3;

    private final AccountRepository accountRepository = new AccountRepository();
    private final CashDispenser cashDispenser = new CashDispenser();
    private AtmState currentState = IdleState.INSTANCE;
    private String currentAccountNumber;
    private int pinAttempts;

    public void registerAccount(String accountNumber, String pin, int initialBalance) {
        accountRepository.save(new Account(accountNumber, pin, initialBalance));
    }

    public void loadCash(int denomination, int count) {
        cashDispenser.loadCash(denomination, count);
    }

    public void insertCard(String accountNumber) {
        currentState.requireIdle();
        accountRepository.findByAccountNumber(accountNumber);
        currentState = currentState.insertCard();
        currentAccountNumber = accountNumber;
        pinAttempts = 0;
    }

    public void enterPin(String pin) {
        currentState.requireCardInserted();
        Account account = accountRepository.findByAccountNumber(currentAccountNumber);
        if (account.isPinCorrect(pin)) {
            currentState = currentState.authenticate();
            pinAttempts = 0;
            return;
        }
        pinAttempts++;
        if (pinAttempts >= MAX_PIN_ATTEMPTS) {
            currentState = currentState.retainCard();
            throw new InvalidPinException("Incorrect PIN entered " + MAX_PIN_ATTEMPTS + " times; card retained");
        }
        throw new InvalidPinException("Incorrect PIN (attempt " + pinAttempts + "/" + MAX_PIN_ATTEMPTS + ")");
    }

    public int checkBalance() {
        currentState.requireAuthenticated();
        return currentAccount().getBalance();
    }

    public TransactionReceipt withdraw(int amount) {
        currentState.requireAuthenticated();
        return recordAndReturn(new WithdrawCommand(currentAccount(), amount, cashDispenser));
    }

    public TransactionReceipt deposit(int amount) {
        currentState.requireAuthenticated();
        return recordAndReturn(new DepositCommand(currentAccount(), amount));
    }

    public List<TransactionReceipt> getMiniStatement() {
        currentState.requireAuthenticated();
        return currentAccount().getTransactionHistory();
    }

    public void ejectCard() {
        currentState = currentState.ejectCard();
        currentAccountNumber = null;
        pinAttempts = 0;
    }

    /** Administrative override (e.g. a technician clearing a retained card) -- not a state transition. */
    public void resetMachine() {
        currentState = IdleState.INSTANCE;
        currentAccountNumber = null;
        pinAttempts = 0;
    }

    public AtmStatus getStatus() {
        return currentState.getStatus();
    }

    private TransactionReceipt recordAndReturn(TransactionCommand command) {
        TransactionReceipt receipt = command.execute();
        currentAccount().addTransaction(receipt);
        return receipt;
    }

    private Account currentAccount() {
        return accountRepository.findByAccountNumber(currentAccountNumber);
    }
}
