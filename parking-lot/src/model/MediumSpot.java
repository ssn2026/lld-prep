package model;

public class MediumSpot extends ParkingSpot {

    public MediumSpot(int id, int floorNumber, int distanceFromExit, int distanceFromElevator) {
        super(id, floorNumber, distanceFromExit, distanceFromElevator);
    }

    @Override
    public SpotType getSpotType() {
        return SpotType.MEDIUM;
    }
}
