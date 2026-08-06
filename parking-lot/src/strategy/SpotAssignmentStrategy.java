package strategy;

import model.ParkingSpot;
import model.SpotType;
import repository.SpotAvailabilityIndex;

public interface SpotAssignmentStrategy {
    /** Returns the preferred free spot of the given type, or null if none is free. */
    ParkingSpot selectSpot(SpotType type, SpotAvailabilityIndex index);
}
