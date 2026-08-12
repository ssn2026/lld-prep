# -*- coding: utf-8 -*-
"""Regenerates snake-and-ladder.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python snake-and-ladder/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model  +  builder — the Board (Builder)"))
y += 34
box, board_id, h1 = uml_box(COL[0], y, 320, "Board",
    attrs=["- size: int  {final}", "- jumps: Map<Integer,Integer>  {final}"],
    methods=["+ getSize()/getJumps()", "  // no public constructor"])
cells += box
box, builder_id, h2 = uml_box(COL[1], y, 340, "BoardBuilder",
    attrs=["- size: int  {final}", "- jumps: Map<Integer,Integer>"],
    methods=["+ addSnake(head, tail): BoardBuilder", "+ addLadder(bottom, top): BoardBuilder",
              "+ build(): Board", "  // throws InvalidBoardConfigException"])
cells += box
box, player_id, h3 = uml_box(COL[2], y, 260, "Player",
    attrs=["- name: String  {final}", "- position: int"],
    methods=["+ getName()/getPosition()/setPosition(p)"])
cells += box
box, reason_id, h4 = uml_box(COL[3], y, 260, "MoveReason", stereotype="enumeration",
    attrs=["DICE_ROLL, SNAKE, LADDER"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(builder_id, board_id, "dependency", "builds", exitX="0.5", exitY="0", entryX="0.5", entryY="0"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — how a roll's value is decided (Strategy)"))
y += 34
box, dice_id, ht0 = uml_box(COL[0], y, 280, "DiceStrategy", stereotype="interface",
    methods=["+ roll(): int"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, rdice_id, ht1 = uml_box(COL[1], y, 300, "RandomDiceStrategy",
    attrs=["- random: Random  {final}"],
    methods=["+ RandomDiceStrategy(seed)", "+ roll(): int  // 1-6"])
cells += box
cells.append(edge(rdice_id, dice_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row2_bottom = y + max(ht0, ht1)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "observer — move / snake / ladder / win events (Observer)  +  exceptions"))
y += 34
box, listener_id, ho0 = uml_box(COL[0], y, 320, "GameListener", stereotype="interface",
    methods=["+ onPositionChanged(name, from, to, reason): void", "+ onGameWon(name): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, ho1 = uml_box(COL[1], y, 300, "ConsoleGameListener",
    methods=["+ onPositionChanged(...)/onGameWon(...): void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))

box, rte_id, he0 = uml_box(COL[2], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, ibc_id, he1 = uml_box(COL[3], y, 280, "InvalidBoardConfigException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, gaw_id, he2 = uml_box(COL[3], y + he1 + 16, 280, "GameAlreadyWonException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(ibc_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(gaw_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row3_bottom = y + max(ho0, ho1, he0, he1 + 16 + he2)

y = row3_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 580, "SnakeAndLadderService",
    attrs=["- board: Board  {final}", "- players: List<Player>  {final}",
           "- diceStrategy: DiceStrategy  {final}", "- listeners: List<GameListener>",
           "- currentPlayerIndex: int", "- winner: String"],
    methods=["+ rollAndMove(): int", "+ getPositions(): Map<String,Integer>",
              "+ getWinner()/getCurrentPlayerName(): String", "+ addListener(listener): void",
              "- advanceTurn(): void"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, board_id, "composition", "board  1", exitX="0.15", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, player_id, "composition", "players  1..*", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, dice_id, "dependency", "uses", exitX="0.6", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.8", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — rollAndMove() landing on a snake
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: SnakeAndLadderService", 380), ("dice: RandomDiceStrategy", 700),
                 ("board: Board", 1000), ("listeners: GameListener", 1300)]:
    box, xx = lifeline(x, name, bottom=820)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: SnakeAndLadderService"], y, "rollAndMove()   // Alice at 12"))
y += 40
cells2.append(msg(xs["svc: SnakeAndLadderService"], xs["dice: RandomDiceStrategy"], y, "roll()"))
y += 40
cells2.append(msg(xs["dice: RandomDiceStrategy"], xs["svc: SnakeAndLadderService"], y, "return 5", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: SnakeAndLadderService"], y, "tentative = 12 + 5 = 17   // <= board.getSize(), legal move", loop_w=190, loop_h=22))
y += 60
cells2.append(msg(xs["svc: SnakeAndLadderService"], xs["listeners: GameListener"], y, "onPositionChanged(\"Alice\", 12, 17, DICE_ROLL)"))
y += 50
cells2.append(msg(xs["svc: SnakeAndLadderService"], xs["board: Board"], y, "board.getJumps().get(17)"))
y += 40
cells2.append(msg(xs["board: Board"], xs["svc: SnakeAndLadderService"], y, "return 4   // a snake head lives at 17", kind="return"))
y += 50
cells2.append(selfcall(xs["svc: SnakeAndLadderService"], y, "reason = 4 < 17 -> SNAKE", loop_w=140, loop_h=20))
y += 50
cells2.append(msg(xs["svc: SnakeAndLadderService"], xs["listeners: GameListener"], y, "onPositionChanged(\"Alice\", 17, 4, SNAKE)"))
y += 50
cells2.append(frame(xs["svc: SnakeAndLadderService"] - 60, xs["listeners: GameListener"] - xs["svc: SnakeAndLadderService"] + 160, y, 70,
                     "alt  [current.getPosition() == board.getSize()]"))
cells2.append(selfcall(xs["svc: SnakeAndLadderService"], y + 24, "winner set, onGameWon() fired", loop_w=140, loop_h=20))
cells2.append(divider(xs["svc: SnakeAndLadderService"] - 60, xs["listeners: GameListener"] - xs["svc: SnakeAndLadderService"] + 160, y + 46, "[else: position 4, game continues]"))
y += 90
cells2.append(selfcall(xs["svc: SnakeAndLadderService"], y, "advanceTurn()   // currentPlayerIndex -> Bob", loop_w=150, loop_h=20))
y += 50
cells2.append(msg(xs["svc: SnakeAndLadderService"], xs[":Main"], y, "return 5   // the roll value", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "snake-and-ladder.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1950, h=1500),
    page("seqSnake", "2 - Sequence - rollAndMove() Landing on a Snake", PAGE2, w=1900, h=880),
], outpath)
validate(outpath)
print("wrote", outpath)
