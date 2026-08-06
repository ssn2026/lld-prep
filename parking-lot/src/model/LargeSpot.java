package model;

public class LargeSpot extends ParkingSpot {

    public LargeSpot(int id, int floorNumber, int distanceFromExit, int distanceFromElevator) {
        super(id, floorNumber, distanceFromExit, distanceFromElevator);
    }

    @Override
    public SpotType getSpotType() {
        return SpotType.LARGE;
    }
}
