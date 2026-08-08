package repository;

import java.util.LinkedHashMap;
import java.util.Map;
import model.Booking;

public class BookingRepository {

    private final Map<String, Booking> bookingsById = new LinkedHashMap<>();

    public void save(Booking booking) {
        bookingsById.put(booking.getBookingId(), booking);
    }

    public Booking findById(String bookingId) {
        return bookingsById.get(bookingId);
    }
}
