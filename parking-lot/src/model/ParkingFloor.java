package model;

import java.util.ArrayList;
import java.util.List;

public class ParkingFloor {
    private final int floorNumber;
    private final List<ParkingSpot> spots;

    public ParkingFloor(int floorNumber) {
        this.floorNumber = floorNumber;
        this.spots = new ArrayList<>();
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }

    public long countAvailable(SpotType type) {
        return spots.stream()
                .filter(s -> s.getSpotType() == type && !s.isOccupied())
                .count();
    }
}
