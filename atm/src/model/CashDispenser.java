package model;

import chain.DenominationHandler;
import exceptions.InsufficientCashException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Owns the ATM's physical cash inventory. Each dispense() call rebuilds the
 * Chain of Responsibility fresh (largest denomination first, via the
 * reverse-order TreeMap) from a read-only snapshot, plans the full
 * breakdown, and only debits notesByDenomination once the whole amount is
 * confirmed coverable end to end -- so a failed dispense (exact change
 * unavailable) never leaves the inventory partially consumed.
 */
public class CashDispenser {

    private final Map<Integer, Integer> notesByDenomination = new TreeMap<>(Comparator.reverseOrder());

    public void loadCash(int denomination, int count) {
        notesByDenomination.merge(denomination, count, Integer::sum);
    }

    public Map<Integer, Integer> dispense(int amount) {
        DenominationHandler chainHead = buildChain();
        Map<Integer, Integer> breakdown = new LinkedHashMap<>();
        chainHead.plan(amount, breakdown);
        breakdown.forEach((denomination, count) -> notesByDenomination.merge(denomination, -count, Integer::sum));
        return breakdown;
    }

    private DenominationHandler buildChain() {
        DenominationHandler head = null;
        DenominationHandler previous = null;
        for (Map.Entry<Integer, Integer> entry : notesByDenomination.entrySet()) {
            DenominationHandler handler = new DenominationHandler(entry.getKey(), entry.getValue());
            if (head == null) {
                head = handler;
            } else {
                previous.linkWith(handler);
            }
            previous = handler;
        }
        if (head == null) {
            throw new InsufficientCashException("ATM has no cash loaded");
        }
        return head;
    }
}
