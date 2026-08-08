package strategy;

import exceptions.InvalidSplitException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Split;
import model.User;

public class PercentSplitStrategy implements SplitStrategy {

    private static final double EPSILON = 0.01;

    @Override
    public List<Split> computeSplits(double totalAmount, List<User> participants, Map<String, Double> shareInputs) {
        double percentSum = 0;
        for (User user : participants) {
            Double pct = shareInputs.get(user.getId());
            if (pct == null) {
                throw new InvalidSplitException("Missing percentage for user " + user.getName());
            }
            percentSum += pct;
        }
        if (Math.abs(percentSum - 100.0) > EPSILON) {
            throw new InvalidSplitException("Percentages sum to " + percentSum + " but must sum to 100");
        }

        List<Split> splits = new ArrayList<>();
        double runningTotal = 0;
        for (int i = 0; i < participants.size(); i++) {
            User user = participants.get(i);
            // last participant absorbs the rounding remainder so shares always sum to totalAmount exactly
            double share = (i == participants.size() - 1)
                    ? round2(totalAmount - runningTotal)
                    : round2(totalAmount * shareInputs.get(user.getId()) / 100.0);
            splits.add(new Split(user, share));
            runningTotal += share;
        }
        return splits;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
