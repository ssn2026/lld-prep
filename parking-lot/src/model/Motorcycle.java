package model;

import java.util.List;

public class Motorcycle extends Vehicle {

    public Motorcycle(String licensePlate) {
        super(licensePlate, VehicleType.MOTORCYCLE);
    }

    @Override
    public List<SpotType> getCompatibleSpotTypes() {
        return List.of(SpotType.SMALL, SpotType.MEDIUM, SpotType.LARGE);
    }
}
