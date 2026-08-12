# -*- coding: utf-8 -*-
"""Regenerates notification-system.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python notification-system/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model  +  builder — constructing a Notification (Builder)"))
y += 34
box, notif_id, h1 = uml_box(COL[0], y, 300, "Notification",
    attrs=["- title: String  {final}", "- body: String  {final}", "- priority: NotificationPriority  {final}"],
    methods=["+ getTitle()/getBody()/getPriority()", "  // no public constructor"])
cells += box
box, builder_id, h2 = uml_box(COL[1], y, 320, "NotificationBuilder",
    attrs=["- title/body: String", "- priority: NotificationPriority  = NORMAL"],
    methods=["+ title(t)/body(b)/priority(p): NotificationBuilder", "+ build(): Notification",
              "  // throws IncompleteNotificationException if title/body missing"])
cells += box
box, prio_id, h3 = uml_box(COL[2], y, 240, "NotificationPriority", stereotype="enumeration",
    attrs=["LOW, NORMAL, HIGH"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, ctype_id, h4 = uml_box(COL[3], y, 240, "ChannelType", stereotype="enumeration",
    attrs=["EMAIL, SMS, PUSH"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(builder_id, notif_id, "dependency", "builds", exitX="0.5", exitY="0", entryX="0.5", entryY="0",
                   ))
cells.append(edge(notif_id, prio_id, "association", "priority", exitX="0.7", exitY="1", entryX="0.3", entryY="0"))
row1_bottom = y + max(h1, h2, h3, h4)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "decorator — enriching the rendered message (Decorator)"))
y += 34
box, content_id, hd0 = uml_box(COL[0], y, 300, "NotificationContent", stereotype="interface",
    methods=["+ render(): String"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, plain_id, hd1 = uml_box(COL[1], y, 300, "PlainContent",
    attrs=["- notification: Notification  {final}"],
    methods=["+ render(): String  // \"[priority] title: body\""])
cells += box
box, urgent_id, hd2 = uml_box(COL[2], y, 300, "UrgentPrefixDecorator",
    attrs=["- inner: NotificationContent  {final}"],
    methods=["+ render(): String  // \"*** URGENT *** \" + inner.render()"])
cells += box
box, sig_id, hd3 = uml_box(COL[3], y, 300, "SignatureDecorator",
    attrs=["- inner: NotificationContent  {final}"],
    methods=["+ render(): String  // inner.render() + \"\\n-- Sent by NotifyService\""])
cells += box
for cid in (plain_id, urgent_id, sig_id):
    cells.append(edge(cid, content_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(urgent_id, content_id, "aggregation", "inner", exitX="0", exitY="0.6", entryX="1", entryY="0.6"))
cells.append(edge(sig_id, content_id, "aggregation", "inner", exitX="0", exitY="0.8", entryX="1", entryY="0.8"))
row2_bottom = y + max(hd0, hd1, hd2, hd3)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "observer — per-user channel subscriptions (Observer)  +  repository  +  exceptions"))
y += 34
box, channel_id, ho0 = uml_box(COL[0], y, 300, "NotificationChannel", stereotype="interface",
    methods=["+ send(userId, renderedMessage): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, email_id, ho1 = uml_box(COL[1], y, 200, "EmailChannel", methods=["+ send(...): void"])
cells += box
box, sms_id, ho2 = uml_box(COL[1], y + ho1 + 16, 200, "SmsChannel", methods=["+ send(...): void"])
cells += box
box, push_id, ho3 = uml_box(COL[1], y + ho1 + ho2 + 32, 200, "PushChannel", methods=["+ send(...): void"])
cells += box
for cid in (email_id, sms_id, push_id):
    cells.append(edge(cid, channel_id, "realize", exitX="0", exitY="0.5", entryX="1", entryY="0.5"))

box, registry_id, ho4 = uml_box(COL[2], y, 340, "SubscriptionRegistry",
    attrs=["- subscriptionsByUser: Map<String,Map<ChannelType,NotificationChannel>>"],
    methods=["+ subscribe(userId, type, channel): void", "+ unsubscribe(userId, type): boolean",
              "+ getChannelsFor(userId): Collection<NotificationChannel>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(registry_id, channel_id, "aggregation", "0..*", exitX="1", exitY="0.5", entryX="0", entryY="0.5"))

box, rte_id, he0 = uml_box(COL[3], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, inc_id, he1 = uml_box(COL[3], y + he0 + 20, 280, "IncompleteNotificationException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(inc_id, rte_id, "inherit", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + max(ho0, ho1+ho2+ho3+32, ho4, he0 + 20 + he1)

y = row3_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "NotificationService",
    attrs=["- registry: SubscriptionRegistry  {final}"],
    methods=["+ subscribe(userId, type): void", "+ unsubscribe(userId, type): boolean",
              "+ send(userId, notification): int  // returns channels notified",
              "- newChannel(type): NotificationChannel"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, registry_id, "composition", "registry  1", exitX="0.3", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, content_id, "dependency", "composes", exitX="0.6", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, notif_id, "dependency", "reads", exitX="0.8", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — send() composing decorators then fanning out
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: NotificationService", 380), ("registry: SubscriptionRegistry", 700),
                 ("plain: PlainContent", 1000), ("urgent: UrgentPrefixDecorator", 1300), ("email: EmailChannel", 1600)]:
    box, xx = lifeline(x, name, bottom=880)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: NotificationService"], y, "send(\"Alice\", notification)   // priority = HIGH"))
y += 40
cells2.append(msg(xs["svc: NotificationService"], xs["plain: PlainContent"], y, "«create» new PlainContent(notification)", kind="create"))
y += 44
cells2.append(selfcall(xs["svc: NotificationService"], y, "content = new SignatureDecorator(content)", loop_w=170, loop_h=22))
y += 60
cells2.append(msg(xs["svc: NotificationService"], xs["urgent: UrgentPrefixDecorator"], y, "«create» new UrgentPrefixDecorator(content)   // priority == HIGH", kind="create"))
y += 50
cells2.append(msg(xs["svc: NotificationService"], xs["urgent: UrgentPrefixDecorator"], y, "content.render()"))
y += 40
cells2.append(selfcall(xs["urgent: UrgentPrefixDecorator"], y, "\"*** URGENT *** \" + inner.render()   // delegates down the chain", loop_w=190, loop_h=22))
y += 60
cells2.append(msg(xs["urgent: UrgentPrefixDecorator"], xs["svc: NotificationService"], y, "return fully-decorated string", kind="return"))
y += 50
cells2.append(msg(xs["svc: NotificationService"], xs["registry: SubscriptionRegistry"], y, "getChannelsFor(\"Alice\")"))
y += 40
cells2.append(msg(xs["registry: SubscriptionRegistry"], xs["svc: NotificationService"], y, "return [emailChannel, smsChannel]", kind="return"))
y += 50
cells2.append(frame(xs["svc: NotificationService"] - 60, xs["email: EmailChannel"] - xs["svc: NotificationService"] + 160, y, 70,
                     "loop  [for each subscribed channel]"))
cells2.append(msg(xs["svc: NotificationService"], xs["email: EmailChannel"], y + 30, "channel.send(\"Alice\", rendered)"))
y += 100
cells2.append(msg(xs["svc: NotificationService"], xs[":Main"], y, "return 2   // channel count", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "notification-system.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1950, h=1500),
    page("seqSend", "2 - Sequence - Compose Decorators + Fan-out", PAGE2, w=1900, h=940),
], outpath)
validate(outpath)
print("wrote", outpath)
