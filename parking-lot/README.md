# Parking Lot

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A multi-floor parking lot that assigns vehicles to spots (optionally nearest
to the exit or elevator), issues tickets, and charges a fee on exit based on
the currently configured pricing strategy.

## Happy flow

1. A vehicle arrives with an optional proximity preference (nearest exit,
   nearest elevator, or none); `ParkingLotService.parkVehicle()` asks the
   `SpotAssignmentStrategy` to pick the best free compatible spot.
2. A `Ticket` is issued recording the spot, vehicle, and entry time; the spot
   is marked occupied and removed from the free-spot index.
3. On exit, `ParkingLotService.unparkVehicle()` computes the fee via the
   active `PricingStrategy`, marks the ticket paid, frees the spot, and
   returns it to the free-spot index.

## Design patterns used

- **Singleton** — `services/ParkingLotService.java`. Only one parking lot
  instance should exist per process; `getInstance()` guards that.
- **Factory** — `factory/ParkingSpotFactory.java`. Builds the correct
  `ParkingSpot` subclass (`SmallSpot`/`MediumSpot`/`LargeSpot`) from a
  `SpotType`, so the service never has to know the concrete spot classes.
- **Strategy** (two independent families):
  - `strategy/PricingStrategy.java` with `HourlyPricingStrategy` and
    `FlatRatePricingStrategy` — the fee algorithm, swappable lot-wide at
    runtime via `ParkingLotService.setPricingStrategy()`.
  - `strategy/SpotAssignmentStrategy.java` with `NearestToExitStrategy`,
    `NearestToElevatorStrategy`, and `AnySpotStrategy` — which free spot to
    hand out, chosen per parking request (passed straight into
    `parkVehicle()`), not fixed lot-wide like pricing.

## Free-spot lookup: dual min-heap index

`repository/SpotAvailabilityIndex.java` keeps two `PriorityQueue<ParkingSpot>`
per `SpotType` — one ordered by `distanceFromExit`, one by
`distanceFromElevator` — containing only currently-free spots. Each
`SpotAssignmentStrategy` just peeks the head of the relevant heap: O(log n)
instead of the O(n) linear scan the first version of this design used.

Both heaps are always kept in sync so neither ever holds a stale (occupied)
or duplicate entry:
- `register(spot)` — spot created → add to both heaps.
- `markUnavailable(spot)` — spot assigned → remove from both heaps.
- `markAvailable(spot)` — spot released → add back to both heaps.

Because `markUnavailable`/`markAvailable` always touch *both* heaps together
regardless of which one the strategy peeked, there's no lazy-deletion or
lingering-duplicate bookkeeping to get wrong.

## Structure

```
parking-lot/
  src/
    model/       Vehicle, ParkingSpot hierarchies (now carry exit/elevator distance), ParkingFloor, Ticket, enums
    strategy/    PricingStrategy family + SpotAssignmentStrategy family
    factory/     ParkingSpotFactory
    repository/  SpotAvailabilityIndex (dual min-heap free-spot index)
    exceptions/  NoAvailableSpotException, InvalidTicketException, VehicleAlreadyParkedException
    services/    ParkingLotService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Command script covering happy path, exit/elevator/any preference, and edge cases
    output/output.txt    Captured run transcript
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

## Known gaps (flagged, not fixed)

- Fee is computed using whichever `PricingStrategy` is active **at unpark
  time**, not the one active at entry time. The test scenario relies on this
  (see the `STRATEGY FLAT` mid-run switch) — it's a reasonable real-world
  choice but worth calling out explicitly.
- No concurrency control: `parkVehicle`/`unparkVehicle` aren't synchronized,
  so two threads racing for the same spot could both see it as free — this
  now also applies to the two heaps in `SpotAvailabilityIndex`, which are
  plain `PriorityQueue`s (not thread-safe).
- `AnySpotStrategy` reuses the exit-distance heap for tie-breaking purely as
  a convenient pool; on an exact tie between two spots' exit distances, which
  one comes back is whatever Java's binary-heap internal ordering produces —
  not a meaningful business rule, just an implementation artifact.
- No persistence — all state is in-memory and lost on process exit.
