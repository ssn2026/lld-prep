# -*- coding: utf-8 -*-
"""Regenerates logger-service.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python logger-service/diagrams/generate.py
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
COL = [40, 420, 800, 1180]
y = 20

cells.append(group_title(COL[0], y, "model — one log event"))
y += 34
box, level_id, h1 = uml_box(COL[0], y, 260, "LogLevel", stereotype="enumeration",
    attrs=["DEBUG < INFO < WARN < ERROR", "  // ordinal() drives threshold comparison"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, event_id, h2 = uml_box(COL[1], y, 300, "LogEvent",
    attrs=["- seq: int  {final}", "- level: LogLevel  {final}", "- message: String  {final}"],
    methods=["+ getSeq()/getLevel()/getMessage()"])
cells += box
cells.append(edge(event_id, level_id, "association", "level", exitX="0.3", exitY="0", entryX="0.3", entryY="1"))
row1_bottom = y + max(h1, h2)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — how a LogEvent becomes one line of text (Strategy)"))
y += 34
box, fmt_id, ht0 = uml_box(COL[0], y, 300, "LogFormatter", stereotype="interface",
    methods=["+ format(event): String"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, plain_id, ht1 = uml_box(COL[1], y, 300, "PlainTextFormatter",
    methods=["+ format(...): String  // \"[seq] LEVEL message\""])
cells += box
box, json_id, ht2 = uml_box(COL[2], y, 300, "JsonFormatter",
    methods=["+ format(...): String  // {\"seq\":..,\"level\":..,\"message\":..}"])
cells += box
cells.append(edge(plain_id, fmt_id, "realize", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
cells.append(edge(json_id, fmt_id, "realize", exitX="0.5", exitY="0", entryX="0.75", entryY="1"))
row2_bottom = y + max(ht0, ht1, ht2)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "observer — where formatted lines get sent (Observer)  +  exceptions"))
y += 34
box, app_id, ho0 = uml_box(COL[0], y, 300, "LogAppender", stereotype="interface",
    methods=["+ append(formattedLine): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, ho1 = uml_box(COL[1], y, 280, "ConsoleLogAppender",
    methods=["+ append(...): void  // println"])
cells += box
box, file_id, ho2 = uml_box(COL[2], y, 300, "FileLogAppender",
    attrs=["- path: Path  {final}"],
    methods=["+ append(...): void  // real file I/O, CREATE+APPEND", "  // throws LogWriteException"])
cells += box
cells.append(edge(console_id, app_id, "realize", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
cells.append(edge(file_id, app_id, "realize", exitX="0.5", exitY="0", entryX="0.75", entryY="1"))

box, rte_id, he0 = uml_box(COL[3], y, 240, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, ile_id, he1 = uml_box(COL[3], y + max(ho0,ho1,ho2) + 20, 260, "InvalidLogLevelException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, lwe_id, he2 = uml_box(COL[3], y + max(ho0,ho1,ho2) + 20 + he1 + 16, 260, "LogWriteException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(ile_id, rte_id, "inherit", exitX="1", exitY="0.5", entryX="1", entryY="1"))
cells.append(edge(lwe_id, rte_id, "inherit", exitX="1", exitY="0.5", entryX="1", entryY="1"))
row3_bottom = y + max(ho0, ho1, ho2, he0) + 20 + he1 + 16 + he2

y = row3_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point (Singleton)"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 560, "LoggerService", stereotype="singleton",
    attrs=["- INSTANCE: LoggerService  {static, final}", "- appenders: List<LogAppender>",
           "- formatter: LogFormatter", "- minLevel: LogLevel", "- seq: int"],
    methods=["+ getInstance(): LoggerService  {static}", "+ setMinLevel(level)/setFormatter(formatter): void",
              "+ addAppender(appender): void",
              "+ debug/info/warn/error(message): void", "+ log(level, message): void",
              "  // filters by minLevel, formats once, fans out to every appender"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, event_id, "dependency", "creates", exitX="0.2", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, fmt_id, "association", "formatter  1", exitX="0.45", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, app_id, "composition", "appenders  0..*", exitX="0.7", exitY="0", entryX="0.3", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — log() below threshold vs. above threshold, fanning out
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("logger: LoggerService", 380), ("formatter: LogFormatter", 700),
                 ("console: ConsoleLogAppender", 1000), ("file: FileLogAppender", 1300)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["logger: LoggerService"], y, "log(DEBUG, \"...\")   // minLevel = INFO"))
y += 40
cells2.append(frame(xs["logger: LoggerService"] - 60, xs["file: FileLogAppender"] - xs["logger: LoggerService"] + 160, y, 70,
                     "alt  [level.ordinal() < minLevel.ordinal()]"))
cells2.append(selfcall(xs["logger: LoggerService"], y + 24, "return  // filtered, no formatting, no appenders touched", loop_w=160, loop_h=20))
cells2.append(divider(xs["logger: LoggerService"] - 60, xs["file: FileLogAppender"] - xs["logger: LoggerService"] + 160, y + 46, "[else: level.ordinal() >= minLevel.ordinal()]"))
y += 90
cells2.append(msg(xs[":Main"], xs["logger: LoggerService"], y, "log(INFO, \"server started on port 8080\")"))
y += 44
cells2.append(selfcall(xs["logger: LoggerService"], y, "event = new LogEvent(++seq, INFO, message)", loop_w=140, loop_h=20))
y += 50
cells2.append(msg(xs["logger: LoggerService"], xs["formatter: LogFormatter"], y, "format(event)"))
y += 40
cells2.append(msg(xs["formatter: LogFormatter"], xs["logger: LoggerService"], y, "return \"[1] INFO server started on port 8080\"", kind="return"))
y += 50
cells2.append(msg(xs["logger: LoggerService"], xs["console: ConsoleLogAppender"], y, "append(formatted)"))
y += 44
cells2.append(msg(xs["logger: LoggerService"], xs["file: FileLogAppender"], y, "append(formatted)   // writes a real line to disk"))
y += 50
cells2.append(msg(xs["logger: LoggerService"], xs[":Main"], y, "return", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "logger-service.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1900, h=1500),
    page("seqLog", "2 - Sequence - Filter + Format + Fan-out", PAGE2, w=1700, h=820),
], outpath)
validate(outpath)
print("wrote", outpath)
