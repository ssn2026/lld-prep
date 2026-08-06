package strategy;

import model.ParkingSpot;
import model.SpotType;
import repository.SpotAvailabilityIndex;

public class NearestToElevatorStrategy implements SpotAssignmentStrategy {
    @Override
    public ParkingSpot selectSpot(SpotType type, SpotAvailabilityIndex index) {
        return index.peekNearestToElevator(type);
    }
}
