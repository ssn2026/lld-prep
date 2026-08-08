package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class Screen {

    private final String screenId;
    private final String name;
    private final Map<String, Seat> seatsById = new LinkedHashMap<>();

    public Screen(String screenId, String name, int rows, int seatsPerRow) {
        this.screenId = screenId;
        this.name = name;
        generateSeats(rows, seatsPerRow);
    }

    private void generateSeats(int rows, int seatsPerRow) {
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            char rowLabel = (char) ('A' + rowIndex);
            SeatType seatType = seatTypeForRow(rowIndex, rows);
            for (int number = 1; number <= seatsPerRow; number++) {
                String seatId = rowLabel + String.valueOf(number);
                seatsById.put(seatId, new Seat(seatId, rowLabel, number, seatType));
            }
        }
    }

    /**
     * Front rows (closest to screen) are REGULAR, middle rows PREMIUM, back
     * rows RECLINER - a common real-world layout, applied proportionally so
     * it scales with any row count.
     */
    private SeatType seatTypeForRow(int rowIndex, int totalRows) {
        if (rowIndex < totalRows * 0.4) {
            return SeatType.REGULAR;
        } else if (rowIndex < totalRows * 0.7) {
            return SeatType.PREMIUM;
        }
        return SeatType.RECLINER;
    }

    public String getScreenId() {
        return screenId;
    }

    public String getName() {
        return name;
    }

    public Seat getSeat(String seatId) {
        return seatsById.get(seatId);
    }

    public Map<String, Seat> getSeatsById() {
        return seatsById;
    }

    public java.util.List<Seat> getAllSeats() {
        return new ArrayList<>(seatsById.values());
    }
}
