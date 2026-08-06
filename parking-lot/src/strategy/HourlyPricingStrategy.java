package strategy;

import java.time.Duration;
import java.util.Map;
import model.Ticket;
import model.VehicleType;

public class HourlyPricingStrategy implements PricingStrategy {

    private final Map<VehicleType, Double> hourlyRate = Map.of(
            VehicleType.MOTORCYCLE, 10.0,
            VehicleType.CAR, 20.0,
            VehicleType.TRUCK, 50.0
    );

    @Override
    public double calculateFee(Ticket ticket) {
        Duration parked = Duration.between(ticket.getEntryTime(), ticket.getExitTime());
        long minutes = Math.max(parked.toMinutes(), 0);
        long billedHours = Math.max(1, (long) Math.ceil(minutes / 60.0));
        double rate = hourlyRate.get(ticket.getVehicle().getType());
        return billedHours * rate;
    }
}
