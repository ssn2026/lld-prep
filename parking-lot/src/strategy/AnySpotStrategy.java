package strategy;

import model.ParkingSpot;
import model.SpotType;
import repository.SpotAvailabilityIndex;

/**
 * Default strategy for a vehicle with no proximity preference. Reuses the
 * exit-distance heap purely as a convenient free-spot pool — the ordering
 * itself carries no meaning here, unlike NearestToExitStrategy.
 */
public class AnySpotStrategy implements SpotAssignmentStrategy {
    @Override
    public ParkingSpot selectSpot(SpotType type, SpotAvailabilityIndex index) {
        return index.peekNearestToExit(type);
    }
}
