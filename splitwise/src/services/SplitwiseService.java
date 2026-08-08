package services;

import factory.SplitStrategyFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Expense;
import model.Split;
import model.SplitType;
import model.User;
import observer.ExpenseObserver;
import repository.BalanceSheetRepository;
import repository.UserRepository;
import strategy.SplitStrategy;

/**
 * Single public entry point for Splitwise. All caller-facing operations
 * (adding users/expenses, querying balances, settling up) go through this
 * class; model/strategy/factory/observer/repository classes are internal
 * collaborators.
 */
public class SplitwiseService {

    private static final double EPSILON = 0.01;

    private final UserRepository userRepository = new UserRepository();
    private final BalanceSheetRepository balanceSheet = new BalanceSheetRepository();
    private final List<ExpenseObserver> observers = new ArrayList<>();

    public User addUser(String name, String email) {
        return userRepository.save(new User(name, email));
    }

    public void addObserver(ExpenseObserver observer) {
        observers.add(observer);
    }

    public Expense addExpense(String description, double amount, String paidByUserId,
            SplitType splitType, List<String> participantUserIds, Map<String, Double> shareInputs) {
        User paidBy = userRepository.findById(paidByUserId);
        List<User> participants = new ArrayList<>();
        for (String id : participantUserIds) {
            participants.add(userRepository.findById(id));
        }

        SplitStrategy strategy = SplitStrategyFactory.getStrategy(splitType);
        List<Split> splits = strategy.computeSplits(amount, participants, shareInputs);

        Expense expense = new Expense(description, amount, paidBy, splitType, splits);
        for (Split split : splits) {
            balanceSheet.adjust(paidBy, split.getUser(), split.getAmount());
        }

        for (ExpenseObserver observer : observers) {
            observer.onExpenseAdded(expense);
        }
        return expense;
    }

    public void settleUp(String payerUserId, String payeeUserId, double amount) {
        User payer = userRepository.findById(payerUserId);
        User payee = userRepository.findById(payeeUserId);
        balanceSheet.adjust(payee, payer, -amount);

        for (ExpenseObserver observer : observers) {
            observer.onSettlement(payer, payee, amount);
        }
    }

    public String showBalance(String userId1, String userId2) {
        User u1 = userRepository.findById(userId1);
        User u2 = userRepository.findById(userId2);
        return formatBalance(u1, u2, balanceSheet.getBalance(u1, u2));
    }

    public String showBalancesFor(String userId) {
        User user = userRepository.findById(userId);
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (Map.Entry<String, Double> entry : balanceSheet.getBalancesFor(userId).entrySet()) {
            if (Math.abs(entry.getValue()) < EPSILON) {
                continue;
            }
            User other = userRepository.findById(entry.getKey());
            sb.append(formatBalance(user, other, entry.getValue())).append('\n');
            any = true;
        }
        if (!any) {
            sb.append(user.getName()).append(" is all settled up");
        }
        return sb.toString().stripTrailing();
    }

    public String showAllBalances() {
        List<User> users = new ArrayList<>(userRepository.findAll());
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (int i = 0; i < users.size(); i++) {
            for (int j = i + 1; j < users.size(); j++) {
                User a = users.get(i);
                User b = users.get(j);
                double balance = balanceSheet.getBalance(a, b);
                if (Math.abs(balance) < EPSILON) {
                    continue;
                }
                sb.append(formatBalance(a, b, balance)).append('\n');
                any = true;
            }
        }
        if (!any) {
            sb.append("Everyone is settled up");
        }
        return sb.toString().stripTrailing();
    }

    private String formatBalance(User a, User b, double amountBOwesA) {
        if (amountBOwesA > 0) {
            return String.format("%s owes %s $%.2f", b.getName(), a.getName(), amountBOwesA);
        }
        if (amountBOwesA < 0) {
            return String.format("%s owes %s $%.2f", a.getName(), b.getName(), -amountBOwesA);
        }
        return String.format("%s and %s are settled up", a.getName(), b.getName());
    }
}
