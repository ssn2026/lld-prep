# -*- coding: utf-8 -*-
"""Regenerates blackjack.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python blackjack/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — cards, a Hand (owns its own State), a Player"))
y += 34
box, card_id, h1 = uml_box(COL[0], y, 260, "Card",
    attrs=["- suit: Suit  {final}", "- rank: Rank  {final}"])
cells += box
box, rank_id, h2 = uml_box(COL[1], y, 260, "Rank", stereotype="enumeration",
    attrs=["TWO..TEN, JACK, QUEEN, KING (10)", "ACE (11)"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, suit_id, h3 = uml_box(COL[2], y, 240, "Suit", stereotype="enumeration",
    attrs=["HEARTS, DIAMONDS, CLUBS, SPADES"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, hand_id, h4 = uml_box(COL[3], y, 340, "Hand",
    attrs=["- cards: List<Card>", "- state: HandState"],
    methods=["+ addCard(card): void", "+ settleInitialState(): void  // Blackjack if total==21 else Active",
              "+ getTotal(): int  // soft-ace algorithm"])
cells += box
row1_bottom = y + max(h1, h2, h3, h4)
cells.append(edge(card_id, rank_id, "association", "rank", exitX="0.3", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(card_id, suit_id, "association", "suit", exitX="0.7", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(hand_id, card_id, "composition", "cards  0..*", exitX="0", exitY="0.3", entryX="1", entryY="0.5"))

y = row1_bottom + 70
box, player_id, h5 = uml_box(COL[0], y, 280, "Player",
    attrs=["- name: String  {final}", "- hand: Hand  {final}"])
cells += box
box, deck_id, h6 = uml_box(COL[1], y, 280, "Deck",
    attrs=["- cards: Deque<Card>  {final}"],
    methods=["+ draw(): Card  // throws EmptyDeckException", "+ remaining(): int"])
cells += box
cells.append(edge(player_id, hand_id, "composition", "hand  1", exitX="1", exitY="0.3", entryX="0", entryY="1"))
row1b_bottom = y + max(h5, h6)

y = row1b_bottom + 70
cells.append(group_title(COL[0], y, "state — per-Hand lifecycle (State), held on the Hand itself"))
y += 34
box, hstate_id, hs0 = uml_box(COL[0], y, 320, "HandState", stereotype="interface",
    methods=["+ getStatus(): HandStatus", "+ requireActive(): void",
              "+ hit(hand, newCard): HandState", "+ stand(hand): HandState",
              "  // defaults throw; only ActiveState overrides the real moves"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, active_id, hs1 = uml_box(COL[1], y, 250, "ActiveState", stereotype="singleton",
    methods=["+ hit(hand, card): HandState", "  // adds card; Busted if total>21, else Active",
              "+ stand(hand): StandingState"])
cells += box
box, standing_id, hs2 = uml_box(COL[2], y, 220, "StandingState", stereotype="singleton",
    methods=["  // terminal"])
cells += box
box, busted_id, hs3 = uml_box(COL[3], y, 220, "BustedState", stereotype="singleton",
    methods=["  // terminal"])
cells += box
box, bj_id, hs4 = uml_box(COL[4], y, 220, "BlackjackState", stereotype="singleton",
    methods=["  // terminal"])
cells += box
for sid in (active_id, standing_id, busted_id, bj_id):
    cells.append(edge(sid, hstate_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(active_id, standing_id, "dependency", "stand() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(active_id, busted_id, "dependency", "hit() → (if bust)", exitX="1", exitY="0.6", entryX="0", entryY="0.6"))
cells.append(edge(hand_id, hstate_id, "composition", "state  1", exitX="0.5", exitY="1", entryX="0.5", entryY="0"))
row2_bottom = y + max(hs0, hs1, hs2, hs3, hs4)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "strategy — dealer's play policy (Strategy)  +  factory"))
y += 34
box, dstrat_id, ht0 = uml_box(COL[0], y, 300, "DealerPlayStrategy", stereotype="interface",
    methods=["+ shouldHit(dealerHand): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, std_id, ht1 = uml_box(COL[1], y, 300, "StandardDealerStrategy",
    methods=["+ shouldHit(...): boolean  // total < 17"])
cells += box
cells.append(edge(std_id, dstrat_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
box, factory_id, ht2 = uml_box(COL[2], y, 320, "DeckFactory",
    methods=["+ createShuffledDeck(): Deck  {static}", "+ createShuffledDeck(seed): Deck  {static}"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(factory_id, deck_id, "dependency", "builds", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + max(ht0, ht1, ht2)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "exceptions"))
y += 34
box, rte_id, he0 = uml_box(COL[0], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, iha_id, he1 = uml_box(COL[1], y, 280, "IllegalHandActionException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, pnf_id, he2 = uml_box(COL[2], y, 280, "PlayerNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, rnr_id, he3 = uml_box(COL[3], y, 280, "RoundNotReadyException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, edq_id, he4 = uml_box(COL[4], y, 280, "EmptyDeckException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
for eid in (iha_id, pnf_id, rnr_id, edq_id):
    cells.append(edge(eid, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))
row4_bottom = y + max(he0, he1, he2, he3, he4)

y = row4_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 600, "BlackjackService",
    attrs=["- dealerStrategy: DealerPlayStrategy  {final}", "- deck: Deck",
           "- playersByName: Map<String,Player>", "- dealer: Player"],
    methods=["+ startRound(playerNames): void", "+ hit(playerName)/stand(playerName): void",
              "+ playDealerTurn(): void", "+ getRoundResult(): String",
              "+ getHandsSummary(): String",
              "- requireAllPlayersDone(): void", "- outcome(playerHand, dealerHand): String"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, deck_id, "composition", "deck  1", exitX="0.15", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, player_id, "composition", "players, dealer", exitX="0.4", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, dstrat_id, "dependency", "uses", exitX="0.65", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, factory_id, "dependency", "uses", exitX="0.85", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — hit() causing a bust
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: BlackjackService", 380), ("hand: Hand", 700),
                 ("state: ActiveState", 1000), ("deck: Deck", 1300)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: BlackjackService"], y, "hit(\"Bob\")"))
y += 40
cells2.append(msg(xs["svc: BlackjackService"], xs["state: ActiveState"], y, "hand.getState().requireActive()   // no-op, legal"))
y += 44
cells2.append(msg(xs["svc: BlackjackService"], xs["deck: Deck"], y, "draw()"))
y += 40
cells2.append(msg(xs["deck: Deck"], xs["svc: BlackjackService"], y, "return card   // e.g. SIX-HEARTS", kind="return"))
y += 44
cells2.append(msg(xs["svc: BlackjackService"], xs["state: ActiveState"], y, "hand.getState().hit(hand, card)"))
y += 40
cells2.append(msg(xs["state: ActiveState"], xs["hand: Hand"], y, "hand.addCard(card)"))
y += 44
cells2.append(msg(xs["state: ActiveState"], xs["hand: Hand"], y, "hand.getTotal()   // e.g. 22, soft-ace algorithm"))
y += 44
cells2.append(frame(xs["state: ActiveState"] - 60, xs["hand: Hand"] - xs["state: ActiveState"] + 160, y, 80,
                     "alt  [total > 21]"))
cells2.append(msg(xs["state: ActiveState"], xs["svc: BlackjackService"], y + 30, "return BustedState.INSTANCE", kind="return"))
cells2.append(divider(xs["state: ActiveState"] - 60, xs["hand: Hand"] - xs["state: ActiveState"] + 160, y + 55, "[else: return ActiveState.INSTANCE]"))
y += 100
cells2.append(msg(xs["svc: BlackjackService"], xs["hand: Hand"], y, "hand.setState(BustedState.INSTANCE)"))
y += 44
cells2.append(msg(xs["svc: BlackjackService"], xs[":Main"], y, "return", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — dealer's turn + round result
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: BlackjackService", 380), ("dealerHand: Hand", 700),
                 ("strategy: StandardDealerStrategy", 1000), ("deck: Deck", 1300)]:
    box, xx = lifeline(x, name, bottom=820)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(msg(xs[":Main"], xs["svc: BlackjackService"], y, "playDealerTurn()"))
y += 40
cells3.append(selfcall(xs["svc: BlackjackService"], y, "requireAllPlayersDone()   // every player ACTIVE? no -> proceed", loop_w=180, loop_h=22))
y += 60
cells3.append(frame(xs["svc: BlackjackService"] - 60, xs["deck: Deck"] - xs["svc: BlackjackService"] + 160, y, 150,
                     "loop  [dealerHand ACTIVE and strategy.shouldHit(dealerHand)]"))
y += 30
cells3.append(msg(xs["strategy: StandardDealerStrategy"], xs["svc: BlackjackService"], y, "shouldHit(dealerHand)   // total < 17", kind="return"))
y += 40
cells3.append(msg(xs["svc: BlackjackService"], xs["deck: Deck"], y, "draw()"))
y += 40
cells3.append(msg(xs["svc: BlackjackService"], xs["dealerHand: Hand"], y, "dealerHand.setState(state.hit(dealerHand, card))"))
y += 60
cells3.append(msg(xs["svc: BlackjackService"], xs["dealerHand: Hand"], y, "dealerHand.setState(state.stand(dealerHand))   // total >= 17, stop hitting"))
y += 50
cells3.append(msg(xs["svc: BlackjackService"], xs[":Main"], y, "return", kind="return"))
y += 60
cells3.append(msg(xs[":Main"], xs["svc: BlackjackService"], y, "getRoundResult()"))
y += 40
cells3.append(selfcall(xs["svc: BlackjackService"], y, "requireAllPlayersDone(); dealerHand not ACTIVE -> proceed", loop_w=190, loop_h=22))
y += 60
cells3.append(selfcall(xs["svc: BlackjackService"], y, "outcome(playerHand, dealerHand) per player   // bust/blackjack/total compare", loop_w=200, loop_h=22))
y += 60
cells3.append(msg(xs["svc: BlackjackService"], xs[":Main"], y, "return \"Dealer: 21 (BLACKJACK)\\nAlice: 21 (BLACKJACK) -> PUSH\\n...\"", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "blackjack.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=2000, h=1900),
    page("seqBust", "2 - Sequence - Hit Causing a Bust", PAGE2, w=1700, h=820),
    page("seqDealerResult", "3 - Sequence - Dealer Turn + Result", PAGE3, w=1700, h=880),
], outpath)
validate(outpath)
print("wrote", outpath)
