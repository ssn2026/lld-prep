# Parking Lot — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You have a parking garage with floors and spots. Someone builds the garage
(add floors, add spots). Then cars/bikes/trucks come in — the system finds
them a free spot and gives them a ticket. Later they come back to that spot,
the system calculates how much they owe based on how long they parked, and
frees the spot back up for the next vehicle. That's it — parking in, paying,
parking out.

---

## 2. The one door you're allowed to knock on

`src/services/ParkingLotService.java` is the **only** class anything outside
the package is meant to call. Every other folder (`model`, `strategy`,
`factory`, `repository`, `exceptions`) is a helper that `ParkingLotService`
uses internally. If you're trying to understand "what can this system do?",
just read the public methods on this one file:

| Method | What it does |
|---|---|
| `getInstance()` | Get the one-and-only parking lot (see step 3 below) |
| `addFloor(floorNumber)` | Add an empty floor |
| `addSpot(floor, type, id, distExit, distElevator)` | Add one physical parking spot |
| `parkVehicle(vehicle, ...)` | Park a vehicle, get back a `Ticket` |
| `unparkVehicle(plate, ...)` | Pay and free up the spot, get back the fee |
| `setPricingStrategy(...)` | Change how fees are calculated, for everyone |
| `getStatusReport()` | See how many spots are free per floor |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)
Start here. These are just data — no clever logic.

- `Vehicle.java` (+ `Car`, `Motorcycle`, `Truck`) — a vehicle has a license
  plate and a type. Each subclass says which spot sizes it's allowed to use
  (`getCompatibleSpotTypes()`). A `Truck` can only use `LARGE`. A
  `Motorcycle` can use `SMALL`, `MEDIUM`, or `LARGE`.
- `ParkingSpot.java` (+ `SmallSpot`, `MediumSpot`, `LargeSpot`) — a spot has
  an id, a floor number, whether it's occupied, and two numbers:
  `distanceFromExit` and `distanceFromElevator` (just "how far is this spot
  from the door / from the lift", made up numbers you assign when creating
  the spot).
- `ParkingFloor.java` — just a list of spots that live on one floor.
- `Ticket.java` — created when a vehicle parks. Stores who parked, where,
  when, and (later) how much they paid.

**Nothing here decides anything. It's just storage.**

### Step 2 — how a spot gets created (`src/factory/ParkingSpotFactory.java`)
One method: `createSpot(type, id, floor, distExit, distElevator)`. Give it a
`SpotType` enum value, it hands back the matching Java object
(`SmallSpot`/`MediumSpot`/`LargeSpot`). This exists so nothing else in the
codebase needs a big `if/switch` on spot type — just this one file.

### Step 3 — where free spots are tracked (`src/repository/SpotAvailabilityIndex.java`)
This is the "address book" of which spots are currently free. For each spot
size (`SMALL`/`MEDIUM`/`LARGE`) it keeps **two lists**, always sorted:
- one sorted by "closest to the exit first"
- one sorted by "closest to the elevator first"

(Technically these are min-heaps / `PriorityQueue`s — a sorted list that's
cheap to pull the smallest item out of, without re-sorting everything.)

Three operations only:
- `register(spot)` — a brand-new spot is free, add it to both lists.
- `markUnavailable(spot)` — a spot just got taken, remove it from both lists.
- `markAvailable(spot)` — a spot just got freed, add it back to both lists.

### Step 4 — how a specific spot gets picked (`src/strategy/`)
Two *different* decisions live in this folder, and it's easy to confuse them:

**A) "Which physical spot do I hand out?"** — `SpotAssignmentStrategy` +
3 implementations:
- `NearestToExitStrategy` — look at the "closest to exit" list, take the top.
- `NearestToElevatorStrategy` — look at the "closest to elevator" list, take
  the top.
- `AnySpotStrategy` — don't care, just take whatever's on top of the exit
  list (reused only because it's a convenient list of free spots, the
  ordering means nothing here).

You pick one of these **per vehicle**, at the moment you call `parkVehicle`.

**B) "How much do I charge?"** — `PricingStrategy` + 2 implementations:
- `HourlyPricingStrategy` — charge per hour parked (rounded up), rate
  depends on vehicle type.
- `FlatRatePricingStrategy` — one fixed price per vehicle type, no matter
  how long they stayed.

You pick one of these **for the whole parking lot**, via
`service.setPricingStrategy(...)` — it stays active until you change it
again.

