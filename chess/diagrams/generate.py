# -*- coding: utf-8 -*-
"""Regenerates chess.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python chess/diagrams/generate.py
Copied from movie-booking/diagrams/generate.py's structure per CLAUDE.md -
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
COL = [40, 400, 760, 1120, 1480, 1840]
y = 20

cells.append(group_title(COL[0], y, "model — pieces (movement geometry) & board"))
y += 34
box, piece_id, h1 = uml_box(COL[0], y, 300, "Piece", stereotype="abstract",
    attrs=["# color: Color", "# position: Position", "# hasMoved: boolean"],
    methods=["+ getType(): PieceType {abstract}", "+ getPossibleMoves(board): List<Position> {abstract}",
             "+ copy(): Piece {abstract}"],
    header_fill="#ffe6cc", header_stroke="#d79b00")
cells += box
box, pawn_id, h2 = uml_box(COL[1], y, 260, "Pawn",
    methods=["+ getPossibleMoves(board): List<Position>", "  // 1/2-forward, diagonal capture"])
cells += box
box, king_id, h3 = uml_box(COL[2], y, 260, "King",
    methods=["+ getPossibleMoves(board): List<Position>", "  // 8 adjacent, no castling"])
cells += box
box, others_id, h4 = uml_box(COL[3], y, 300, "Knight / Bishop / Rook / Queen", stereotype="+4 more",
    methods=["+ getPossibleMoves(board): List<Position>", "  // knight L-shape; sliding for B/R/Q",
             "  // (SlidingPieceSupport shared ray-cast helper)"])
cells += box
cells.append(edge(pawn_id, piece_id, "inherit", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(king_id, piece_id, "inherit", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(others_id, piece_id, "inherit", exitX="0.2", exitY="0", entryX="0.8", entryY="1"))
box, board_id, h5 = uml_box(COL[4], y, 320, "Board",
    attrs=["- grid: Piece[8][8]"],
    methods=["+ getPiece(pos): Piece", "+ placePiece(piece, pos) / removePiece(pos): void",
             "+ isSquareAttacked(pos, byColor): boolean", "+ isMoveLegal(piece, dest): boolean",
             "+ hasAnyLegalMove(color): boolean", "+ findKing(color): Position",
             "+ copy(): Board", "+ render(): String"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(board_id, piece_id, "aggregation", "grid  0..32", exitX="0.1", exitY="0.5", entryX="1", entryY="0.5"))
row1_bottom = y + max(h1, h2, h3, h4, h5)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "model — positions, players, moves, enums"))
y += 34
box, pos_id, hp1 = uml_box(COL[0], y, 260, "Position",
    attrs=["- row: int", "- col: int"],
    methods=["+ of(algebraic): Position {static}", "+ toAlgebraic(): String", "+ isOnBoard(): boolean"])
cells += box
box, player_id, hp2 = uml_box(COL[1], y, 240, "Player",
    attrs=["- playerId/name: String", "- color: Color"])
cells += box
box, move_id, hp3 = uml_box(COL[2], y, 320, "Move",
    attrs=["- from/to: Position", "- movedPieceType: PieceType", "- movedBy: Color",
           "- capturedPieceType: PieceType", "- promotedTo: PieceType"],
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, color_id, hp4 = uml_box(COL[3], y, 200, "Color", stereotype="enumeration",
    attrs=["WHITE", "BLACK"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, ptype_id, hp5 = uml_box(COL[3], y + 110, 200, "PieceType", stereotype="enumeration",
    attrs=["PAWN, KNIGHT, BISHOP,", "ROOK, QUEEN, KING"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, gstatus_id, hp6 = uml_box(COL[4], y, 240, "GameStatus", stereotype="enumeration",
    attrs=["IN_PROGRESS", "CHECK", "CHECKMATE", "STALEMATE"], header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(piece_id, pos_id, "association", "position  1", exitX="0.05", exitY="0.5", entryX="0.5", entryY="0"))
cells.append(edge(piece_id, color_id, "association", "color  1", exitX="0.95", exitY="0.5", entryX="0.5", entryY="0"))
cells.append(edge(move_id, pos_id, "association", "from/to", exitX="0.2", exitY="0", entryX="0.7", entryY="1"))
row2_bottom = y + max(hp1, hp2, hp3, hp4 + 110 + hp5, hp6)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "factory — piece creation"))
y += 34
box, factory_id, hf1 = uml_box(COL[0], y, 340, "PieceFactory", stereotype="static factory",
    methods=["+ createPiece(type, color, pos): Piece {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(factory_id, piece_id, "dependency", "«creates»", exitX="0.7", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + hf1

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "state — GameStatus gates whether makeMove() is accepted"))
y += 34
box, gstate_id, hg1 = uml_box(COL[0], y, 320, "GameState", stereotype="interface",
    methods=["+ getStatus(): GameStatus", "+ allowsMove(): boolean",
             "+ evaluate(board, colorToMove): GameState {static}"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, inprog_id, hg2 = uml_box(COL[1], y, 240, "InProgressState",
    methods=["allowsMove() = true"])
cells += box
box, check_id, hg3 = uml_box(COL[2], y, 240, "CheckState",
    methods=["allowsMove() = true"])
cells += box
box, mate_id, hg4 = uml_box(COL[3], y, 240, "CheckmateState",
    methods=["allowsMove() = false"])
cells += box
box, stale_id, hg5 = uml_box(COL[4], y, 240, "StalemateState",
    methods=["allowsMove() = false"])
cells += box
cells.append(edge(inprog_id, gstate_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.2"))
cells.append(edge(check_id, gstate_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.4"))
cells.append(edge(mate_id, gstate_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.6"))
cells.append(edge(stale_id, gstate_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.8"))
row4_bottom = y + max(hg1, hg2, hg3, hg4, hg5)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "observer — move/check/game-over notifications"))
y += 34
box, gobs_id, ho1 = uml_box(COL[0], y, 300, "GameObserver", stereotype="interface",
    methods=["+ onMove(move): void", "+ onCheck(colorInCheck): void",
             "+ onGameOver(finalStatus, winner): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, cobs_id, ho2 = uml_box(COL[1], y, 300, "ConsoleGameObserver",
    methods=["+ onMove(move): void", "+ onCheck(colorInCheck): void",
             "+ onGameOver(finalStatus, winner): void"])
cells += box
cells.append(edge(cobs_id, gobs_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
row5_bottom = y + max(ho1, ho2)

y = row5_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he1 = uml_box(COL[0], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, invmove_id, he2 = uml_box(COL[1], y, 280, "InvalidMoveException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, gameover_id, he3 = uml_box(COL[2], y, 280, "GameOverException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(invmove_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(gameover_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row6_bottom = y + max(he1, he2, he3)

y = row6_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "ChessGameService",
    attrs=["- board: Board", "- whitePlayer/blackPlayer: Player", "- moveHistory: List<Move>",
           "- observers: List<GameObserver>", "- currentTurn: Color", "- currentState: GameState"],
    methods=["+ addObserver(observer): void",
             "+ makeMove(fromAlg, toAlg, promotionChoice): Move",
             "+ renderBoard(): String", "+ getStatus(): GameStatus", "+ getCurrentTurn(): Color",
             "+ getMoveHistory(): List<Move>",
             "- applyPromotionIfNeeded(piece, to, choice): PieceType", "- setupBoard(): void"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, board_id, "composition", "board  1", exitX="0.1", exitY="0", entryX="0.4", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.25", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, gstate_id, "association", "currentState  1", exitX="0.45", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, gobs_id, "aggregation", "observers  0..*", exitX="0.6", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(svc_id, player_id, "association", "white/black  2", exitX="0.75", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, move_id, "dependency", "records", exitX="0.9", exitY="0", entryX="0.6", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — MAKE MOVE (happy path, in progress)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: ChessGameService", 400), ("board: Board", 720),
                 ("piece: Piece", 1020), ("state: GameState", 1300), ("observer: GameObserver", 1580)]:
    box, xx = lifeline(x, name, bottom=1020)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: ChessGameService"], y, "makeMove(fromAlg, toAlg, promotionChoice)"))
y += 40
cells2.append(selfcall(xs["svc: ChessGameService"], y, "currentState.allowsMove()", loop_w=90, loop_h=22))
y += 50
cells2.append(msg(xs["svc: ChessGameService"], xs["board: Board"], y, "getPiece(from)"))
y += 40
cells2.append(msg(xs["board: Board"], xs["svc: ChessGameService"], y, "return piece", kind="return"))
y += 40
cells2.append(msg(xs["svc: ChessGameService"], xs["piece: Piece"], y, "getPossibleMoves(board)"))
y += 40
cells2.append(msg(xs["piece: Piece"], xs["svc: ChessGameService"], y, "return possibleMoves", kind="return"))
y += 40
cells2.append(msg(xs["svc: ChessGameService"], xs["board: Board"], y, "isMoveLegal(piece, to)"))
y += 30
cells2.append(selfcall(xs["board: Board"], y, "copy() + simulate + isSquareAttacked(ownKing)", loop_w=110, loop_h=22))
y += 60
cells2.append(msg(xs["board: Board"], xs["svc: ChessGameService"], y, "return true", kind="return"))
y += 50
cells2.append(frame(xs["svc: ChessGameService"] - 40, xs["board: Board"] - xs["svc: ChessGameService"] + 160, y, 40,
                     "alt  [captured != null] -> board.removePiece(to)"))
y += 66
cells2.append(selfcall(xs["svc: ChessGameService"], y, "board.removePiece(from); piece.setPosition(to); board.placePiece(piece, to)", loop_w=140, loop_h=22))
y += 60
cells2.append(msg(xs["svc: ChessGameService"], xs["board: Board"], y, "«create» new Move(from, to, ...)", kind="create"))
y += 50
cells2.append(msg(xs["svc: ChessGameService"], xs["state: GameState"], y, "evaluate(board, opponentColor)"))
y += 30
cells2.append(selfcall(xs["state: GameState"], y, "isSquareAttacked(king) + hasAnyLegalMove(color)", loop_w=120, loop_h=22))
y += 60
cells2.append(msg(xs["state: GameState"], xs["svc: ChessGameService"], y, "return InProgressState.INSTANCE", kind="return"))
y += 50
cells2.append(frame(xs["svc: ChessGameService"] - 60, xs["observer: GameObserver"] - xs["svc: ChessGameService"] + 160, y, 90,
                     "loop  [for each registered observer]"))
y += 34
cells2.append(msg(xs["svc: ChessGameService"], xs["observer: GameObserver"], y, "onMove(move)"))
y += 60
cells2.append(msg(xs["svc: ChessGameService"], xs[":Main"], y, "return move", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — MAKE MOVE THAT DELIVERS CHECKMATE
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: ChessGameService", 420), ("board: Board", 760),
                 ("state: GameState", 1080), ("observer: GameObserver", 1380)]:
    box, xx = lifeline(x, name, bottom=760)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: ChessGameService"], y, "makeMove(\"d8\", \"h4\", null)"))
y += 50
cells3.append(selfcall(xs["svc: ChessGameService"], y, "validate + apply move on board (Queen d8->h4)", loop_w=150, loop_h=22))
y += 60
cells3.append(msg(xs["svc: ChessGameService"], xs["state: GameState"], y, "evaluate(board, WHITE)"))
y += 30
cells3.append(msg(xs["state: GameState"], xs["board: Board"], y, "isSquareAttacked(whiteKingPos, BLACK)"))
y += 40
cells3.append(msg(xs["board: Board"], xs["state: GameState"], y, "return true", kind="return"))
y += 40
cells3.append(msg(xs["state: GameState"], xs["board: Board"], y, "hasAnyLegalMove(WHITE)"))
y += 40
cells3.append(msg(xs["board: Board"], xs["state: GameState"], y, "return false  // king boxed in, nothing blocks/captures", kind="return"))
y += 40
cells3.append(msg(xs["state: GameState"], xs["svc: ChessGameService"], y, "return CheckmateState.INSTANCE", kind="return"))
y += 50
cells3.append(frame(xs["svc: ChessGameService"] - 60, xs["observer: GameObserver"] - xs["svc: ChessGameService"] + 160, y, 90,
                     "loop  [for each registered observer]"))
y += 34
cells3.append(msg(xs["svc: ChessGameService"], xs["observer: GameObserver"], y, "onGameOver(CHECKMATE, winner=BLACK)"))
y += 60
cells3.append(msg(xs["svc: ChessGameService"], xs[":Main"], y, "return move", kind="return"))
y += 50
cells3.append(note(xs[":Main"] - 60, y, 700,
    "Any further Main -> svc.makeMove(...) call now throws GameOverException,\n"
    "since currentState.allowsMove() == false for CheckmateState."))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "chess.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2200, h=2300),
    page("seqMove", "2 - Sequence - Make Move", PAGE2, w=1900, h=1100),
    page("seqCheckmate", "3 - Sequence - Checkmate", PAGE3, w=1700, h=850),
], outpath)
validate(outpath)
print("wrote", outpath)
