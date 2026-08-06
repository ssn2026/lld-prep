package model;

import java.util.List;

public class Car extends Vehicle {

    public Car(String licensePlate) {
        super(licensePlate, VehicleType.CAR);
    }

    @Override
    public List<SpotType> getCompatibleSpotTypes() {
        return List.of(SpotType.MEDIUM, SpotType.LARGE);
    }
}
