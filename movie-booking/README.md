# Movie Booking System

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A booking system where users search for shows by movie/city, check seat
availability, book a set of seats (priced by seat type), and cancel
bookings — with notifications fired on every confirm/cancel.

## Happy flow

1. Admin sets up `Theater` → `Screen` (auto-generates its `Seat` grid, front
   rows REGULAR, middle PREMIUM, back RECLINER) → `Movie` → `Show` (movie +
   theater + screen + start time + base seat price).
2. A user calls `MovieBookingService.searchShows()` by movie title + city,
   then `getAvailableSeats()` for a chosen show.
3. `bookSeats()` validates every requested seat is `AVAILABLE`, prices each
   one via the `SeatPricingStrategy` looked up for its `SeatType`, marks the
   seats `BOOKED`, creates a `CONFIRMED` `Booking`, and notifies every
   registered `BookingObserver`.
4. `cancelBooking()` reverses this: seats go back to `AVAILABLE`, the
   booking flips to `CANCELLED`, observers are notified again.

## Design patterns used

- **Strategy** — `strategy/SeatPricingStrategy.java` with
  `RegularPricingStrategy` (1.0x), `PremiumPricingStrategy` (1.5x), and
  `ReclinerPricingStrategy` (2.0x). The price multiplier per seat type is
  swappable business logic, independent of everything else.
- **Factory** — `factory/SeatPricingStrategyFactory.java` maps
  `SeatType -> SeatPricingStrategy` so `MovieBookingService` never
  branches on seat type itself — it just asks the factory for "the"
  strategy for a given seat.
- **Observer** — `observer/BookingObserver.java` with
  `EmailNotificationObserver` and `SmsNotificationObserver`. The service
  fires `onBookingConfirmed`/`onBookingCancelled` without knowing or caring
  who's listening; both observers are registered in `Main` at startup.

## Structure

```
movie-booking/
  src/
    model/       Movie, Theater, Screen (generates its own Seat grid), Seat,
                 Show, User, Booking, and their enums (SeatType, SeatStatus, BookingStatus)
    strategy/    SeatPricingStrategy family
    factory/     SeatPricingStrategyFactory
    observer/    BookingObserver family
    repository/  ShowRepository, BookingRepository (in-memory)
    exceptions/  ShowNotFoundException, SeatNotAvailableException, BookingNotFoundException
    services/    MovieBookingService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Command script covering search, booking, pricing mix, cancel, and edge cases
    output/output.txt    Captured run transcript
  explainer/index.html   Interactive step-through: pick a show and seats, tap "Next step" to watch
                          the real bookSeats()/cancelBooking() call chain execute with live values
                          (open directly in a browser)
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open `movie-booking/` itself as
the workspace root.

## Known gaps (flagged, not fixed)

- No seat locking / hold-with-timeout: a seat goes straight from
  `AVAILABLE` to `BOOKED` inside `bookSeats()`. Two concurrent requests for
  the same seat aren't safety-checked against each other (no
  synchronization), so a real system would need a short-lived "locked"
  state during payment.
- Cancellation doesn't model refunds/payment reversal beyond printing the
  refund amount in the notification — there's no `Payment` entity.
- `Screen`'s row-based seat-type split (40% REGULAR / 30% PREMIUM / 30%
  RECLINER) is a fixed heuristic, not configurable per screen.
- No persistence — all state is in-memory and lost on process exit.
