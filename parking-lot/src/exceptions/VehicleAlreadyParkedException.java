package exceptions;

public class VehicleAlreadyParkedException extends RuntimeException {
    public VehicleAlreadyParkedException(String message) {
        super(message);
    }
}
