package strategy;

import exceptions.InvalidSplitException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Split;
import model.User;

public class ExactSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    @Override
    public List<Split> computeSplits(double totalAmount, List<User> participants, Map<String, Double> shareInputs) {
        List<Split> splits = new ArrayList<>();
        double sum = 0;
        for (User user : participants) {
            Double share = shareInputs.get(user.getId());
            if (share == null) {
                throw new InvalidSplitException("Missing exact share for user " + user.getName());
            }
            splits.add(new Split(user, share));
            sum += share;
        }
        if (Math.abs(sum - totalAmount) > EPSILON) {
            throw new InvalidSplitException(
                    "Exact splits sum to " + sum + " but expense amount is " + totalAmount);
        }
        return splits;
    }
}
