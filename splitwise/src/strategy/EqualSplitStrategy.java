package strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import model.Split;
import model.User;

public class EqualSplitStrategy implements SplitStrategy {

    @Override
    public List<Split> computeSplits(double totalAmount, List<User> participants, Map<String, Double> shareInputs) {
        int n = participants.size();
        double baseShare = round2(totalAmount / n);

        List<Split> splits = new ArrayList<>();
        double runningTotal = 0;
        for (int i = 0; i < n; i++) {
            // last participant absorbs the rounding remainder so shares always sum to totalAmount exactly
            double share = (i == n - 1) ? round2(totalAmount - runningTotal) : baseShare;
            splits.add(new Split(participants.get(i), share));
            runningTotal += share;
        }
        return splits;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
