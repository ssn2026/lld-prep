import exceptions.InvalidTicketException;
import exceptions.NoAvailableSpotException;
import exceptions.VehicleAlreadyParkedException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import model.Car;
import model.Motorcycle;
import model.SpotType;
import model.Ticket;
import model.Truck;
import model.Vehicle;
import services.ParkingLotService;
import strategy.AnySpotStrategy;
import strategy.FlatRatePricingStrategy;
import strategy.HourlyPricingStrategy;
import strategy.NearestToElevatorStrategy;
import strategy.NearestToExitStrategy;
import strategy.SpotAssignmentStrategy;

/**
 * Drives ParkingLotService from a plain-text command script so the design can
 * be exercised end-to-end without a UI. Anchors simulated clock time to make
 * duration-based fee calculation deterministic and reproducible.
 */
public class Main {

    private static final LocalDateTime ANCHOR = LocalDateTime.of(2026, 8, 6, 9, 0);

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        ParkingLotService service = ParkingLotService.getInstance();

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String result = execute(service, line);
            log(output, "> " + line);
            log(output, result);
        }

        if (args.length >= 2) {
            Files.writeString(Path.of(args[1]), output.toString());
        }
    }

    private static void log(StringBuilder output, String line) {
        System.out.println(line);
        output.append(line).append(System.lineSeparator());
    }

    private static String execute(ParkingLotService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "FLOOR" -> {
                    service.addFloor(Integer.parseInt(parts[1]));
                    yield "OK floor " + parts[1] + " added";
                }
                case "SPOT" -> {
                    int floorNumber = Integer.parseInt(parts[1]);
                    SpotType type = SpotType.valueOf(parts[2]);
                    int spotId = Integer.parseInt(parts[3]);
                    int distanceFromExit = Integer.parseInt(parts[4]);
                    int distanceFromElevator = Integer.parseInt(parts[5]);
                    service.addSpot(floorNumber, type, spotId, distanceFromExit, distanceFromElevator);
                    yield "OK spot " + type + " #" + spotId + " added to floor " + floorNumber
                            + " (exit=" + distanceFromExit + " elevator=" + distanceFromElevator + ")";
                }
                case "STRATEGY" -> {
                    if (parts[1].equals("FLAT")) {
                        service.setPricingStrategy(new FlatRatePricingStrategy());
                    } else {
                        service.setPricingStrategy(new HourlyPricingStrategy());
                    }
                    yield "OK pricing strategy switched to " + parts[1];
                }
                case "PARK" -> {
                    Vehicle vehicle = createVehicle(parts[1], parts[2]);
                    LocalDateTime entryTime = ANCHOR.plusMinutes(Long.parseLong(parts[3]));
                    String preference = parts.length >= 5 ? parts[4] : "ANY";
                    SpotAssignmentStrategy assignmentStrategy = createAssignmentStrategy(preference);
                    Ticket ticket = service.parkVehicle(vehicle, entryTime, assignmentStrategy);
                    yield "OK parked " + parts[2] + " (" + preference + ") -> ticket " + ticket.getTicketId()
                            + " spot " + ticket.getSpot().getSpotCode();
                }
                case "UNPARK" -> {
                    String plate = parts[1];
                    LocalDateTime exitTime = ANCHOR.plusMinutes(Long.parseLong(parts[2]));
                    double fee = service.unparkVehicle(plate, exitTime);
                    yield "OK unparked " + plate + " -> fee $" + fee;
                }
                case "STATUS" -> "STATUS\n" + service.getStatusReport().stripTrailing();
                default -> "ERROR unknown command: " + command;
            };
        } catch (VehicleAlreadyParkedException | NoAvailableSpotException | InvalidTicketException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static SpotAssignmentStrategy createAssignmentStrategy(String preference) {
        return switch (preference) {
            case "EXIT" -> new NearestToExitStrategy();
            case "ELEVATOR" -> new NearestToElevatorStrategy();
            default -> new AnySpotStrategy();
        };
    }

    private static Vehicle createVehicle(String type, String plate) {
        return switch (type) {
            case "MOTORCYCLE" -> new Motorcycle(plate);
            case "CAR" -> new Car(plate);
            case "TRUCK" -> new Truck(plate);
            default -> throw new IllegalArgumentException("Unknown vehicle type: " + type);
        };
    }
}
