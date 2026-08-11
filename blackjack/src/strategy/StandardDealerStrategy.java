package strategy;

import model.Hand;

/** The house rule this table plays: hit on anything below 17, stand at 17+. */
public class StandardDealerStrategy implements DealerPlayStrategy {
    @Override
    public boolean shouldHit(Hand dealerHand) {
        return dealerHand.getTotal() < 17;
    }
}
