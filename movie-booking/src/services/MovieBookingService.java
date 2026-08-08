package services;

import exceptions.BookingNotFoundException;
import exceptions.SeatNotAvailableException;
import exceptions.ShowNotFoundException;
import factory.SeatPricingStrategyFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import model.Booking;
import model.BookingStatus;
import model.Movie;
import model.Screen;
import model.Seat;
import model.SeatStatus;
import model.Show;
import model.Theater;
import model.User;
import observer.BookingObserver;
import repository.BookingRepository;
import repository.ShowRepository;
import strategy.SeatPricingStrategy;

/**
 * Single public entry point for the movie booking system. All caller-facing
 * operations (setup, search, booking, cancellation) go through this class;
 * model/strategy/factory/observer classes are internal collaborators.
 */
public class MovieBookingService {

    private final Map<String, Theater> theatersById = new LinkedHashMap<>();
    private final Map<String, Movie> moviesById = new LinkedHashMap<>();
    private final Map<String, User> usersById = new LinkedHashMap<>();
    private final ShowRepository showRepository = new ShowRepository();
    private final BookingRepository bookingRepository = new BookingRepository();
    private final List<BookingObserver> observers = new ArrayList<>();
    private final AtomicInteger bookingSequence = new AtomicInteger(1);

    public void registerObserver(BookingObserver observer) {
        observers.add(observer);
    }

    public Theater addTheater(String theaterId, String name, String city) {
        Theater theater = new Theater(theaterId, name, city);
        theatersById.put(theaterId, theater);
        return theater;
    }

    public Movie addMovie(String movieId, String title, int durationMinutes) {
        Movie movie = new Movie(movieId, title, durationMinutes);
        moviesById.put(movieId, movie);
        return movie;
    }

    public Show addShow(String showId, String movieId, String theaterId, String screenId,
            String screenName, int rows, int seatsPerRow, LocalDateTime startTime, double baseSeatPrice) {
        Movie movie = findMovie(movieId);
        Theater theater = findTheater(theaterId);
        Screen screen = new Screen(screenId, screenName, rows, seatsPerRow);
        Show show = new Show(showId, movie, theater, screen, startTime, baseSeatPrice);
        showRepository.save(show);
        return show;
    }

    public List<Show> searchShows(String movieTitle, String city) {
        return showRepository.findByMovieTitleAndCity(movieTitle, city);
    }

    public List<Seat> getAvailableSeats(String showId) {
        Show show = findShow(showId);
        List<Seat> available = new ArrayList<>();
        for (Seat seat : show.getScreen().getAllSeats()) {
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                available.add(seat);
            }
        }
        return available;
    }

    public Booking bookSeats(String showId, String userId, String userName, String userEmail,
            List<String> seatIds) {
        Show show = findShow(showId);
        User user = usersById.computeIfAbsent(userId, id -> new User(id, userName, userEmail));

        List<Seat> seatsToBook = new ArrayList<>();
        for (String seatId : seatIds) {
            Seat seat = show.getScreen().getSeat(seatId);
            if (seat == null) {
                throw new SeatNotAvailableException("Seat " + seatId + " does not exist on show " + showId);
            }
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new SeatNotAvailableException("Seat " + seatId + " is already booked for show " + showId);
            }
            seatsToBook.add(seat);
        }

        double totalAmount = 0.0;
        for (Seat seat : seatsToBook) {
            SeatPricingStrategy pricingStrategy = SeatPricingStrategyFactory.getStrategy(seat.getSeatType());
            totalAmount += pricingStrategy.calculatePrice(show.getBaseSeatPrice());
        }

        for (Seat seat : seatsToBook) {
            seat.setStatus(SeatStatus.BOOKED);
        }

        String bookingId = "BK" + bookingSequence.getAndIncrement();
        Booking booking = new Booking(bookingId, show, user, seatsToBook, totalAmount, LocalDateTime.now());
        bookingRepository.save(booking);

        for (BookingObserver observer : observers) {
            observer.onBookingConfirmed(booking);
        }
        return booking;
    }

    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId);
        if (booking == null || booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingNotFoundException("No confirmed booking found with id " + bookingId);
        }

        for (Seat seat : booking.getSeats()) {
            seat.setStatus(SeatStatus.AVAILABLE);
        }
        booking.setStatus(BookingStatus.CANCELLED);

        for (BookingObserver observer : observers) {
            observer.onBookingCancelled(booking);
        }
    }

    private Show findShow(String showId) {
        Show show = showRepository.findById(showId);
        if (show == null) {
            throw new ShowNotFoundException("No such show: " + showId);
        }
        return show;
    }

    private Movie findMovie(String movieId) {
        Movie movie = moviesById.get(movieId);
        if (movie == null) {
            throw new IllegalArgumentException("No such movie: " + movieId);
        }
        return movie;
    }

    private Theater findTheater(String theaterId) {
        Theater theater = theatersById.get(theaterId);
        if (theater == null) {
            throw new IllegalArgumentException("No such theater: " + theaterId);
        }
        return theater;
    }
}
