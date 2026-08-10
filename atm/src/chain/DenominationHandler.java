package chain;

import exceptions.InsufficientCashException;
import java.util.Map;

/**
 * Chain of Responsibility: each handler owns exactly one denomination and,
 * given the amount still owed, greedily contributes as many of its own
 * notes as it can before handing the remainder to the next (smaller)
 * denomination. CashDispenser rebuilds this chain fresh from a read-only
 * snapshot before every dispense, so plan() never mutates shared state --
 * only CashDispenser commits the result, and only after the full amount is
 * confirmed coverable end to end (see CashDispenser.dispense()).
 */
public class DenominationHandler {

    private final int denomination;
    private final int availableNotes;
    private DenominationHandler next;

    public DenominationHandler(int denomination, int availableNotes) {
        this.denomination = denomination;
        this.availableNotes = availableNotes;
    }

    public DenominationHandler linkWith(DenominationHandler next) {
        this.next = next;
        return next;
    }

    public void plan(int amountRemaining, Map<Integer, Integer> breakdown) {
        int notesUsable = Math.min(amountRemaining / denomination, availableNotes);
        int coveredByThis = notesUsable * denomination;
        if (notesUsable > 0) {
            breakdown.put(denomination, notesUsable);
        }
        int remaining = amountRemaining - coveredByThis;
        if (remaining == 0) {
            return;
        }
        if (next == null) {
            throw new InsufficientCashException(
                    "Cannot dispense exact amount; no smaller denomination available for the remaining " + remaining);
        }
        next.plan(remaining, breakdown);
    }
}
