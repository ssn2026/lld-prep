import exceptions.BookingNotFoundException;
import exceptions.SeatNotAvailableException;
import exceptions.ShowNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import model.Booking;
import model.Seat;
import model.Show;
import observer.EmailNotificationObserver;
import observer.SmsNotificationObserver;
import services.MovieBookingService;

/**
 * Drives MovieBookingService from a plain-text command script so the design
 * can be exercised end-to-end without a UI. Anchors simulated clock time to
 * make show start times deterministic and reproducible.
 */
public class Main {

    private static final LocalDateTime ANCHOR = LocalDateTime.of(2026, 8, 8, 9, 0);

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Main <input-script-path> [output-path]");
            System.exit(1);
        }
        Path inputPath = Path.of(args[0]);
        StringBuilder output = new StringBuilder();

        MovieBookingService service = new MovieBookingService();
        service.registerObserver(new EmailNotificationObserver());
        service.registerObserver(new SmsNotificationObserver());

        List<String> lines = Files.readAllLines(inputPath);
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            log(output, "> " + line);
            String result = execute(service, line);
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

    private static String execute(MovieBookingService service, String line) {
        String[] parts = line.split("\\s+");
        String command = parts[0];
        try {
            return switch (command) {
                case "THEATER" -> {
                    service.addTheater(parts[1], parts[2], parts[3]);
                    yield "OK theater " + parts[1] + " (" + parts[2] + ", " + parts[3] + ") added";
                }
                case "MOVIE" -> {
                    int durationMinutes = Integer.parseInt(parts[3]);
                    service.addMovie(parts[1], parts[2], durationMinutes);
                    yield "OK movie " + parts[1] + " '" + parts[2] + "' (" + durationMinutes + " min) added";
                }
                case "SHOW" -> {
                    String showId = parts[1];
                    String movieId = parts[2];
                    String theaterId = parts[3];
                    String screenId = parts[4];
                    String screenName = parts[5];
                    int rows = Integer.parseInt(parts[6]);
                    int seatsPerRow = Integer.parseInt(parts[7]);
                    long startOffsetMinutes = Long.parseLong(parts[8]);
                    double basePrice = Double.parseDouble(parts[9]);
                    Show show = service.addShow(showId, movieId, theaterId, screenId, screenName,
                            rows, seatsPerRow, ANCHOR.plusMinutes(startOffsetMinutes), basePrice);
                    yield "OK show " + showId + " added: " + show.getMovie().getTitle()
                            + " at " + show.getTheater().getName() + " screen " + screenName
                            + " (" + (rows * seatsPerRow) + " seats, base $" + basePrice + ")";
                }
                case "SEARCH" -> {
                    String movieTitle = parts[1];
                    String city = parts[2];
                    List<Show> shows = service.searchShows(movieTitle, city);
                    if (shows.isEmpty()) {
                        yield "SEARCH no shows found for '" + movieTitle + "' in " + city;
                    }
                    StringBuilder sb = new StringBuilder("SEARCH found " + shows.size() + " show(s):");
                    for (Show show : shows) {
                        sb.append("\n  ").append(show.getShowId()).append(" @ ")
                                .append(show.getTheater().getName()).append(" ").append(show.getStartTime());
                    }
                    yield sb.toString();
                }
                case "SEATS" -> {
                    String showId = parts[1];
                    List<Seat> available = service.getAvailableSeats(showId);
                    StringBuilder sb = new StringBuilder("SEATS " + available.size() + " available: ");
                    for (Seat seat : available) {
                        sb.append(seat.getSeatId()).append('(').append(seat.getSeatType()).append(") ");
                    }
                    yield sb.toString().stripTrailing();
                }
                case "BOOK" -> {
                    String showId = parts[1];
                    String userId = parts[2];
                    String userName = parts[3];
                    String userEmail = parts[4];
                    List<String> seatIds = Arrays.asList(parts[5].split(","));
                    Booking booking = service.bookSeats(showId, userId, userName, userEmail, seatIds);
                    yield "OK booked " + seatIds + " -> booking " + booking.getBookingId()
                            + " total $" + booking.getTotalAmount();
                }
                case "CANCEL" -> {
                    String bookingId = parts[1];
                    service.cancelBooking(bookingId);
                    yield "OK cancelled booking " + bookingId;
                }
                default -> "ERROR unknown command: " + command;
            };
        } catch (ShowNotFoundException | SeatNotAvailableException | BookingNotFoundException
                | IllegalArgumentException e) {
            return "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
