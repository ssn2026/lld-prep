package strategy;

import model.Ticket;

public interface PricingStrategy {
    double calculateFee(Ticket ticket);
}
