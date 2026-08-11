# -*- coding: utf-8 -*-
"""Regenerates cricinfo.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python cricinfo/diagrams/generate.py
Copied from atm/diagrams/generate.py's structure per CLAUDE.md -- only
supplies data (class fields/methods, edges, sequence messages); all
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
COL = [40, 420, 800, 1180, 1520]
y = 20

cells.append(group_title(COL[0], y, "model — teams, one innings, the match"))
y += 34
box, team_id, h1 = uml_box(COL[0], y, 280, "Team",
    attrs=["- name: String  {final}", "- players: List<String>  {final}"],
    methods=["+ getName()/getPlayers()"])
cells += box
box, innings_id, h2 = uml_box(COL[1], y, 360, "Innings",
    attrs=["- battingTeam: Team  {final}", "- oversLimit: int  {final}", "- target: Integer  {final}",
           "- runs/wickets: int", "- legalBallsThisOver/completedOvers: int",
           "- strikerIndex/nonStrikerIndex/nextBatsmanIndex: int"],
    methods=["+ addRuns(amount)/swapEnds(): void", "+ recordLegalBall(): void  // auto over-completion + end swap",
              "+ recordWicket(): void  // brings in next batsman", "+ isAllOut()/isOversComplete()/hasReachedTarget(): boolean",
              "+ getOversDisplay(): String"])
cells += box
box, match_id, h3 = uml_box(COL[2], y, 300, "Match",
    attrs=["- teamA/teamB: Team  {final}", "- oversLimit: int  {final}",
           "- innings1/innings2: Innings", "- result: String"])
cells += box
box, status_id, h4 = uml_box(COL[3], y, 260, "MatchStatus", stereotype="enumeration",
    attrs=["NOT_STARTED", "INNINGS_1", "INNINGS_BREAK", "INNINGS_2", "COMPLETED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(innings_id, team_id, "aggregation", "battingTeam  1", exitX="0", exitY="0.3", entryX="1", entryY="0.5"))
cells.append(edge(match_id, team_id, "aggregation", "teamA, teamB  2", exitX="0", exitY="0.3", entryX="1", entryY="0.7"))
cells.append(edge(match_id, innings_id, "composition", "innings1, innings2  0..2", exitX="0", exitY="0.6", entryX="1", entryY="0.7"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "command — one delivery's outcome (Command)  +  factory"))
y += 34
box, cmd_id, hc0 = uml_box(COL[0], y, 300, "BallCommand", stereotype="interface",
    methods=["+ execute(innings): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, runs_id, hc1 = uml_box(COL[1], y, 260, "RunsBallCommand",
    attrs=["- runs: int  {final}"],
    methods=["+ execute(...): void", "  // addRuns, swap if odd, recordLegalBall"])
cells += box
box, wkt_id, hc2 = uml_box(COL[2], y, 260, "WicketBallCommand",
    methods=["+ execute(...): void", "  // recordWicket, recordLegalBall"])
cells += box
box, wide_id, hc3 = uml_box(COL[3], y, 260, "WideBallCommand",
    methods=["+ execute(...): void  // +1 run, not legal"])
cells += box
box, nb_id, hc4 = uml_box(COL[4], y, 260, "NoBallBallCommand",
    methods=["+ execute(...): void  // +1 run, not legal"])
cells += box
for cid in (runs_id, wkt_id, wide_id, nb_id):
    cells.append(edge(cid, cmd_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
box, factory_id, hc5 = uml_box(COL[0], y + max(hc0,hc1,hc2,hc3,hc4) + 20, 340, "BallCommandFactory",
    methods=["+ create(type, runs): BallCommand  {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(factory_id, cmd_id, "dependency", "creates", exitX="0.5", exitY="0", entryX="0.1", entryY="1"))
row2_bottom = y + max(hc0,hc1,hc2,hc3,hc4) + 20 + hc5

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "state — match-phase lifecycle (State), held once on the service"))
y += 34
box, mstate_id, hs0 = uml_box(COL[0], y, 340, "MatchState", stereotype="interface",
    methods=["+ getStatus(): MatchStatus", "+ startFirstInnings(): MatchState",
              "+ requireInningsInProgress(): void", "+ startSecondInnings(): MatchState",
              "+ endInnings(): MatchState",
              "  // defaults throw; each state overrides only its legal moves"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, ns_id, hs1 = uml_box(COL[1], y, 250, "NotStartedState", stereotype="singleton",
    methods=["+ startFirstInnings(): Innings1State"])
cells += box
box, i1_id, hs2 = uml_box(COL[2], y, 250, "Innings1State", stereotype="singleton",
    methods=["+ requireInningsInProgress(): void  // no-op", "+ endInnings(): InningsBreakState"])
cells += box
box, ib_id, hs3 = uml_box(COL[3], y, 250, "InningsBreakState", stereotype="singleton",
    methods=["+ startSecondInnings(): Innings2State"])
cells += box
box, i2_id, hs4 = uml_box(COL[4], y, 250, "Innings2State", stereotype="singleton",
    methods=["+ requireInningsInProgress(): void  // no-op", "+ endInnings(): CompletedState"])
cells += box
box, comp_id, hs5 = uml_box(COL[2], y + max(hs0,hs1,hs2,hs3,hs4) + 20, 250, "CompletedState", stereotype="singleton",
    methods=["  // terminal: every transition throws"])
cells += box
for sid in (ns_id, i1_id, ib_id, i2_id, comp_id):
    cells.append(edge(sid, mstate_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(ns_id, i1_id, "dependency", "startFirstInnings() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(i1_id, ib_id, "dependency", "endInnings() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(ib_id, i2_id, "dependency", "startSecondInnings() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(i2_id, comp_id, "dependency", "endInnings() →", exitX="0.3", exitY="1", entryX="0.7", entryY="0"))
row3_bottom = y + max(hs0,hs1,hs2,hs3,hs4) + 20 + hs5

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "observer — wicket / innings / match events (Observer)  +  exceptions"))
y += 34
box, listener_id, hl0 = uml_box(COL[0], y, 340, "MatchListener", stereotype="interface",
    methods=["+ onWicketFallen(team, wickets, runs): void", "+ onInningsComplete(team, runs, wickets, overs): void",
              "+ onMatchComplete(resultSummary): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, hl1 = uml_box(COL[1], y, 300, "ConsoleMatchListener",
    methods=["+ onWicketFallen/onInningsComplete/onMatchComplete: void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))

box, rte_id, he0 = uml_box(COL[2], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, mnp_id, he1 = uml_box(COL[3], y, 300, "MatchNotInProgressException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, imo_id, he2 = uml_box(COL[4], y, 300, "IllegalMatchOperationException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(mnp_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(imo_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row4_bottom = y + max(hl0, hl1, he0, he1, he2)

y = row4_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 620, "CricInfoService",
    attrs=["- teamA/teamB: Team", "- match: Match", "- state: MatchState", "- listeners: List<MatchListener>"],
    methods=["+ setTeams(nameA, playersA, nameB, playersB): void", "+ startMatch(oversLimit): void",
              "+ recordBall(type, runs): void", "+ startSecondInnings(): void",
              "+ getScorecard(): String", "+ addListener(listener): void",
              "- computeAndNotifyResult(): void"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, match_id, "composition", "match  1", exitX="0.15", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, mstate_id, "association", "state  1", exitX="0.35", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.55", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.8", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — RECORD A WICKET BALL (Command dispatch via Factory)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: CricInfoService", 380), ("state: Innings1State", 700),
                 ("factory: BallCommandFactory", 1000), ("cmd: WicketBallCommand", 1300), ("innings: Innings", 1600)]:
    box, xx = lifeline(x, name, bottom=820)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: CricInfoService"], y, "recordBall(\"WICKET\", 0)"))
y += 40
cells2.append(msg(xs["svc: CricInfoService"], xs["state: Innings1State"], y, "state.requireInningsInProgress()   // no-op, legal"))
y += 44
cells2.append(msg(xs["svc: CricInfoService"], xs["factory: BallCommandFactory"], y, "create(\"WICKET\", 0)"))
y += 40
cells2.append(msg(xs["factory: BallCommandFactory"], xs["cmd: WicketBallCommand"], y, "«create»", kind="create"))
y += 40
cells2.append(msg(xs["factory: BallCommandFactory"], xs["svc: CricInfoService"], y, "return command", kind="return"))
y += 44
cells2.append(msg(xs["svc: CricInfoService"], xs["cmd: WicketBallCommand"], y, "command.execute(innings)"))
y += 40
cells2.append(msg(xs["cmd: WicketBallCommand"], xs["innings: Innings"], y, "recordWicket()   // wickets++, next batsman on strike"))
y += 44
cells2.append(msg(xs["cmd: WicketBallCommand"], xs["innings: Innings"], y, "recordLegalBall()   // legalBalls++"))
y += 50
cells2.append(msg(xs["cmd: WicketBallCommand"], xs["svc: CricInfoService"], y, "return", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: CricInfoService"], y, "command instanceof WicketBallCommand -> notifyWicketFallen(innings)", loop_w=170, loop_h=22))
y += 60
cells2.append(selfcall(xs["svc: CricInfoService"], y, "innings.isAllOut() / isOversComplete() / hasReachedTarget() -> all false here", loop_w=190, loop_h=22))
y += 60
cells2.append(msg(xs["svc: CricInfoService"], xs[":Main"], y, "return", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — LAST BALL OF INNINGS 1 (auto end-innings + state transition)
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: CricInfoService", 380), ("innings: Innings", 700),
                 ("state: Innings1State", 1000), ("listeners: MatchListener", 1300)]:
    box, xx = lifeline(x, name, bottom=760)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(note(xs[":Main"] - 60, y, 320, "2nd over's 6th legal ball -- India's innings is due to end"))
y += 70
cells3.append(msg(xs[":Main"], xs["svc: CricInfoService"], y, "recordBall(\"RUN\", 0)"))
y += 40
cells3.append(msg(xs["svc: CricInfoService"], xs["innings: Innings"], y, "command.execute(innings)   // addRuns(0), recordLegalBall()"))
y += 44
cells3.append(selfcall(xs["innings: Innings"], y, "legalBallsThisOver hits 6 -> completedOvers++, legalBallsThisOver=0, swapEnds()", loop_w=200, loop_h=22))
y += 70
cells3.append(msg(xs["svc: CricInfoService"], xs["innings: Innings"], y, "isOversComplete()"))
y += 40
cells3.append(msg(xs["innings: Innings"], xs["svc: CricInfoService"], y, "return true   // completedOvers >= oversLimit", kind="return"))
y += 50
cells3.append(msg(xs["svc: CricInfoService"], xs["listeners: MatchListener"], y, "notifyInningsComplete(innings)   // \"India 23/2 (2.0 overs)\""))
y += 44
cells3.append(msg(xs["svc: CricInfoService"], xs["state: Innings1State"], y, "state = state.endInnings()"))
y += 40
cells3.append(msg(xs["state: Innings1State"], xs["svc: CricInfoService"], y, "return InningsBreakState.INSTANCE", kind="return"))
y += 44
cells3.append(selfcall(xs["svc: CricInfoService"], y, "state.getStatus() == COMPLETED?  no -> skip computeAndNotifyResult()", loop_w=190, loop_h=22))
y += 60
cells3.append(msg(xs["svc: CricInfoService"], xs[":Main"], y, "return", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "cricinfo.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=2100),
    page("seqWicket", "2 - Sequence - Record a Wicket Ball", PAGE2, w=1900, h=880),
    page("seqEndInnings", "3 - Sequence - Auto End-of-Innings", PAGE3, w=1700, h=820),
], outpath)
validate(outpath)
print("wrote", outpath)