### Step 5 — the orchestrator (`src/services/ParkingLotService.java`)
Now that you've seen all the pieces, this file just wires them together:

```
parkVehicle(vehicle, time, assignmentStrategy):
    if vehicle's plate already has an active ticket -> throw error
    ask assignmentStrategy to pick a free spot (loops through vehicle's
        allowed sizes, smallest-fitting size first)
    mark that spot as taken (in the spot itself, AND in the availability index)
    create a Ticket, remember it by license plate
    return the ticket

unparkVehicle(plate, time):
    look up the active ticket for this plate -> if none, throw error
    ask the current pricingStrategy to calculate the fee
    mark the spot as free again (in the spot itself, AND in the index)
    forget the ticket
    return the fee
```

### Step 6 — errors (`src/exceptions/`)
Three plain-English failure cases, each its own exception class:
- `VehicleAlreadyParkedException` — tried to park a plate that's already in.
- `NoAvailableSpotException` — no free spot fits this vehicle anywhere.
- `InvalidTicketException` — tried to unpark a plate with no active ticket.

### Step 7 — the runner (`src/Main.java`)
Not part of the "real" system — it's a test harness. It reads a text file
line by line (`test/input/scenario.txt`), turns each line into a call on
`ParkingLotService`, and writes what happened to
`test/output/output.txt`. This is how you can "run" the whole design without
writing a UI.

---

## 4. Picture of one park + unpark

```
Main.java (reads "PARK CAR KA01C1111 0 ELEVATOR" from the script)
   |
   v
ParkingLotService.parkVehicle(car, time, NearestToElevatorStrategy)
   |
   v
NearestToElevatorStrategy.selectSpot(MEDIUM, availabilityIndex)
   |
   v
SpotAvailabilityIndex --> peeks the "closest to elevator" heap for MEDIUM
   |
   v
returns e.g. spot F2-M2
   |
   v
ParkingLotService: mark F2-M2 unavailable, create Ticket, store it
   |
   v
Main.java prints: "OK parked KA01C1111 -> ticket T1 spot F2-M2"


... later ...

Main.java (reads "UNPARK KA01C1111 90")
   |
   v
ParkingLotService.unparkVehicle("KA01C1111", time)
   |
   v
find the active Ticket for that plate
   |
   v
HourlyPricingStrategy.calculateFee(ticket)   <- or FlatRatePricingStrategy, whichever is active
   |
   v
mark F2-M2 free again, forget the ticket
   |
   v
Main.java prints: "OK unparked KA01C1111 -> fee $XX.0"
```

---

## 5. Things to try / test yourself

You can edit `test/input/scenario.txt` and re-run with:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Things worth trying, and what you should expect to see:

1. **Fill up one spot size completely, then try one more.**
   Add `PARK TRUCK ...` lines until every `LARGE` spot is taken, then one
   more `PARK TRUCK ...`. Expect `ERROR NoAvailableSpotException`.

2. **Park the same plate twice without unparking.**
   Two `PARK CAR KA01C9999 0` lines in a row. Second one should error with
   `VehicleAlreadyParkedException`.

3. **Unpark a plate that was never parked.**
   `UNPARK KA01C0000 0` with no matching `PARK` before it. Expect
   `InvalidTicketException`.

4. **Compare EXIT vs ELEVATOR vs ANY on the same layout.**
   Give two same-size spots very different exit/elevator distances (e.g.
   spot A: exit=1, elevator=10; spot B: exit=10, elevator=1). Park one
   vehicle with `EXIT` and another with `ELEVATOR` and confirm they land on
   different spots (A and B respectively).

5. **Check hourly billing rounds up.**
   Park at minute 0, unpark at minute 61 with `HOURLY` (the default)
   pricing. You should be charged for **2 hours**, not 1 — the code rounds
   any partial hour up (`Math.ceil`).

6. **Switch pricing strategy mid-run.**
   Park two vehicles, do `STRATEGY FLAT`, then unpark both. The fee should
   now ignore how long they were parked — same flat number regardless of
   duration.

7. **Release and re-park the same spot.**
   Unpark a vehicle, then immediately park a new one that wants the same
   size/preference. Confirm it can get the just-freed spot back (proves the
   free-spot heaps are updated correctly on release).

8. **Break something on purpose.**
   Try `PARK CAR KA01C1111 abc` (a non-number for the time) or `SPOT 99 ...`
   (a floor that doesn't exist) — see what error you get and trace it back
   to which line of code threw it.
