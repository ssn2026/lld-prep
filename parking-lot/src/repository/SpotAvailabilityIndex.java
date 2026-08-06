package repository;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.PriorityQueue;
import model.ParkingSpot;
import model.SpotType;

/**
 * In-memory index of free spots, kept as two min-heaps per SpotType so the
 * nearest-to-exit / nearest-to-elevator spot can be found in O(log n)
 * instead of scanning every spot on every floor.
 *
 * Invariant: for a given SpotType, a spot sits in the exit heap if and only
 * if it also sits in the elevator heap, if and only if the spot is free.
 * register()/markAvailable() add to both heaps together; markUnavailable()
 * removes from both together — so neither heap ever holds a stale
 * (occupied) or duplicate entry.
 */
public class SpotAvailabilityIndex {

    private final Map<SpotType, PriorityQueue<ParkingSpot>> byExitDistance = new EnumMap<>(SpotType.class);
    private final Map<SpotType, PriorityQueue<ParkingSpot>> byElevatorDistance = new EnumMap<>(SpotType.class);

    public SpotAvailabilityIndex() {
        for (SpotType type : SpotType.values()) {
            byExitDistance.put(type, new PriorityQueue<>(Comparator.comparingInt(ParkingSpot::getDistanceFromExit)));
            byElevatorDistance.put(type, new PriorityQueue<>(Comparator.comparingInt(ParkingSpot::getDistanceFromElevator)));
        }
    }

    /** Called once when a spot is first created; a new spot starts free. */
    public void register(ParkingSpot spot) {
        byExitDistance.get(spot.getSpotType()).add(spot);
        byElevatorDistance.get(spot.getSpotType()).add(spot);
    }

    public ParkingSpot peekNearestToExit(SpotType type) {
        return byExitDistance.get(type).peek();
    }

    public ParkingSpot peekNearestToElevator(SpotType type) {
        return byElevatorDistance.get(type).peek();
    }

    /** Removes a spot from both heaps once it has been assigned to a vehicle. */
    public void markUnavailable(ParkingSpot spot) {
        byExitDistance.get(spot.getSpotType()).remove(spot);
        byElevatorDistance.get(spot.getSpotType()).remove(spot);
    }

    /** Returns a released spot to both heaps so it becomes selectable again. */
    public void markAvailable(ParkingSpot spot) {
        byExitDistance.get(spot.getSpotType()).add(spot);
        byElevatorDistance.get(spot.getSpotType()).add(spot);
    }
}
