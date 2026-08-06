# -*- coding: utf-8 -*-
"""Regenerates parking-lot.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python parking-lot/diagrams/generate.py
This is the template other problems' diagrams/generate.py scripts copy —
it only supplies data (class fields/methods, edges, sequence messages);
all escaping/geometry logic lives in the shared module.
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "docs", "tooling"))
from drawio_uml import (uml_box, group_title, edge, lifeline, msg, selfcall,
                         frame, divider, note, page, write_mxfile, validate)

# ===========================================================================
# PAGE 1: CLASS DIAGRAM
# ===========================================================================
cells = []
COL = [40, 400, 760, 1120, 1480]
y = 20

cells.append(group_title(COL[0], y, "model — vehicle hierarchy"))
y += 34
box, vehicle_id, h1 = uml_box(COL[0], y, 340, "Vehicle", stereotype="abstract",
    attrs=["- licensePlate: String", "- type: VehicleType"],
    methods=["+ getLicensePlate(): String", "+ getType(): VehicleType",
             "+ getCompatibleSpotTypes(): List<SpotType> {abstract}"])
cells += box
box, moto_id, h2 = uml_box(COL[1], y, 300, "Motorcycle",
    methods=["+ getCompatibleSpotTypes(): List<SpotType>", "  // [SMALL, MEDIUM, LARGE]"])
cells += box
box, car_id, h3 = uml_box(COL[2], y, 300, "Car",
    methods=["+ getCompatibleSpotTypes(): List<SpotType>", "  // [MEDIUM, LARGE]"])
cells += box
box, truck_id, h4 = uml_box(COL[3], y, 300, "Truck",
    methods=["+ getCompatibleSpotTypes(): List<SpotType>", "  // [LARGE]"])
cells += box
box, vtype_id, h5 = uml_box(COL[4], y, 260, "VehicleType", stereotype="enumeration",
    attrs=["MOTORCYCLE", "CAR", "TRUCK"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(moto_id, vehicle_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(car_id, vehicle_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(truck_id, vehicle_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
cells.append(edge(vehicle_id, vtype_id, "association", "type", exitX="1", exitY="0.15", entryX="0", entryY="0.5"))
row1_bottom = y + max(h1, h2, h3, h4, h5)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "model — parking spot hierarchy"))
y += 34
box, spot_id, hs1 = uml_box(COL[0], y, 340, "ParkingSpot", stereotype="abstract",
    attrs=["- id: int", "- floorNumber: int", "- distanceFromExit: int",
           "- distanceFromElevator: int", "- occupied: boolean", "- parkedVehicle: Vehicle"],
    methods=["+ getDistanceFromExit(): int", "+ getDistanceFromElevator(): int",
              "+ getSpotType(): SpotType {abstract}", "+ getId(): int", "+ getFloorNumber(): int",
              "+ isOccupied(): boolean", "+ getParkedVehicle(): Vehicle",
              "+ assignVehicle(v: Vehicle): void", "+ release(): void", "+ getSpotCode(): String"])
cells += box
box, small_id, hs2 = uml_box(COL[1], y, 300, "SmallSpot", methods=["+ getSpotType(): SpotType"])
cells += box
box, medium_id, hs3 = uml_box(COL[2], y, 300, "MediumSpot", methods=["+ getSpotType(): SpotType"])
cells += box
box, large_id, hs4 = uml_box(COL[3], y, 300, "LargeSpot", methods=["+ getSpotType(): SpotType"])
cells += box
box, stype_id, hs5 = uml_box(COL[4], y, 260, "SpotType", stereotype="enumeration",
    attrs=["SMALL", "MEDIUM", "LARGE"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(small_id, spot_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(medium_id, spot_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(large_id, spot_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
cells.append(edge(spot_id, stype_id, "association", "spotType", exitX="1", exitY="0.15", entryX="0", entryY="0.5"))
row2_bottom = y + max(hs1, hs2, hs3, hs4, hs5)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "model — floor, ticket"))
y += 34
box, floor_id, hf1 = uml_box(COL[0], y, 300, "ParkingFloor",
    attrs=["- floorNumber: int", "- spots: List<ParkingSpot>"],
    methods=["+ getFloorNumber(): int", "+ addSpot(spot: ParkingSpot): void",
              "+ getSpots(): List<ParkingSpot>", "+ countAvailable(type: SpotType): long"])
cells += box
box, ticket_id, hf2 = uml_box(COL[1], y, 320, "Ticket",
    attrs=["- ticketId: String", "- vehicle: Vehicle", "- spot: ParkingSpot",
           "- entryTime: LocalDateTime", "- exitTime: LocalDateTime",
           "- status: TicketStatus", "- fee: double"],
    methods=["+ getVehicle(): Vehicle", "+ getSpot(): ParkingSpot",
              "+ getEntryTime()/getExitTime()/setExitTime()", "+ getStatus()/setStatus()",
              "+ getFee()/setFee()"])
cells += box
box, tstatus_id, hf3 = uml_box(COL[2], y, 260, "TicketStatus", stereotype="enumeration",
    attrs=["ACTIVE", "PAID"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(floor_id, spot_id, "composition", "spots  0..*",
                   exitX="0.5", exitY="0", entryX="0.4", entryY="1"))
cells.append(edge(ticket_id, vehicle_id, "association", "vehicle  1",
                   exitX="0.7", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(ticket_id, spot_id, "association", "spot  1",
                   exitX="0.3", exitY="0", entryX="0.85", entryY="1"))
cells.append(edge(ticket_id, tstatus_id, "association", "status", exitX="1", exitY="0.5", entryX="0", entryY="0.5"))
row3_bottom = y + max(hf1, hf2, hf3)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "factory & repository"))
y += 34
box, factory_id, hg1 = uml_box(COL[0], y, 340, "ParkingSpotFactory", stereotype="static factory",
    methods=["+ createSpot(type, id, floor,", "    distExit, distElevator): ParkingSpot {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, index_id, hg2 = uml_box(COL[1], y, 380, "SpotAvailabilityIndex",
    attrs=["- byExitDistance: Map<SpotType,PriorityQueue<ParkingSpot>>",
           "- byElevatorDistance: Map<SpotType,PriorityQueue<ParkingSpot>>"],
    methods=["+ register(spot): void", "+ peekNearestToExit(type): ParkingSpot",
              "+ peekNearestToElevator(type): ParkingSpot", "+ markUnavailable(spot): void",
              "+ markAvailable(spot): void"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(factory_id, spot_id, "dependency", "«creates»", exitX="1", exitY="0.3", entryX="0", entryY="0.95"))
cells.append(edge(index_id, spot_id, "aggregation", "holds free spots",
                   exitX="0.5", exitY="0", entryX="0.6", entryY="1"))
row4_bottom = y + max(hg1, hg2)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "strategy — pricing (lot-wide, swappable via setPricingStrategy)"))
y += 34
box, pricing_id, hp1 = uml_box(COL[0], y, 300, "PricingStrategy", stereotype="interface",
    methods=["+ calculateFee(ticket: Ticket): double"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, hourly_id, hp2 = uml_box(COL[1], y, 300, "HourlyPricingStrategy",
    attrs=["- hourlyRate: Map<VehicleType,Double>"],
    methods=["+ calculateFee(ticket): double", "  // ceil(minutes/60) * rate"])
cells += box
box, flat_id, hp3 = uml_box(COL[2], y, 300, "FlatRatePricingStrategy",
    attrs=["- flatRate: Map<VehicleType,Double>"],
    methods=["+ calculateFee(ticket): double", "  // ignores duration"])
cells += box
cells.append(edge(hourly_id, pricing_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.35"))
cells.append(edge(flat_id, pricing_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.65"))
row5_bottom = y + max(hp1, hp2, hp3)

y = row5_bottom + 70
cells.append(group_title(COL[0], y, "strategy — spot assignment (per parkVehicle() request)"))
y += 34
box, assign_id, ha1 = uml_box(COL[0], y, 340, "SpotAssignmentStrategy", stereotype="interface",
    methods=["+ selectSpot(type: SpotType,", "    index: SpotAvailabilityIndex): ParkingSpot"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, nexit_id, ha2 = uml_box(COL[1], y, 300, "NearestToExitStrategy",
    methods=["+ selectSpot(type, index): ParkingSpot", "  // peekNearestToExit(type)"])
cells += box
box, nelev_id, ha3 = uml_box(COL[2], y, 300, "NearestToElevatorStrategy",
    methods=["+ selectSpot(type, index): ParkingSpot", "  // peekNearestToElevator(type)"])
cells += box
box, anyspot_id, ha4 = uml_box(COL[3], y, 300, "AnySpotStrategy",
    methods=["+ selectSpot(type, index): ParkingSpot", "  // peekNearestToExit(type), no preference"])
cells += box
cells.append(edge(nexit_id, assign_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(nelev_id, assign_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(anyspot_id, assign_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
cells.append(edge(assign_id, index_id, "dependency", "uses", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
row6_bottom = y + max(ha1, ha2, ha3, ha4)

y = row6_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he1 = uml_box(COL[0], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, nospot_id, he2 = uml_box(COL[1], y, 300, "NoAvailableSpotException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, invtk_id, he3 = uml_box(COL[2], y, 300, "InvalidTicketException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, dup_id, he4 = uml_box(COL[3], y, 300, "VehicleAlreadyParkedException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(nospot_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(invtk_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(dup_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row7_bottom = y + max(he1, he2, he3, he4)

y = row7_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point (Singleton)"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 520, "ParkingLotService", stereotype="singleton",
    attrs=["- instance: ParkingLotService {static}", "- floors: List<ParkingFloor>",
           "- activeTicketsByPlate: Map<String,Ticket>", "- ticketSequence: AtomicInteger",
           "- availabilityIndex: SpotAvailabilityIndex", "- pricingStrategy: PricingStrategy",
           "- DEFAULT_ASSIGNMENT_STRATEGY: SpotAssignmentStrategy {static final}"],
    methods=["+ getInstance(): ParkingLotService {static}", "+ addFloor(floorNumber): void",
              "+ addSpot(floor, type, id, distExit, distElevator): void",
              "+ setPricingStrategy(strategy): void",
              "+ parkVehicle(vehicle[, entryTime][, assignmentStrategy]): Ticket",
              "+ unparkVehicle(plate[, exitTime]): double", "+ getStatusReport(): String",
              "- findFloor(floorNumber): ParkingFloor",
              "- findAvailableSpot(vehicle, assignmentStrategy): ParkingSpot"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, floor_id, "composition", "floors  0..*", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, index_id, "composition", "availabilityIndex  1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, pricing_id, "association", "pricingStrategy  1", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, assign_id, "dependency", "uses per call", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, ticket_id, "association", "activeTicketsByPlate  0..*", exitX="0.7", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.85", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — SETUP (addSpot)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("lot: ParkingLotService", 380), ("factory: ParkingSpotFactory", 680),
                 ("spot: ParkingSpot", 960), ("floor: ParkingFloor", 1220), ("index: SpotAvailabilityIndex", 1500)]:
    box, xx = lifeline(x, name, bottom=520)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["lot: ParkingLotService"], y, "addSpot(floor, type, id, distExit, distElevator)"))
y += 50
cells2.append(msg(xs["lot: ParkingLotService"], xs["factory: ParkingSpotFactory"], y, "createSpot(type, id, floor, distExit, distElevator)"))
y += 50
cells2.append(msg(xs["factory: ParkingSpotFactory"], xs["spot: ParkingSpot"], y, "«create» new SmallSpot/MediumSpot/LargeSpot(...)", kind="create"))
y += 50
cells2.append(msg(xs["factory: ParkingSpotFactory"], xs["lot: ParkingLotService"], y, "return spot", kind="return"))
y += 50
cells2.append(msg(xs["lot: ParkingLotService"], xs["floor: ParkingFloor"], y, "addSpot(spot)"))
y += 50
cells2.append(msg(xs["lot: ParkingLotService"], xs["index: SpotAvailabilityIndex"], y, "register(spot)"))
y += 30
cells2.append(note(xs["index: SpotAvailabilityIndex"] + 20, y, 260,
                    "adds spot to BOTH the exit-distance\nheap and elevator-distance heap\nfor its SpotType (spot starts free)"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — PARK VEHICLE
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("lot: ParkingLotService", 380), ("strategy: SpotAssignmentStrategy", 680),
                 ("index: SpotAvailabilityIndex", 980), ("spot: ParkingSpot", 1260), ("ticket: Ticket", 1520)]:
    box, xx = lifeline(x, name, bottom=980)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["lot: ParkingLotService"], y, "parkVehicle(vehicle, entryTime, assignmentStrategy)"))
y += 40
cells3.append(frame(xs["lot: ParkingLotService"] - 60, xs["ticket: Ticket"] - xs["lot: ParkingLotService"] + 160, y, 90,
                     "alt  [activeTicketsByPlate has this plate]"))
cells3.append(selfcall(xs["lot: ParkingLotService"], y + 25, "throw VehicleAlreadyParkedException", loop_w=70, loop_h=20))
cells3.append(divider(xs["lot: ParkingLotService"] - 60, xs["ticket: Ticket"] - xs["lot: ParkingLotService"] + 160, y + 55, "[else: plate is free]"))
y += 100
cells3.append(selfcall(xs["lot: ParkingLotService"], y, "findAvailableSpot(vehicle, assignmentStrategy)", loop_w=80, loop_h=22))
y += 40
cells3.append(frame(xs["lot: ParkingLotService"] - 40, xs["index: SpotAvailabilityIndex"] - xs["lot: ParkingLotService"] + 160, y, 190,
                     "loop  [for each compatible SpotType, best-fit (smallest) first]"))
y += 34
cells3.append(msg(xs["lot: ParkingLotService"], xs["strategy: SpotAssignmentStrategy"], y, "selectSpot(type, index)"))
y += 40
cells3.append(msg(xs["strategy: SpotAssignmentStrategy"], xs["index: SpotAvailabilityIndex"], y, "peekNearestToExit(type) / peekNearestToElevator(type)"))
y += 40
cells3.append(msg(xs["index: SpotAvailabilityIndex"], xs["strategy: SpotAssignmentStrategy"], y, "return spot | null", kind="return"))
y += 34
cells3.append(msg(xs["strategy: SpotAssignmentStrategy"], xs["lot: ParkingLotService"], y, "return spot | null  (loop breaks on first non-null)", kind="return"))
y += 60
cells3.append(msg(xs["lot: ParkingLotService"], xs["index: SpotAvailabilityIndex"], y, "markUnavailable(spot)   // removes from BOTH heaps"))
y += 50
cells3.append(msg(xs["lot: ParkingLotService"], xs["spot: ParkingSpot"], y, "assignVehicle(vehicle)"))
y += 50
cells3.append(msg(xs["lot: ParkingLotService"], xs["ticket: Ticket"], y, "«create» new Ticket(ticketId, vehicle, spot, entryTime)", kind="create"))
y += 50
cells3.append(selfcall(xs["lot: ParkingLotService"], y, "activeTicketsByPlate.put(plate, ticket)", loop_w=90, loop_h=22))
y += 50
cells3.append(msg(xs["lot: ParkingLotService"], xs[":Main"], y, "return ticket", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
# PAGE 4: SEQUENCE — UNPARK VEHICLE
# ===========================================================================
cells4 = []
xs = {}
for name, x in [(":Main", 100), ("lot: ParkingLotService", 380), ("ticket: Ticket", 680),
                 ("pricing: PricingStrategy", 980), ("spot: ParkingSpot", 1260), ("index: SpotAvailabilityIndex", 1520)]:
    box, xx = lifeline(x, name, bottom=900)
    cells4 += box
    xs[name] = xx

y = 120
cells4.append(msg(xs[":Main"], xs["lot: ParkingLotService"], y, "unparkVehicle(plate, exitTime)"))
y += 40
cells4.append(selfcall(xs["lot: ParkingLotService"], y, "activeTicketsByPlate.get(plate)", loop_w=80, loop_h=22))
y += 50
cells4.append(frame(xs["lot: ParkingLotService"] - 60, xs["index: SpotAvailabilityIndex"] - xs["lot: ParkingLotService"] + 160, y, 90,
                     "alt  [ticket == null or status != ACTIVE]"))
cells4.append(selfcall(xs["lot: ParkingLotService"], y + 25, "throw InvalidTicketException", loop_w=70, loop_h=20))
cells4.append(divider(xs["lot: ParkingLotService"] - 60, xs["index: SpotAvailabilityIndex"] - xs["lot: ParkingLotService"] + 160, y + 55, "[else: ticket is active]"))
y += 100
cells4.append(msg(xs["lot: ParkingLotService"], xs["ticket: Ticket"], y, "setExitTime(exitTime)"))
y += 50
cells4.append(msg(xs["lot: ParkingLotService"], xs["pricing: PricingStrategy"], y, "calculateFee(ticket)   // currently-active strategy"))
y += 40
cells4.append(msg(xs["pricing: PricingStrategy"], xs["ticket: Ticket"], y, "reads entryTime, exitTime, vehicle.type"))
y += 40
cells4.append(msg(xs["pricing: PricingStrategy"], xs["lot: ParkingLotService"], y, "return fee", kind="return"))
y += 50
cells4.append(msg(xs["lot: ParkingLotService"], xs["ticket: Ticket"], y, "setFee(fee)"))
y += 44
cells4.append(msg(xs["lot: ParkingLotService"], xs["ticket: Ticket"], y, "setStatus(PAID)"))
y += 44
cells4.append(msg(xs["lot: ParkingLotService"], xs["ticket: Ticket"], y, "getSpot()"))
y += 30
cells4.append(msg(xs["ticket: Ticket"], xs["lot: ParkingLotService"], y, "return spot", kind="return"))
y += 44
cells4.append(msg(xs["lot: ParkingLotService"], xs["spot: ParkingSpot"], y, "release()"))
y += 44
cells4.append(msg(xs["lot: ParkingLotService"], xs["index: SpotAvailabilityIndex"], y, "markAvailable(spot)   // adds back to BOTH heaps"))
y += 50
cells4.append(selfcall(xs["lot: ParkingLotService"], y, "activeTicketsByPlate.remove(plate)", loop_w=90, loop_h=22))
y += 50
cells4.append(msg(xs["lot: ParkingLotService"], xs[":Main"], y, "return fee", kind="return"))

PAGE4 = "\n".join(cells4)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "parking-lot.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2400),
    page("seqSetup", "2 - Sequence - Add Spot (setup)", PAGE2, w=1900, h=600),
    page("seqPark", "3 - Sequence - Park Vehicle", PAGE3, w=1900, h=1050),
    page("seqUnpark", "4 - Sequence - Unpark Vehicle", PAGE4, w=1900, h=950),
], outpath)
validate(outpath)
print("wrote", outpath)
