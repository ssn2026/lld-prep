package strategy;

import java.util.Map;
import model.Ticket;
import model.VehicleType;

public class FlatRatePricingStrategy implements PricingStrategy {

    private final Map<VehicleType, Double> flatRate = Map.of(
            VehicleType.MOTORCYCLE, 30.0,
            VehicleType.CAR, 50.0,
            VehicleType.TRUCK, 100.0
    );

    @Override
    public double calculateFee(Ticket ticket) {
        return flatRate.get(ticket.getVehicle().getType());
    }
}
