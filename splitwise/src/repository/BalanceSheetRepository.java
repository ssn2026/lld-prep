package repository;

import java.util.LinkedHashMap;
import java.util.Map;
import model.User;

/**
 * balances[A][B] = amount B owes A (negative means A owes B). adjust()
 * always touches both directions together so the sheet stays symmetric by
 * construction: balances[A][B] == -balances[B][A] holds after every call.
 */
public class BalanceSheetRepository {

    private final Map<String, Map<String, Double>> balances = new LinkedHashMap<>();

    public void adjust(User creditor, User debtor, double amount) {
        if (creditor.equals(debtor) || amount == 0) {
            return;
        }
        bump(creditor.getId(), debtor.getId(), amount);
        bump(debtor.getId(), creditor.getId(), -amount);
    }

    public double getBalance(User a, User b) {
        return balances.getOrDefault(a.getId(), Map.of()).getOrDefault(b.getId(), 0.0);
    }

    public Map<String, Double> getBalancesFor(String userId) {
        return balances.getOrDefault(userId, Map.of());
    }

    private void bump(String id1, String id2, double delta) {
        Map<String, Double> row = balances.computeIfAbsent(id1, k -> new LinkedHashMap<>());
        row.merge(id2, delta, Double::sum);
    }
}
