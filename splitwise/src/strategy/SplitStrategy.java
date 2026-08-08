package strategy;

import java.util.List;
import java.util.Map;
import model.Split;
import model.User;

public interface SplitStrategy {
    /**
     * shareInputs is keyed by User.id; EQUAL ignores it, EXACT expects an
     * absolute amount per participant, PERCENT expects a percentage per
     * participant.
     */
    List<Split> computeSplits(double totalAmount, List<User> participants, Map<String, Double> shareInputs);
}
