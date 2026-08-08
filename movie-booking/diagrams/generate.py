# -*- coding: utf-8 -*-
"""Regenerates movie-booking.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python movie-booking/diagrams/generate.py
Copied from parking-lot/diagrams/generate.py's structure per CLAUDE.md -
only data (class fields/methods, edges, sequence messages) lives here; all
escaping/geometry logic lives in the shared module.
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

cells.append(group_title(COL[0], y, "model — show setup"))
y += 34
box, movie_id, h1 = uml_box(COL[0], y, 300, "Movie",
    attrs=["- movieId: String", "- title: String", "- durationMinutes: int"],
    methods=["+ getMovieId(): String", "+ getTitle(): String", "+ getDurationMinutes(): int"])
cells += box
box, theater_id, h2 = uml_box(COL[1], y, 300, "Theater",
    attrs=["- theaterId: String", "- name: String", "- city: String"],
    methods=["+ getTheaterId(): String", "+ getName(): String", "+ getCity(): String"])
cells += box
box, screen_id, h3 = uml_box(COL[2], y, 340, "Screen",
    attrs=["- screenId: String", "- name: String", "- seatsById: Map<String,Seat>"],
    methods=["+ getSeat(seatId): Seat", "+ getAllSeats(): List<Seat>",
             "- generateSeats(rows, seatsPerRow): void", "- seatTypeForRow(rowIndex, totalRows): SeatType"])
cells += box
box, show_id, h4 = uml_box(COL[3], y, 320, "Show",
    attrs=["- showId: String", "- movie: Movie", "- theater: Theater", "- screen: Screen",
           "- startTime: LocalDateTime", "- baseSeatPrice: double"],
    methods=["+ getShowId(): String", "+ getMovie()/getTheater()/getScreen(): ...",
             "+ getStartTime(): LocalDateTime", "+ getBaseSeatPrice(): double"])
cells += box
cells.append(edge(show_id, movie_id, "association", "movie  1", exitX="0.2", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(show_id, theater_id, "association", "theater  1", exitX="0.4", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(show_id, screen_id, "association", "screen  1", exitX="0.6", exitY="0", entryX="0.5", entryY="1"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "model — seats, users, bookings"))
y += 34
box, seat_id, hs1 = uml_box(COL[0], y, 300, "Seat",
    attrs=["- seatId: String", "- row: char", "- number: int",
           "- seatType: SeatType", "- status: SeatStatus"],
    methods=["+ getSeatId(): String", "+ getSeatType(): SeatType",
             "+ getStatus()/setStatus(status): ..."])
cells += box
box, stype_id, hs2 = uml_box(COL[1], y, 220, "SeatType", stereotype="enumeration",
    attrs=["REGULAR", "PREMIUM", "RECLINER"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, sstatus_id, hs3 = uml_box(COL[1], y + 130, 220, "SeatStatus", stereotype="enumeration",
    attrs=["AVAILABLE", "BOOKED"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, user_id, hs4 = uml_box(COL[2], y, 300, "User",
    attrs=["- userId: String", "- name: String", "- email: String"],
    methods=["+ getUserId()/getName()/getEmail(): String"])
cells += box
box, booking_id, hs5 = uml_box(COL[3], y, 340, "Booking",
    attrs=["- bookingId: String", "- show: Show", "- user: User", "- seats: List<Seat>",
           "- totalAmount: double", "- bookingTime: LocalDateTime", "- status: BookingStatus"],
    methods=["+ getBookingId(): String", "+ getShow()/getUser()/getSeats(): ...",
             "+ getTotalAmount(): double", "+ getStatus()/setStatus(status): ..."])
cells += box
box, bstatus_id, hs6 = uml_box(COL[4], y, 220, "BookingStatus", stereotype="enumeration",
    attrs=["CONFIRMED", "CANCELLED"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(seat_id, stype_id, "association", "seatType", exitX="1", exitY="0.3", entryX="0", entryY="0.5"))
cells.append(edge(seat_id, sstatus_id, "association", "status", exitX="1", exitY="0.7", entryX="0", entryY="0.5"))
cells.append(edge(screen_id, seat_id, "composition", "seats  0..*", exitX="0.3", exitY="1", entryX="0.5", entryY="0", ))
cells.append(edge(booking_id, show_id, "association", "show  1", exitX="0.5", exitY="0", entryX="0.8", entryY="1"))
cells.append(edge(booking_id, user_id, "association", "user  1", exitX="0.1", exitY="0", entryX="1", entryY="0.5"))
cells.append(edge(booking_id, seat_id, "association", "seats  1..*", exitX="0.3", exitY="0", entryX="1", entryY="0.5"))
cells.append(edge(booking_id, bstatus_id, "association", "status", exitX="1", exitY="0.5", entryX="0", entryY="0.5"))
row2_bottom = y + max(hs1, hs2 + 130 + hs3, hs4, hs5, hs6)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "strategy — seat pricing (chosen per SeatType via factory)"))
y += 34
box, pricing_id, hp1 = uml_box(COL[0], y, 300, "SeatPricingStrategy", stereotype="interface",
    methods=["+ calculatePrice(baseSeatPrice): double"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, regular_id, hp2 = uml_box(COL[1], y, 280, "RegularPricingStrategy",
    methods=["+ calculatePrice(base): double", "  // base * 1.0"])
cells += box
box, premium_id, hp3 = uml_box(COL[2], y, 280, "PremiumPricingStrategy",
    methods=["+ calculatePrice(base): double", "  // base * 1.5"])
cells += box
box, recliner_id, hp4 = uml_box(COL[3], y, 280, "ReclinerPricingStrategy",
    methods=["+ calculatePrice(base): double", "  // base * 2.0"])
cells += box
cells.append(edge(regular_id, pricing_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(premium_id, pricing_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(recliner_id, pricing_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row3_bottom = y + max(hp1, hp2, hp3, hp4)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "factory & observer"))
y += 34
box, factory_id, hg1 = uml_box(COL[0], y, 340, "SeatPricingStrategyFactory", stereotype="static factory",
    methods=["+ getStrategy(seatType: SeatType): SeatPricingStrategy {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, bobs_id, ho1 = uml_box(COL[1], y, 300, "BookingObserver", stereotype="interface",
    methods=["+ onBookingConfirmed(booking): void", "+ onBookingCancelled(booking): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, email_id, ho2 = uml_box(COL[2], y, 300, "EmailNotificationObserver",
    methods=["+ onBookingConfirmed(booking): void", "+ onBookingCancelled(booking): void"])
cells += box
box, sms_id, ho3 = uml_box(COL[3], y, 300, "SmsNotificationObserver",
    methods=["+ onBookingConfirmed(booking): void", "+ onBookingCancelled(booking): void"])
cells += box
cells.append(edge(factory_id, pricing_id, "dependency", "«creates»", exitX="1", exitY="0.3", entryX="0.2", entryY="1"))
cells.append(edge(email_id, bobs_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(sms_id, bobs_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row4_bottom = y + max(hg1, ho1, ho2, ho3)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "repository & exceptions"))
y += 34
box, showrepo_id, hr1 = uml_box(COL[0], y, 340, "ShowRepository",
    attrs=["- showsById: Map<String,Show>"],
    methods=["+ save(show): void", "+ findById(showId): Show",
             "+ findByMovieTitleAndCity(title, city): List<Show>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, bookrepo_id, hr2 = uml_box(COL[1], y, 300, "BookingRepository",
    attrs=["- bookingsById: Map<String,Booking>"],
    methods=["+ save(booking): void", "+ findById(bookingId): Booking"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, rte_id, hr3 = uml_box(COL[2], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, shownf_id, hr4 = uml_box(COL[3], y, 280, "ShowNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, seatna_id, hr5 = uml_box(COL[3], y + 90, 280, "SeatNotAvailableException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, booknf_id, hr6 = uml_box(COL[3], y + 180, 280, "BookingNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(showrepo_id, show_id, "dependency", "stores", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(bookrepo_id, booking_id, "dependency", "stores", exitX="0.5", exitY="0", entryX="0.4", entryY="1"))
cells.append(edge(shownf_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(seatna_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
cells.append(edge(booknf_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row5_bottom = y + max(hr1, hr2, hr3, hr4 + 180 + hr6)

y = row5_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "MovieBookingService",
    attrs=["- theatersById: Map<String,Theater>", "- moviesById: Map<String,Movie>",
           "- usersById: Map<String,User>", "- showRepository: ShowRepository",
           "- bookingRepository: BookingRepository", "- observers: List<BookingObserver>",
           "- bookingSequence: AtomicInteger"],
    methods=["+ registerObserver(observer): void",
             "+ addTheater(id, name, city): Theater", "+ addMovie(id, title, durationMinutes): Movie",
             "+ addShow(showId, movieId, theaterId, screenId, screenName, rows, seatsPerRow,",
             "    startTime, baseSeatPrice): Show",
             "+ searchShows(movieTitle, city): List<Show>", "+ getAvailableSeats(showId): List<Seat>",
             "+ bookSeats(showId, userId, userName, userEmail, seatIds): Booking",
             "+ cancelBooking(bookingId): void",
             "- findShow(showId)/findMovie(movieId)/findTheater(theaterId): ..."],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, showrepo_id, "composition", "showRepository  1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, bookrepo_id, "composition", "bookingRepository  1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, bobs_id, "aggregation", "observers  0..*", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, theater_id, "association", "theatersById  0..*", exitX="0.7", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, movie_id, "association", "moviesById  0..*", exitX="0.85", exitY="0", entryX="0.7", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — BOOK SEATS
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: MovieBookingService", 380), ("show: Show", 680),
                 ("factory: SeatPricingStrategyFactory", 980), ("booking: Booking", 1300),
                 ("observer: BookingObserver", 1580)]:
    box, xx = lifeline(x, name, bottom=1020)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: MovieBookingService"], y, "bookSeats(showId, userId, userName, userEmail, seatIds)"))
y += 50
cells2.append(selfcall(xs["svc: MovieBookingService"], y, "findShow(showId)", loop_w=80, loop_h=22))
y += 50
cells2.append(frame(xs["svc: MovieBookingService"] - 60, xs["booking: Booking"] - xs["svc: MovieBookingService"] + 160, y, 130,
                     "loop  [for each requested seatId]"))
y += 34
cells2.append(msg(xs["svc: MovieBookingService"], xs["show: Show"], y, "getScreen().getSeat(seatId)"))
y += 40
cells2.append(msg(xs["show: Show"], xs["svc: MovieBookingService"], y, "return seat", kind="return"))
y += 40
cells2.append(frame(xs["svc: MovieBookingService"] - 40, 300, y, 40,
                     "alt  [seat missing or not AVAILABLE] -> throw SeatNotAvailableException"))
y += 66
cells2.append(msg(xs["svc: MovieBookingService"], xs["factory: SeatPricingStrategyFactory"], y, "getStrategy(seat.getSeatType())"))
y += 40
cells2.append(msg(xs["factory: SeatPricingStrategyFactory"], xs["svc: MovieBookingService"], y, "return pricingStrategy", kind="return"))
y += 40
cells2.append(selfcall(xs["svc: MovieBookingService"], y, "totalAmount += pricingStrategy.calculatePrice(baseSeatPrice)", loop_w=100, loop_h=22))
y += 60
cells2.append(selfcall(xs["svc: MovieBookingService"], y, "seat.setStatus(BOOKED)", loop_w=80, loop_h=22))
y += 70
cells2.append(msg(xs["svc: MovieBookingService"], xs["booking: Booking"], y, "«create» new Booking(bookingId, show, user, seats, totalAmount, now)", kind="create"))
y += 50
cells2.append(frame(xs["svc: MovieBookingService"] - 60, xs["observer: BookingObserver"] - xs["svc: MovieBookingService"] + 160, y, 90,
                     "loop  [for each registered observer]"))
y += 34
cells2.append(msg(xs["svc: MovieBookingService"], xs["observer: BookingObserver"], y, "onBookingConfirmed(booking)"))
y += 60
cells2.append(msg(xs["svc: MovieBookingService"], xs[":Main"], y, "return booking", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — CANCEL BOOKING
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: MovieBookingService", 380), ("booking: Booking", 680),
                 ("seat: Seat", 960), ("observer: BookingObserver", 1220)]:
    box, xx = lifeline(x, name, bottom=680)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: MovieBookingService"], y, "cancelBooking(bookingId)"))
y += 40
cells3.append(selfcall(xs["svc: MovieBookingService"], y, "bookingRepository.findById(bookingId)", loop_w=90, loop_h=22))
y += 50
cells3.append(frame(xs["svc: MovieBookingService"] - 60, xs["observer: BookingObserver"] - xs["svc: MovieBookingService"] + 160, y, 90,
                     "alt  [booking == null or already CANCELLED]"))
cells3.append(selfcall(xs["svc: MovieBookingService"], y + 25, "throw BookingNotFoundException", loop_w=70, loop_h=20))
cells3.append(divider(xs["svc: MovieBookingService"] - 60, xs["observer: BookingObserver"] - xs["svc: MovieBookingService"] + 160, y + 55, "[else: booking is CONFIRMED]"))
y += 100
cells3.append(frame(xs["svc: MovieBookingService"] - 40, xs["seat: Seat"] - xs["svc: MovieBookingService"] + 160, y, 80,
                     "loop  [for each seat in booking]"))
y += 34
cells3.append(msg(xs["svc: MovieBookingService"], xs["seat: Seat"], y, "setStatus(AVAILABLE)"))
y += 66
cells3.append(msg(xs["svc: MovieBookingService"], xs["booking: Booking"], y, "setStatus(CANCELLED)"))
y += 50
cells3.append(frame(xs["svc: MovieBookingService"] - 60, xs["observer: BookingObserver"] - xs["svc: MovieBookingService"] + 160, y, 90,
                     "loop  [for each registered observer]"))
y += 34
cells3.append(msg(xs["svc: MovieBookingService"], xs["observer: BookingObserver"], y, "onBookingCancelled(booking)"))
y += 60
cells3.append(msg(xs["svc: MovieBookingService"], xs[":Main"], y, "return", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "movie-booking.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2800),
    page("seqBook", "2 - Sequence - Book Seats", PAGE2, w=1900, h=1100),
    page("seqCancel", "3 - Sequence - Cancel Booking", PAGE3, w=1600, h=750),
], outpath)
validate(outpath)
print("wrote", outpath)
