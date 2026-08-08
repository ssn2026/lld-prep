package model;

public class Seat {

    private final String seatId;
    private final char row;
    private final int number;
    private final SeatType seatType;
    private SeatStatus status;

    public Seat(String seatId, char row, int number, SeatType seatType) {
        this.seatId = seatId;
        this.row = row;
        this.number = number;
        this.seatType = seatType;
        this.status = SeatStatus.AVAILABLE;
    }

    public String getSeatId() {
        return seatId;
    }

    public char getRow() {
        return row;
    }

    public int getNumber() {
        return number;
    }

    public SeatType getSeatType() {
        return seatType;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }
}
