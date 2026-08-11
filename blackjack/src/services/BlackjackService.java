package services;

import exceptions.PlayerNotFoundException;
import exceptions.RoundNotReadyException;
import factory.DeckFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import model.Card;
import model.Deck;
import model.Hand;
import model.HandStatus;
import model.Player;
import strategy.DealerPlayStrategy;
import strategy.StandardDealerStrategy;

/**
 * The single public entry point. Owns the Deck, every Player (including
 * the dealer, itself just a Player), and drives each Hand's own HandState
 * through hit()/stand()/dealer-play.
 */
public class BlackjackService {
    private static final String DEALER_NAME = "Dealer";

    private final DealerPlayStrategy dealerStrategy = new StandardDealerStrategy();
    private Deck deck;
    private Map<String, Player> playersByName;
    private Player dealer;

    public void startRound(List<String> playerNames) {
        deck = DeckFactory.createShuffledDeck();
        playersByName = new LinkedHashMap<>();
        for (String name : playerNames) {
            playersByName.put(name, new Player(name));
        }
        dealer = new Player(DEALER_NAME);

        for (Player player : playersByName.values()) {
            dealInitialHand(player);
        }
        dealInitialHand(dealer);
    }

    private void dealInitialHand(Player player) {
        Hand hand = player.getHand();
        hand.addCard(deck.draw());
        hand.addCard(deck.draw());
        hand.settleInitialState();
    }

    public void hit(String playerName) {
        Hand hand = findPlayer(playerName).getHand();
        hand.getState().requireActive();
        Card card = deck.draw();
        hand.setState(hand.getState().hit(hand, card));
    }

    public void stand(String playerName) {
        Hand hand = findPlayer(playerName).getHand();
        hand.setState(hand.getState().stand(hand));
    }

    public void playDealerTurn() {
        requireAllPlayersDone();
        Hand dealerHand = dealer.getHand();
        while (dealerHand.getState().getStatus() == HandStatus.ACTIVE && dealerStrategy.shouldHit(dealerHand)) {
            Card card = deck.draw();
            dealerHand.setState(dealerHand.getState().hit(dealerHand, card));
        }
        if (dealerHand.getState().getStatus() == HandStatus.ACTIVE) {
            dealerHand.setState(dealerHand.getState().stand(dealerHand));
        }
    }

    public String getHandsSummary() {
        StringBuilder sb = new StringBuilder();
        appendHand(sb, dealer);
        for (Player player : playersByName.values()) {
            appendHand(sb, player);
        }
        return sb.toString().stripTrailing();
    }

    private void appendHand(StringBuilder sb, Player player) {
        Hand hand = player.getHand();
        sb.append(player.getName()).append(": ").append(hand.getCards()).append(" = ")
                .append(hand.getTotal()).append(" (").append(hand.getState().getStatus()).append(")\n");
    }

    public String getRoundResult() {
        requireAllPlayersDone();
        Hand dealerHand = dealer.getHand();
        if (dealerHand.getState().getStatus() == HandStatus.ACTIVE) {
            throw new RoundNotReadyException("Dealer hasn't played yet");
        }
        StringBuilder sb = new StringBuilder();
        sb.append(DEALER_NAME).append(": ").append(dealerHand.getTotal())
                .append(" (").append(dealerHand.getState().getStatus()).append(")\n");
        for (Player player : playersByName.values()) {
            sb.append(player.getName()).append(": ").append(player.getHand().getTotal())
                    .append(" (").append(player.getHand().getState().getStatus()).append(") -> ")
                    .append(outcome(player.getHand(), dealerHand)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String outcome(Hand playerHand, Hand dealerHand) {
        HandStatus playerStatus = playerHand.getState().getStatus();
        HandStatus dealerStatus = dealerHand.getState().getStatus();
        if (playerStatus == HandStatus.BUSTED) {
            return "LOSE";
        }
        if (dealerStatus == HandStatus.BUSTED) {
            return "WIN";
        }
        if (playerStatus == HandStatus.BLACKJACK && dealerStatus == HandStatus.BLACKJACK) {
            return "PUSH";
        }
        if (playerStatus == HandStatus.BLACKJACK) {
            return "WIN";
        }
        if (dealerStatus == HandStatus.BLACKJACK) {
            return "LOSE";
        }
        int playerTotal = playerHand.getTotal();
        int dealerTotal = dealerHand.getTotal();
        if (playerTotal > dealerTotal) {
            return "WIN";
        }
        if (playerTotal < dealerTotal) {
            return "LOSE";
        }
        return "PUSH";
    }

    private void requireAllPlayersDone() {
        for (Player player : playersByName.values()) {
            if (player.getHand().getState().getStatus() == HandStatus.ACTIVE) {
                throw new RoundNotReadyException(player.getName() + " still has to hit or stand");
            }
        }
    }

    private Player findPlayer(String name) {
        Player player = playersByName.get(name);
        if (player == null) {
            throw new PlayerNotFoundException(name);
        }
        return player;
    }
}
