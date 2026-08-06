package model;

import java.util.List;

public class Truck extends Vehicle {

    public Truck(String licensePlate) {
        super(licensePlate, VehicleType.TRUCK);
    }

    @Override
    public List<SpotType> getCompatibleSpotTypes() {
        return List.of(SpotType.LARGE);
    }
}
