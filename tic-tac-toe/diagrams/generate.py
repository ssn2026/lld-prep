# -*- coding: utf-8 -*-
"""Regenerates tic-tac-toe.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python tic-tac-toe/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — the grid"))
y += 34
box, board_id, h1 = uml_box(COL[0], y, 300, "Board",
    attrs=["- size: int  {final}", "- grid: Mark[][]  {final}", "- filledCount: int"],
    methods=["+ get(row, col): Mark", "+ isInBounds(row, col)/isEmpty(row, col): boolean",
              "+ place(row, col, mark): void", "+ isFull(): boolean", "+ render(): String"])
cells += box
box, mark_id, h2 = uml_box(COL[1], y, 240, "Mark", stereotype="enumeration",
    attrs=["X, O, EMPTY"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, status_id, h3 = uml_box(COL[2], y, 260, "GameStatus", stereotype="enumeration",
    attrs=["IN_PROGRESS, X_WON, O_WON, DRAW"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(board_id, mark_id, "composition", "grid  size×size", exitX="0.3", exitY="0", entryX="0.3", entryY="1"))
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — what counts as a win (Strategy)"))
y += 34
box, win_id, ht0 = uml_box(COL[0], y, 320, "WinningStrategy", stereotype="interface",
    methods=["+ checkWinner(board, row, col, mark): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, line_id, ht1 = uml_box(COL[1], y, 340, "LineWinningStrategy",
    methods=["+ checkWinner(...): boolean", "  // only the row/col/diagonals through (row,col) -- O(size)"])
cells += box
cells.append(edge(line_id, win_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row2_bottom = y + max(ht0, ht1)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "state — game-phase lifecycle (State), held once on the service"))
y += 34
box, gstate_id, hs0 = uml_box(COL[0], y, 300, "GameState", stereotype="interface",
    methods=["+ getStatus(): GameStatus", "+ requireInProgress(): void",
              "  // default throws GameOverException"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, prog_id, hs1 = uml_box(COL[1], y, 230, "InProgressState", stereotype="singleton",
    methods=["+ requireInProgress(): void  // no-op"])
cells += box
box, xwon_id, hs2 = uml_box(COL[2], y, 200, "XWonState", stereotype="singleton", methods=["  // terminal"])
cells += box
box, owon_id, hs3 = uml_box(COL[3], y, 200, "OWonState", stereotype="singleton", methods=["  // terminal"])
cells += box
box, draw_id, hs4 = uml_box(COL[4], y, 200, "DrawState", stereotype="singleton", methods=["  // terminal"])
cells += box
for sid in (prog_id, xwon_id, owon_id, draw_id):
    cells.append(edge(sid, gstate_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + max(hs0, hs1, hs2, hs3, hs4)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "observer — move / game-over events (Observer)  +  exceptions"))
y += 34
box, listener_id, ho0 = uml_box(COL[0], y, 300, "GameListener", stereotype="interface",
    methods=["+ onMove(mark, row, col): void", "+ onGameOver(result): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, ho1 = uml_box(COL[1], y, 280, "ConsoleGameListener",
    methods=["+ onMove(...)/onGameOver(...): void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))

box, rte_id, he0 = uml_box(COL[2], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, gov_id, he1 = uml_box(COL[3], y, 260, "GameOverException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, imv_id, he2 = uml_box(COL[3], y + he1 + 16, 260, "InvalidMoveException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(gov_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(imv_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row4_bottom = y + max(ho0, ho1, he0, he1 + 16 + he2)

y = row4_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 580, "TicTacToeService",
    attrs=["- board: Board  {final}", "- winningStrategy: WinningStrategy  {final}",
           "- listeners: List<GameListener>", "- state: GameState", "- currentMark: Mark"],
    methods=["+ makeMove(row, col): void", "+ getStatus(): GameStatus", "+ renderBoard(): String",
              "+ addListener(listener): void"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, board_id, "composition", "board  1", exitX="0.15", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, win_id, "dependency", "uses", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, gstate_id, "association", "state  1", exitX="0.6", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.85", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — makeMove() completing a winning line
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: TicTacToeService", 380), ("board: Board", 700),
                 ("win: LineWinningStrategy", 1000), ("state: InProgressState", 1300), ("listeners: GameListener", 1600)]:
    box, xx = lifeline(x, name, bottom=880)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: TicTacToeService"], y, "makeMove(0, 2)   // X's 3rd move, top row already X X _"))
y += 40
cells2.append(msg(xs["svc: TicTacToeService"], xs["state: InProgressState"], y, "state.requireInProgress()   // no-op, legal"))
y += 44
cells2.append(msg(xs["svc: TicTacToeService"], xs["board: Board"], y, "isInBounds(0,2) && isEmpty(0,2)"))
y += 40
cells2.append(msg(xs["board: Board"], xs["svc: TicTacToeService"], y, "return true, true", kind="return"))
y += 44
cells2.append(msg(xs["svc: TicTacToeService"], xs["board: Board"], y, "place(0, 2, X)"))
y += 44
cells2.append(msg(xs["svc: TicTacToeService"], xs["listeners: GameListener"], y, "notifyMove(X, 0, 2)"))
y += 50
cells2.append(msg(xs["svc: TicTacToeService"], xs["win: LineWinningStrategy"], y, "checkWinner(board, 0, 2, X)"))
y += 40
cells2.append(selfcall(xs["win: LineWinningStrategy"], y, "row 0: (0,0)=X, (0,1)=X, (0,2)=X -> rowWin=true", loop_w=180, loop_h=22))
y += 60
cells2.append(msg(xs["win: LineWinningStrategy"], xs["svc: TicTacToeService"], y, "return true", kind="return"))
y += 50
cells2.append(selfcall(xs["svc: TicTacToeService"], y, "state = XWonState.INSTANCE", loop_w=150, loop_h=20))
y += 50
cells2.append(msg(xs["svc: TicTacToeService"], xs["listeners: GameListener"], y, "notifyGameOver(X_WON)"))
y += 50
cells2.append(msg(xs["svc: TicTacToeService"], xs[":Main"], y, "return", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "tic-tac-toe.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1950, h=1500),
    page("seqWin", "2 - Sequence - makeMove() Completing a Win", PAGE2, w=1900, h=940),
], outpath)
validate(outpath)
print("wrote", outpath)
