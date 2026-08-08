package observer;

import java.util.function.Consumer;
import model.Expense;
import model.Split;
import model.User;

/**
 * Writes notifications through a caller-supplied sink instead of printing
 * directly, so Main can fold them into the same transcript it logs command
 * results to rather than having two independent output streams.
 */
public class ConsoleNotifier implements ExpenseObserver {

    private final Consumer<String> sink;

    public ConsoleNotifier(Consumer<String> sink) {
        this.sink = sink;
    }

    @Override
    public void onExpenseAdded(Expense expense) {
        sink.accept(String.format("  [NOTIFY] '%s' ($%.2f) added by %s",
                expense.getDescription(), expense.getAmount(), expense.getPaidBy().getName()));
        for (Split split : expense.getSplits()) {
            if (split.getUser().equals(expense.getPaidBy())) {
                continue;
            }
            sink.accept(String.format("  [NOTIFY] %s owes %s $%.2f",
                    split.getUser().getName(), expense.getPaidBy().getName(), split.getAmount()));
        }
    }

    @Override
    public void onSettlement(User payer, User payee, double amount) {
        sink.accept(String.format("  [NOTIFY] %s paid %s $%.2f", payer.getName(), payee.getName(), amount));
    }
}
