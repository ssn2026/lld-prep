package model;

public class SmallSpot extends ParkingSpot {

    public SmallSpot(int id, int floorNumber, int distanceFromExit, int distanceFromElevator) {
        super(id, floorNumber, distanceFromExit, distanceFromElevator);
    }

    @Override
    public SpotType getSpotType() {
        return SpotType.SMALL;
    }
}
