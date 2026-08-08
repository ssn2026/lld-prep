package observer;

import model.Booking;

public class EmailNotificationObserver implements BookingObserver {

    @Override
    public void onBookingConfirmed(Booking booking) {
        System.out.println("EMAIL to " + booking.getUser().getEmail() + ": booking "
                + booking.getBookingId() + " confirmed for " + booking.getShow().getMovie().getTitle()
                + " ($" + booking.getTotalAmount() + ")");
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        System.out.println("EMAIL to " + booking.getUser().getEmail() + ": booking "
                + booking.getBookingId() + " cancelled, refund $" + booking.getTotalAmount());
    }
}
