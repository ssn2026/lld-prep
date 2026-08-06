package services;

import exceptions.InvalidTicketException;
import exceptions.NoAvailableSpotException;
import exceptions.VehicleAlreadyParkedException;
import factory.ParkingSpotFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import model.ParkingFloor;
import model.ParkingSpot;
import model.SpotType;
import model.Ticket;
import model.TicketStatus;
import model.Vehicle;
import repository.SpotAvailabilityIndex;
import strategy.AnySpotStrategy;
import strategy.HourlyPricingStrategy;
import strategy.PricingStrategy;
import strategy.SpotAssignmentStrategy;

/**
 * Single public entry point for the parking lot. All caller-facing operations
 * (parking, unparking, configuring pricing, checking availability) go through
 * this class; model/strategy/factory classes are internal collaborators.
 */
public class ParkingLotService {

    private static ParkingLotService instance;

    private final List<ParkingFloor> floors = new ArrayList<>();
    private final Map<String, Ticket> activeTicketsByPlate = new HashMap<>();
    private final AtomicInteger ticketSequence = new AtomicInteger(1);
    private final SpotAvailabilityIndex availabilityIndex = new SpotAvailabilityIndex();
    private PricingStrategy pricingStrategy = new HourlyPricingStrategy();
    private static final SpotAssignmentStrategy DEFAULT_ASSIGNMENT_STRATEGY = new AnySpotStrategy();

    private ParkingLotService() {
    }

    public static synchronized ParkingLotService getInstance() {
        if (instance == null) {
            instance = new ParkingLotService();
        }
        return instance;
    }

    public void addFloor(int floorNumber) {
        floors.add(new ParkingFloor(floorNumber));
    }

    public void addSpot(int floorNumber, SpotType type, int spotId,
            int distanceFromExit, int distanceFromElevator) {
        ParkingFloor floor = findFloor(floorNumber);
        ParkingSpot spot = ParkingSpotFactory.createSpot(type, spotId, floorNumber,
                distanceFromExit, distanceFromElevator);
        floor.addSpot(spot);
        availabilityIndex.register(spot);
    }

    public void setPricingStrategy(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public Ticket parkVehicle(Vehicle vehicle) {
        return parkVehicle(vehicle, LocalDateTime.now(), DEFAULT_ASSIGNMENT_STRATEGY);
    }

    public Ticket parkVehicle(Vehicle vehicle, SpotAssignmentStrategy assignmentStrategy) {
        return parkVehicle(vehicle, LocalDateTime.now(), assignmentStrategy);
    }

    /**
     * Overload accepting an explicit entry time so callers (and tests) can
     * simulate elapsed parking duration deterministically.
     */
    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime) {
        return parkVehicle(vehicle, entryTime, DEFAULT_ASSIGNMENT_STRATEGY);
    }

    /**
     * Full form: caller picks the spot-selection algorithm per request (e.g. a
     * driver asking for "nearest to elevator"), same way a payment method is
     * chosen per checkout rather than fixed lot-wide.
     */
    public Ticket parkVehicle(Vehicle vehicle, LocalDateTime entryTime, SpotAssignmentStrategy assignmentStrategy) {
        if (activeTicketsByPlate.containsKey(vehicle.getLicensePlate())) {
            throw new VehicleAlreadyParkedException(
                    "Vehicle " + vehicle.getLicensePlate() + " already has an active ticket");
        }

        ParkingSpot spot = findAvailableSpot(vehicle, assignmentStrategy);
        availabilityIndex.markUnavailable(spot);
        spot.assignVehicle(vehicle);

        String ticketId = "T" + ticketSequence.getAndIncrement();
        Ticket ticket = new Ticket(ticketId, vehicle, spot, entryTime);
        activeTicketsByPlate.put(vehicle.getLicensePlate(), ticket);
        return ticket;
    }

    public double unparkVehicle(String licensePlate) {
        return unparkVehicle(licensePlate, LocalDateTime.now());
    }

    public double unparkVehicle(String licensePlate, LocalDateTime exitTime) {
        Ticket ticket = activeTicketsByPlate.get(licensePlate);
        if (ticket == null || ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new InvalidTicketException("No active ticket found for vehicle " + licensePlate);
        }

        ticket.setExitTime(exitTime);
        double fee = pricingStrategy.calculateFee(ticket);
        ticket.setFee(fee);
        ticket.setStatus(TicketStatus.PAID);

        ticket.getSpot().release();
        availabilityIndex.markAvailable(ticket.getSpot());
        activeTicketsByPlate.remove(licensePlate);
        return fee;
    }

    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        for (ParkingFloor floor : floors) {
            sb.append("Floor ").append(floor.getFloorNumber()).append(": ");
            for (SpotType type : SpotType.values()) {
                long available = floor.countAvailable(type);
                long total = floor.getSpots().stream().filter(s -> s.getSpotType() == type).count();
                sb.append(type).append('=').append(available).append('/').append(total).append(' ');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private ParkingFloor findFloor(int floorNumber) {
        return floors.stream()
                .filter(f -> f.getFloorNumber() == floorNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No such floor: " + floorNumber));
    }

    private ParkingSpot findAvailableSpot(Vehicle vehicle, SpotAssignmentStrategy assignmentStrategy) {
        for (SpotType compatibleType : vehicle.getCompatibleSpotTypes()) {
            ParkingSpot spot = assignmentStrategy.selectSpot(compatibleType, availabilityIndex);
            if (spot != null) {
                return spot;
            }
        }
        throw new NoAvailableSpotException(
                "No available spot for vehicle type " + vehicle.getType());
    }
}
