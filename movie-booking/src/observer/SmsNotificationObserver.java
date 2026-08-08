package observer;

import model.Booking;

public class SmsNotificationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(Booking booking) {
        System.out.println("SMS to " + booking.getUser().getName() + ": your seats for "
                + booking.getShow().getMovie().getTitle() + " are booked. Booking "
                + booking.getBookingId() + ".");
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        System.out.println("SMS to " + booking.getUser().getName() + ": booking "
                + booking.getBookingId() + " has been cancelled.");
    }
}
