package factory;

import model.LargeSpot;
import model.MediumSpot;
import model.ParkingSpot;
import model.SmallSpot;
import model.SpotType;

public class ParkingSpotFactory {

    private ParkingSpotFactory() {
    }

    public static ParkingSpot createSpot(SpotType type, int id, int floorNumber,
            int distanceFromExit, int distanceFromElevator) {
        return switch (type) {
            case SMALL -> new SmallSpot(id, floorNumber, distanceFromExit, distanceFromElevator);
            case MEDIUM -> new MediumSpot(id, floorNumber, distanceFromExit, distanceFromElevator);
            case LARGE -> new LargeSpot(id, floorNumber, distanceFromExit, distanceFromElevator);
        };
    }
}
