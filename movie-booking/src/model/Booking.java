package model;

import java.time.LocalDateTime;
import java.util.List;

public class Booking {

    private final String bookingId;
    private final Show show;
    private final User user;
    private final List<Seat> seats;
    private final double totalAmount;
    private final LocalDateTime bookingTime;
    private BookingStatus status;

    public Booking(String bookingId, Show show, User user, List<Seat> seats,
            double totalAmount, LocalDateTime bookingTime) {
        this.bookingId = bookingId;
        this.show = show;
        this.user = user;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.bookingTime = bookingTime;
        this.status = BookingStatus.CONFIRMED;
    }

    public String getBookingId() {
        return bookingId;
    }

    public Show getShow() {
        return show;
    }

    public User getUser() {
        return user;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
