package repository;

import exceptions.AccountNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import model.Account;

public class AccountRepository {

    private final Map<String, Account> accountsById = new LinkedHashMap<>();

    public void save(Account account) {
        accountsById.put(account.getAccountId(), account);
    }

    public Account findByAccountId(String accountId) {
        Account account = accountsById.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("No account with id " + accountId);
        }
        return account;
    }
}
