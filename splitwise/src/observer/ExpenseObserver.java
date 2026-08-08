package observer;

import model.Expense;
import model.User;

public interface ExpenseObserver {
    void onExpenseAdded(Expense expense);

    void onSettlement(User payer, User payee, double amount);
}
