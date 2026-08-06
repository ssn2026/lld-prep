package model;

public abstract class ParkingSpot {
    private final int id;
    private final int floorNumber;
    private final int distanceFromExit;
    private final int distanceFromElevator;
    private boolean occupied;
    private Vehicle parkedVehicle;

    protected ParkingSpot(int id, int floorNumber, int distanceFromExit, int distanceFromElevator) {
        this.id = id;
        this.floorNumber = floorNumber;
        this.distanceFromExit = distanceFromExit;
        this.distanceFromElevator = distanceFromElevator;
        this.occupied = false;
    }

    public int getDistanceFromExit() {
        return distanceFromExit;
    }

    public int getDistanceFromElevator() {
        return distanceFromElevator;
    }

    public abstract SpotType getSpotType();

    public int getId() {
        return id;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public Vehicle getParkedVehicle() {
        return parkedVehicle;
    }

    public void assignVehicle(Vehicle vehicle) {
        this.parkedVehicle = vehicle;
        this.occupied = true;
    }

    public void release() {
        this.parkedVehicle = null;
        this.occupied = false;
    }

    public String getSpotCode() {
        return "F" + floorNumber + "-" + getSpotType().name().charAt(0) + id;
    }
}
