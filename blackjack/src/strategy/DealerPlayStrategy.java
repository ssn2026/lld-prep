package strategy;

import model.Hand;

public interface DealerPlayStrategy {
    boolean shouldHit(Hand dealerHand);
}
