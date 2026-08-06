package strategy;

import model.ParkingSpot;
import model.SpotType;
import repository.SpotAvailabilityIndex;

public class NearestToExitStrategy implements SpotAssignmentStrategy {
    @Override
    public ParkingSpot selectSpot(SpotType type, SpotAvailabilityIndex index) {
        return index.peekNearestToExit(type);
    }
}
