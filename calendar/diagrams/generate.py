# -*- coding: utf-8 -*-
"""Regenerates calendar.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python calendar/diagrams/generate.py
Copied from parking-lot/diagrams/generate.py's structure per CLAUDE.md --
only supplies data (class fields/methods, edges, sequence messages); all
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

cells.append(group_title(COL[0], y, "model — users & events"))
y += 34
box, user_id, h1 = uml_box(COL[0], y, 280, "User",
    attrs=["- userId: String", "- name: String"],
    methods=["+ getUserId(): String", "+ getName(): String"])
cells += box
box, event_id, h2 = uml_box(COL[1], y, 340, "Event",
    attrs=["- eventId: String", "- title: String", "- description: String",
           "- start/end: LocalDateTime", "- ownerId: String", "- attendeeIds: Set<String>",
           "- seriesId: String  // null if one-off"],
    methods=["+ getters…", "+ redacted(): Event", "  // \"Busy\" placeholder, keeps timing only",
              "+ equals()/hashCode()  // by eventId"])
cells += box
box, rtype_id, h3 = uml_box(COL[2], y, 260, "RecurrenceType", stereotype="enumeration",
    attrs=["NONE", "DAILY", "WEEKLY", "MONTHLY"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "composite — calendar aggregation (Composite pattern)  +  model.Calendar (leaf)"))
y += 34
box, component_id, hc0 = uml_box(COL[0], y, 340, "CalendarComponent", stereotype="interface",
    methods=["+ getEvents(rangeStart, rangeEnd): List<Event>", "+ isBusy(instant): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, calendar_id, hc1 = uml_box(COL[1], y, 340, "Calendar", stereotype="leaf",
    attrs=["- ownerId: String", "- eventsById: Map<String,Event>"],
    methods=["+ addEvent(event)/removeEvent(eventId): void", "+ hasConflict(start, end): boolean",
              "+ getEvents(rangeStart, rangeEnd): List<Event>", "+ isBusy(instant): boolean"])
cells += box
box, group_id, hc2 = uml_box(COL[2], y, 340, "CalendarGroup", stereotype="composite",
    attrs=["- groupId: String", "- members: List<CalendarComponent>"],
    methods=["+ addMember(member): void", "+ getEvents(rangeStart, rangeEnd): List<Event>",
              "  // merges + dedupes (by Event.equals) across members", "+ isBusy(instant): boolean  // OR across members"])
cells += box
cells.append(edge(calendar_id, component_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(group_id, component_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.6"))
cells.append(edge(group_id, component_id, "aggregation", "members  0..*", exitX="0.5", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(calendar_id, event_id, "composition", "eventsById  0..*", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
row2_bottom = y + max(hc0, hc1, hc2)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "proxy — privacy-restricted view (Proxy pattern)"))
y += 34
box, proxy_id, hp1 = uml_box(COL[0], y, 360, "RestrictedCalendarProxy", stereotype="protection proxy",
    attrs=["- realCalendar: Calendar", "- viewerId: String"],
    methods=["+ getEvents(rangeStart, rangeEnd): List<Event>", "  // real event if owner/attendee, else event.redacted()",
              "+ isBusy(instant): boolean  // delegates unrestricted", "- canViewDetails(event): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
cells.append(edge(proxy_id, component_id, "realize", exitX="1", exitY="0.2", entryX="0", entryY="0.1"))
cells.append(edge(proxy_id, calendar_id, "association", "realCalendar  1  «wraps»", exitX="1", exitY="0.5", entryX="0", entryY="0.7"))
row3_bottom = y + hp1

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "strategy — recurrence expansion (Strategy pattern)"))
y += 34
box, rstrat_id, hs0 = uml_box(COL[0], y, 340, "RecurrenceStrategy", stereotype="interface",
    methods=["+ generateStartTimes(firstStart, count): List<LocalDateTime>"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, single_id, hs1 = uml_box(COL[1], y, 280, "SingleOccurrenceStrategy",
    methods=["+ generateStartTimes(...)", "  // [firstStart], ignores count"])
cells += box
box, daily_id, hs2 = uml_box(COL[2], y, 280, "DailyRecurrenceStrategy",
    methods=["+ generateStartTimes(...)", "  // firstStart.plusDays(i)"])
cells += box
box, weekly_id, hs3 = uml_box(COL[3], y, 280, "WeeklyRecurrenceStrategy",
    methods=["+ generateStartTimes(...)", "  // firstStart.plusWeeks(i)"])
cells += box
box, monthly_id, hs4 = uml_box(COL[4], y, 280, "MonthlyRecurrenceStrategy",
    methods=["+ generateStartTimes(...)", "  // firstStart.plusMonths(i)"])
cells += box
cells.append(edge(single_id, rstrat_id, "realize", exitX="0.5", exitY="0", entryX="0.15", entryY="1"))
cells.append(edge(daily_id, rstrat_id, "realize", exitX="0.5", exitY="0", entryX="0.4", entryY="1"))
cells.append(edge(weekly_id, rstrat_id, "realize", exitX="0.5", exitY="0", entryX="0.65", entryY="1"))
cells.append(edge(monthly_id, rstrat_id, "realize", exitX="0.5", exitY="0", entryX="0.9", entryY="1"))
row4_bottom = y + max(hs0, hs1, hs2, hs3, hs4)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)"))
y += 34
box, urepo_id, hr1 = uml_box(COL[0], y, 300, "UserRepository",
    attrs=["- usersById: Map<String,User>"],
    methods=["+ save(user): void", "+ findByUserId(id): User"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, crepo_id, hr2 = uml_box(COL[1], y, 300, "CalendarRepository",
    attrs=["- calendarsByOwnerId: Map<String,Calendar>"],
    methods=["+ save(calendar): void", "+ findByOwnerId(id): Calendar", "+ exists(id): boolean"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, erepo_id, hr3 = uml_box(COL[2], y, 300, "EventRepository",
    attrs=["- eventsById: Map<String,Event>"],
    methods=["+ save(event)/remove(eventId): void", "+ findByEventId(id): Event", "+ findBySeriesId(id): List<Event>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, grepo_id, hr4 = uml_box(COL[3], y, 300, "CalendarGroupRepository",
    attrs=["- groupsById: Map<String,CalendarGroup>"],
    methods=["+ save(group): void", "+ exists(id): boolean", "+ findByGroupId(id): CalendarGroup"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(urepo_id, user_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(crepo_id, calendar_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(erepo_id, event_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.75", entryY="1"))
cells.append(edge(grepo_id, group_id, "aggregation", "0..*", exitX="0.5", exitY="0", entryX="0.8", entryY="1"))
row5_bottom = y + max(hr1, hr2, hr3, hr4)

y = row5_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he0 = uml_box(COL[0], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, unf_id, he1 = uml_box(COL[1], y, 280, "UserNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, ecf_id, he2 = uml_box(COL[2], y, 280, "EventConflictException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, enf_id, he3 = uml_box(COL[3], y, 280, "EventNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
row6a_bottom = y + max(he0, he1, he2, he3)
y = row6a_bottom + 20
box, ive_id, he4 = uml_box(COL[1], y, 280, "InvalidEventException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, cgnf_id, he5 = uml_box(COL[2], y, 300, "CalendarGroupNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
for eid in (unf_id, ecf_id, enf_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
for eid in (ive_id, cgnf_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0", entryX="1", entryY="0.6"))
row6_bottom = y + max(he4, he5)

y = row6_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 580, "CalendarService",
    attrs=["- userRepository: UserRepository", "- calendarRepository: CalendarRepository",
           "- eventRepository: EventRepository", "- groupRepository: CalendarGroupRepository",
           "- eventSequence/seriesSequence: AtomicInteger"],
    methods=["+ registerUser(userId, name): void",
              "+ createEvent(owner, title, desc, start, end, attendees, recurrence, count): List<Event>",
              "  // plans+validates the whole series before committing any of it",
              "+ cancelEvent(eventId)/cancelSeries(seriesId): void",
              "+ viewCalendar(viewer, owner, rangeStart, rangeEnd): List<Event>  // through RestrictedCalendarProxy",
              "+ createGroup(groupId)/addGroupMember(groupId, memberId): void",
              "+ getGroupEvents(groupId, rangeStart, rangeEnd): List<Event>",
              "- resolveStrategy(type): RecurrenceStrategy"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, urepo_id, "composition", "1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, crepo_id, "composition", "1", exitX="0.25", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, erepo_id, "composition", "1", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, grepo_id, "composition", "1", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, proxy_id, "dependency", "«creates» per view", exitX="0.7", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, rstrat_id, "dependency", "resolves per createEvent", exitX="0.85", exitY="0", entryX="0.7", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — CREATE RECURRING EVENT (Strategy, plan-then-commit)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: CalendarService", 380), ("strategy: RecurrenceStrategy", 700),
                 ("calRepo: CalendarRepository", 1000), ("calendar: Calendar", 1300), ("eventRepo: EventRepository", 1580)]:
    box, xx = lifeline(x, name, bottom=1000)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: CalendarService"], y, "createEvent(owner, title, start, end, attendees, DAILY, count)"))
y += 44
cells2.append(selfcall(xs["svc: CalendarService"], y, "validate end > start; resolve participants = attendees + owner", loop_w=140, loop_h=22))
y += 60
cells2.append(msg(xs["svc: CalendarService"], xs["strategy: RecurrenceStrategy"], y, "generateStartTimes(start, count)   // DailyRecurrenceStrategy"))
y += 40
cells2.append(msg(xs["strategy: RecurrenceStrategy"], xs["svc: CalendarService"], y, "return occurrenceStarts", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: CalendarService"], y, "requireOccurrencesDontOverlapEachOther(starts, duration)", loop_w=150, loop_h=22))
y += 60
cells2.append(frame(xs["svc: CalendarService"] - 40, xs["calendar: Calendar"] - xs["svc: CalendarService"] + 140, y, 160,
                     "loop  [each occurrence x each participant -- plan only, no mutation yet]"))
y += 34
cells2.append(msg(xs["svc: CalendarService"], xs["calRepo: CalendarRepository"], y, "findByOwnerId(participantId)"))
y += 40
cells2.append(msg(xs["calRepo: CalendarRepository"], xs["calendar: Calendar"], y, "hasConflict(occStart, occEnd)"))
y += 40
cells2.append(msg(xs["calendar: Calendar"], xs["svc: CalendarService"], y, "return true|false", kind="return"))
y += 34
cells2.append(note(xs["calendar: Calendar"] + 30, y, 300, "any conflict throws EventConflictException\nhere -- nothing has been saved or added yet"))
y += 90
cells2.append(selfcall(xs["svc: CalendarService"], y, "build one Event per occurrence (shared seriesId)", loop_w=150, loop_h=22))
y += 60
cells2.append(frame(xs["svc: CalendarService"] - 40, xs["eventRepo: EventRepository"] - xs["svc: CalendarService"] + 140, y, 150,
                     "loop  [each occurrence -- commit]"))
y += 34
cells2.append(msg(xs["svc: CalendarService"], xs["eventRepo: EventRepository"], y, "save(event)"))
y += 40
cells2.append(msg(xs["svc: CalendarService"], xs["calendar: Calendar"], y, "addEvent(event)   // once per participant's calendar"))
y += 60
cells2.append(msg(xs["svc: CalendarService"], xs[":Main"], y, "return occurrences", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — VIEW CALENDAR (Proxy pattern)
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 140), ("svc: CalendarService", 440), ("proxy: RestrictedCalendarProxy", 780),
                 ("calendar: Calendar", 1120), ("event: Event", 1420)]:
    box, xx = lifeline(x, name, bottom=800)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: CalendarService"], y, "viewCalendar(viewerId, ownerId, rangeStart, rangeEnd)"))
y += 44
cells3.append(msg(xs["svc: CalendarService"], xs["proxy: RestrictedCalendarProxy"], y, "«create» new RestrictedCalendarProxy(calendar, viewerId)", kind="create"))
y += 44
cells3.append(msg(xs["svc: CalendarService"], xs["proxy: RestrictedCalendarProxy"], y, "getEvents(rangeStart, rangeEnd)"))
y += 40
cells3.append(msg(xs["proxy: RestrictedCalendarProxy"], xs["calendar: Calendar"], y, "getEvents(rangeStart, rangeEnd)   // unrestricted, real data"))
y += 40
cells3.append(msg(xs["calendar: Calendar"], xs["proxy: RestrictedCalendarProxy"], y, "return events", kind="return"))
y += 44
cells3.append(frame(xs["proxy: RestrictedCalendarProxy"] - 40, xs["event: Event"] - xs["proxy: RestrictedCalendarProxy"] + 140, y, 170,
                     "loop  [for each event in range]"))
y += 34
cells3.append(selfcall(xs["proxy: RestrictedCalendarProxy"], y, "canViewDetails(event) = viewerId == owner || event.attendeeIds.contains(viewerId)", loop_w=180, loop_h=22))
y += 60
cells3.append(frame(xs["proxy: RestrictedCalendarProxy"] - 20, xs["event: Event"] - xs["proxy: RestrictedCalendarProxy"] + 120, y, 90,
                     "alt  [canViewDetails]"))
cells3.append(note(xs["proxy: RestrictedCalendarProxy"] + 20, y + 25, 260, "keep the real Event as-is"))
cells3.append(divider(xs["proxy: RestrictedCalendarProxy"] - 20, xs["event: Event"] - xs["proxy: RestrictedCalendarProxy"] + 120, y + 55, "[else]"))
cells3.append(msg(xs["proxy: RestrictedCalendarProxy"], xs["event: Event"], y + 75, "redacted()   // title=\"Busy\", no description/attendees"))
y += 130
cells3.append(msg(xs["proxy: RestrictedCalendarProxy"], xs["svc: CalendarService"], y, "return visible events", kind="return"))
y += 44
cells3.append(msg(xs["svc: CalendarService"], xs[":Main"], y, "return visible events", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
# PAGE 4: SEQUENCE — NESTED GROUP VIEW (Composite pattern)
# ===========================================================================
cells4 = []
xs = {}
for name, x in [(":Main", 140), ("svc: CalendarService", 440), ("org: CalendarGroup  (\"Org\")", 800),
                 ("team: CalendarGroup  (\"TeamCal\")", 1160), ("alice: Calendar", 1500)]:
    box, xx = lifeline(x, name, bottom=760)
    cells4 += box
    xs[name] = xx

y = 120
cells4.append(msg(xs[":Main"], xs["svc: CalendarService"], y, "getGroupEvents(\"Org\", rangeStart, rangeEnd)"))
y += 44
cells4.append(msg(xs["svc: CalendarService"], xs["org: CalendarGroup  (\"Org\")"], y, "getEvents(rangeStart, rangeEnd)"))
y += 44
cells4.append(frame(xs["org: CalendarGroup  (\"Org\")"] - 40, xs["alice: Calendar"] - xs["org: CalendarGroup  (\"Org\")"] + 140, y, 190,
                     "loop  [for each member: TeamCal (a group), carol (a leaf)]"))
y += 34
cells4.append(msg(xs["org: CalendarGroup  (\"Org\")"], xs["team: CalendarGroup  (\"TeamCal\")"], y, "getEvents(rangeStart, rangeEnd)   // member is itself a CalendarGroup"))
y += 40
cells4.append(frame(xs["team: CalendarGroup  (\"TeamCal\")"] - 30, xs["alice: Calendar"] - xs["team: CalendarGroup  (\"TeamCal\")"] + 110, y, 90,
                     "loop  [TeamCal's own members: alice, bob]"))
y += 30
cells4.append(msg(xs["team: CalendarGroup  (\"TeamCal\")"], xs["alice: Calendar"], y + 20, "getEvents(rangeStart, rangeEnd)"))
y += 90
cells4.append(msg(xs["team: CalendarGroup  (\"TeamCal\")"], xs["org: CalendarGroup  (\"Org\")"], y, "return merged events (TeamCal's own dedupe)", kind="return"))
y += 44
cells4.append(note(xs["org: CalendarGroup  (\"Org\")"] + 40, y, 300, "Org merges TeamCal's result with carol's\nown events into a LinkedHashSet, deduping\nany event shared by two members"))
y += 70
cells4.append(msg(xs["org: CalendarGroup  (\"Org\")"], xs["svc: CalendarService"], y, "return merged + sorted events", kind="return"))
y += 44
cells4.append(msg(xs["svc: CalendarService"], xs[":Main"], y, "return events", kind="return"))

PAGE4 = "\n".join(cells4)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "calendar.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2500),
    page("seqCreateEvent", "2 - Sequence - Create Recurring Event", PAGE2, w=1900, h=1050),
    page("seqView", "3 - Sequence - View Calendar (Proxy)", PAGE3, w=1700, h=850),
    page("seqGroupView", "4 - Sequence - Nested Group View (Composite)", PAGE4, w=1800, h=800),
], outpath)
validate(outpath)
print("wrote", outpath)
