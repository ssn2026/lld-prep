package model;

import java.util.List;

public abstract class Vehicle {
    private final String licensePlate;
    private final VehicleType type;

    protected Vehicle(String licensePlate, VehicleType type) {
        this.licensePlate = licensePlate;
        this.type = type;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleType getType() {
        return type;
    }

    /**
     * Spot types this vehicle can legally park in, ordered best-fit first
     * (smallest compatible spot first) so the service can minimize wasted space.
     */
    public abstract List<SpotType> getCompatibleSpotTypes();
}
