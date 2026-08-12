# -*- coding: utf-8 -*-
"""Regenerates facebook.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python facebook/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — users, posts, and a FriendRequest (owns its own State)"))
y += 34
box, user_id, h1 = uml_box(COL[0], y, 260, "User",
    attrs=["- id: String  {final}", "- name: String  {final}"])
cells += box
box, post_id, h2 = uml_box(COL[1], y, 280, "Post",
    attrs=["- id/authorId/content: String  {final}", "- sequence: int  {final}"])
cells += box
box, freq_id, h3 = uml_box(COL[2], y, 300, "FriendRequest",
    attrs=["- id/fromUserId/toUserId: String  {final}", "- state: FriendRequestState"],
    methods=["+ accept()/reject(): void", "  // delegates to state.xxx(this)", "+ getStatus(): FriendRequestStatus"])
cells += box
box, status_id, h4 = uml_box(COL[3], y, 260, "FriendRequestStatus", stereotype="enumeration",
    attrs=["PENDING, ACCEPTED, REJECTED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(freq_id, status_id, "dependency", "getStatus()", exitX="0.7", exitY="0", entryX="0.3", entryY="1"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "state — per-FriendRequest lifecycle (State), held on the request itself"))
y += 34
box, fstate_id, hs0 = uml_box(COL[0], y, 320, "FriendRequestState", stereotype="interface",
    methods=["+ getStatus(): FriendRequestStatus", "+ accept(request)/reject(request): FriendRequestState",
              "  // defaults throw; only PendingState overrides"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, pend_id, hs1 = uml_box(COL[1], y, 240, "PendingState", stereotype="singleton",
    methods=["+ accept(): AcceptedState", "+ reject(): RejectedState"])
cells += box
box, acc_id, hs2 = uml_box(COL[2], y, 220, "AcceptedState", stereotype="singleton", methods=["  // terminal"])
cells += box
box, rej_id, hs3 = uml_box(COL[3], y, 220, "RejectedState", stereotype="singleton", methods=["  // terminal"])
cells += box
for sid in (pend_id, acc_id, rej_id):
    cells.append(edge(sid, fstate_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(pend_id, acc_id, "dependency", "accept() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(pend_id, rej_id, "dependency", "reject() →", exitX="0.3", exitY="1", entryX="0.7", entryY="0"))
cells.append(edge(freq_id, fstate_id, "composition", "state  1", exitX="0.7", exitY="1", entryX="0.5", entryY="0"))
row2_bottom = y + max(hs0, hs1, hs2, hs3)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "strategy — how a feed is ordered (Strategy)  +  observer — feed events (Observer)"))
y += 34
box, rank_id, ht0 = uml_box(COL[0], y, 300, "FeedRankingStrategy", stereotype="interface",
    methods=["+ rank(posts): List<Post>"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, chrono_id, ht1 = uml_box(COL[1], y, 300, "ChronologicalFeedStrategy",
    methods=["+ rank(...): List<Post>  // newest sequence first"])
cells += box
cells.append(edge(chrono_id, rank_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))

box, listener_id, ho0 = uml_box(COL[2], y, 320, "FeedListener", stereotype="interface",
    methods=["+ onNewPost(authorId, postId, content): void", "+ onFriendRequestAccepted(from, to): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, ho1 = uml_box(COL[3], y, 300, "ConsoleFeedListener",
    methods=["+ onNewPost(...)/onFriendRequestAccepted(...): void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + max(ht0, ht1, ho0, ho1)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)  +  exceptions"))
y += 34
box, urepo_id, hr0 = uml_box(COL[0], y, 280, "UserRepository",
    methods=["+ save(user): void", "+ findById(id): User"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, frepo_id, hr1 = uml_box(COL[1], y, 300, "FriendRequestRepository",
    methods=["+ save(request): void", "+ findById(id): FriendRequest",
              "+ hasPendingBetween(a, b): boolean"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, shiprepo_id, hr2 = uml_box(COL[2], y, 280, "FriendshipRepository",
    attrs=["- friendsByUser: Map<String,Set<String>>"],
    methods=["+ addFriendship(a, b): void", "+ areFriends(a, b): boolean", "+ getFriendsOf(id): Set<String>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
box, prepo_id, hr3 = uml_box(COL[3], y, 280, "PostRepository",
    methods=["+ save(post): void", "+ findByAuthors(ids): List<Post>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
row4_bottom = y + max(hr0, hr1, hr2, hr3)

y = row4_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 640, "FacebookService",
    attrs=["- userRepository/friendshipRepository/", "  requestRepository/postRepository", "- listeners: List<FeedListener>"],
    methods=["+ registerUser(name): String", "+ sendFriendRequest(from, to): String",
              "+ acceptFriendRequest(id)/rejectFriendRequest(id): void",
              "+ createPost(userId, content): String",
              "+ getNewsFeed(userId, rankingStrategy): List<Post>", "+ addListener(listener): void"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, urepo_id, "composition", "1", exitX="0.1", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, frepo_id, "composition", "1", exitX="0.3", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, shiprepo_id, "composition", "1", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, prepo_id, "composition", "1", exitX="0.7", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.9", exitY="0", entryX="0.3", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — getNewsFeed() (self + friends, ranked)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: FacebookService", 380), ("shipRepo: FriendshipRepository", 700),
                 ("postRepo: PostRepository", 1000), ("strategy: ChronologicalFeedStrategy", 1300)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: FacebookService"], y, "getNewsFeed(\"u1\", new ChronologicalFeedStrategy())"))
y += 40
cells2.append(msg(xs["svc: FacebookService"], xs["shipRepo: FriendshipRepository"], y, "getFriendsOf(\"u1\")"))
y += 40
cells2.append(msg(xs["shipRepo: FriendshipRepository"], xs["svc: FacebookService"], y, "return {\"u2\"}", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: FacebookService"], y, "authorIds = {\"u2\"} + \"u1\" (self included)", loop_w=170, loop_h=22))
y += 60
cells2.append(msg(xs["svc: FacebookService"], xs["postRepo: PostRepository"], y, "findByAuthors({\"u1\",\"u2\"})"))
y += 40
cells2.append(msg(xs["postRepo: PostRepository"], xs["svc: FacebookService"], y, "return [p1(u1), p2(u2)]", kind="return"))
y += 44
cells2.append(msg(xs["svc: FacebookService"], xs["strategy: ChronologicalFeedStrategy"], y, "rank([p1, p2])"))
y += 40
cells2.append(selfcall(xs["strategy: ChronologicalFeedStrategy"], y, "sort by sequence, descending", loop_w=130, loop_h=20))
y += 50
cells2.append(msg(xs["strategy: ChronologicalFeedStrategy"], xs["svc: FacebookService"], y, "return [p2, p1]   // newest first", kind="return"))
y += 50
cells2.append(msg(xs["svc: FacebookService"], xs[":Main"], y, "return [p2, p1]", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "facebook.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1950, h=1600),
    page("seqFeed", "2 - Sequence - getNewsFeed()", PAGE2, w=1700, h=820),
], outpath)
validate(outpath)
print("wrote", outpath)
