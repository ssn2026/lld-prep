package repository;

import exceptions.AccountNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Account;

public class AccountRepository {

    private final Map<String, Account> accountsByNumber = new LinkedHashMap<>();

    public void save(Account account) {
        accountsByNumber.put(account.getAccountNumber(), account);
    }

    public Account findByAccountNumber(String accountNumber) {
        Account account = accountsByNumber.get(accountNumber);
        if (account == null) {
            throw new AccountNotFoundException("No account with number " + accountNumber);
        }
        return account;
    }
}
