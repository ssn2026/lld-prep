package observer;

import model.Booking;

public interface BookingObserver {
    void onBookingConfirmed(Booking booking);

    void onBookingCancelled(Booking booking);
}
