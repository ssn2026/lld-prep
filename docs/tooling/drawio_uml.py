# -*- coding: utf-8 -*-
"""
Reusable draw.io (.drawio / mxGraph) generator for LLD-prep problems.

Used by /learning to produce a UML class diagram + sequence diagram(s) for
each problem, saved to `<problem>/diagrams/<problem>.drawio`. Import this
module and supply data (class fields/methods, edges, sequence messages) —
do not re-derive the escaping or geometry logic per problem; that's the
whole point of this file existing.

Escaping note (the one bug worth knowing about before touching this file):
mxCell "value" attributes are XML attribute text. UML class boxes want real
<b>/<br> tags inside that value so draw.io's html=1 renderer can bold names
and break lines. That means TWO escaping layers, applied in order:
  1. htext() on each piece of dynamic text (handles generics like List<X>
     so a literal '<' doesn't get mistaken for a tag).
  2. esc() on the FULLY ASSEMBLED string (real tags + htext'd content)
     right before it goes into the XML attribute.
Escaping pieces individually and then joining with a raw, unescaped "<br>"
is invalid — the "<br>" itself must survive the esc() pass too.
Anywhere a label has no intentional HTML tags (edges, lifelines, notes),
just use esc() once; there is nothing to double-escape.

Usage sketch:
    from drawio_uml import *
    cells, hit_id, h = uml_box(40, 40, 300, "Foo", attrs=[...], methods=[...])
    ...
    write_mxfile([page("classDiagram", "1 - Class Diagram", "\\n".join(cells))],
                 "problem/diagrams/problem.drawio")
    validate("problem/diagrams/problem.drawio")
"""
import html as _html

_id = [0]


def nid():
    _id[0] += 1
    return "n%d" % _id[0]


def reset_ids():
    _id[0] = 0


def esc(s):
    return _html.escape(str(s), quote=True)


def htext(s):
    """Escape dynamic text so it's safe as HTML *content* (handles generics like List<X>)."""
    return str(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


# ---------------------------------------------------------------------------
# UML class box: header (name/stereotype) + attrs compartment + methods
# compartment + an invisible full-footprint "hitbox" cell that every edge
# should connect to (so edges land on the class as a whole, not one row).
# ---------------------------------------------------------------------------
LINE_H = 16
HEADER_H = 30


def uml_box(x, y, w, name, attrs=None, methods=None, stereotype=None,
            header_fill="#dae8fc", header_stroke="#6c8ebf", dashed=False):
    """Returns (cells: list[str], hitbox_id: str, total_height: int)."""
    attrs = attrs or []
    methods = methods or []
    attrs_h = max(LINE_H, len(attrs) * LINE_H + 8)
    methods_h = max(LINE_H, len(methods) * LINE_H + 8)
    total_h = HEADER_H + attrs_h + methods_h

    header_inner = (("«" + htext(stereotype) + "»<br>") if stereotype else "") + "<b>" + htext(name) + "</b>"
    header_label = esc(header_inner)
    header_style = ("rounded=0;whiteSpace=wrap;html=1;fillColor=%s;strokeColor=%s;"
                     "align=center;verticalAlign=middle;fontSize=12;" % (header_fill, header_stroke))
    if dashed:
        header_style += "dashed=1;"

    attrs_inner = "<br>".join(htext(a) for a in attrs) if attrs else " "
    methods_inner = "<br>".join(htext(m) for m in methods) if methods else " "
    attrs_label = esc(attrs_inner)
    methods_label = esc(methods_inner)
    body_style = ("rounded=0;whiteSpace=wrap;html=1;align=left;verticalAlign=top;"
                  "spacingLeft=6;spacingTop=4;fontSize=11;fillColor=#ffffff;strokeColor=%s;" % header_stroke)
    if dashed:
        body_style += "dashed=1;"

    header_id, attrs_id, methods_id, hit_id = nid(), nid(), nid(), nid()
    cells = [
        '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="1">'
        '<mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
        % (header_id, header_label, header_style, x, y, w, HEADER_H),

        '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="1">'
        '<mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
        % (attrs_id, attrs_label, body_style, x, y + HEADER_H, w, attrs_h),

        '<mxCell id="%s" value="%s" style="%s" vertex="1" parent="1">'
        '<mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
        % (methods_id, methods_label, body_style, x, y + HEADER_H + attrs_h, w, methods_h),

        '<mxCell id="%s" value="" style="rounded=0;html=1;fillColor=none;strokeColor=none;" '
        'vertex="1" parent="1"><mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
        % (hit_id, x, y, w, total_h),
    ]
    return cells, hit_id, total_h


def group_title(x, y, text, width=520):
    tid = nid()
    return ('<mxCell id="%s" value="%s" style="text;html=1;fontStyle=1;fontSize=14;fontColor=#333333;'
            'align=left;verticalAlign=middle;" vertex="1" parent="1">'
            '<mxGeometry x="%d" y="%d" width="%d" height="24" as="geometry"/></mxCell>'
            % (tid, esc(text), x, y, width))


# ---------------------------------------------------------------------------
# Class-diagram edges (UML notation)
# ---------------------------------------------------------------------------
def edge(src, dst, kind, label="", exitX=None, exitY=None, entryX=None, entryY=None):
    """kind: inherit | realize | composition | aggregation | dependency | association"""
    eid = nid()
    style = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;fontSize=10;jettySize=auto;"
    if kind == "inherit":
        style += "endArrow=block;endFill=0;endSize=16;startArrow=none;"
    elif kind == "realize":
        style += "endArrow=block;endFill=0;endSize=16;dashed=1;startArrow=none;"
    elif kind == "composition":
        style += "startArrow=diamond;startFill=1;startSize=14;endArrow=none;"
    elif kind == "aggregation":
        style += "startArrow=diamond;startFill=0;startSize=14;endArrow=none;"
    elif kind == "dependency":
        style += "endArrow=open;endFill=0;endSize=12;dashed=1;"
    else:
        style += "endArrow=open;endFill=0;endSize=12;"
    if exitX is not None:
        style += "exitX=%s;exitY=%s;exitDx=0;exitDy=0;" % (exitX, exitY)
    if entryX is not None:
        style += "entryX=%s;entryY=%s;entryDx=0;entryDy=0;" % (entryX, entryY)
    return ('<mxCell id="%s" value="%s" style="%s" edge="1" parent="1" source="%s" target="%s">'
            '<mxGeometry relative="1" as="geometry"/></mxCell>'
            % (eid, esc(label), style, src, dst))


# ---------------------------------------------------------------------------
# Sequence-diagram helpers: lifelines, call/return messages, self-calls,
# alt/loop frames, dividers, sticky notes.
# ---------------------------------------------------------------------------
def lifeline(x, name, top=40, bottom=760):
    """Single-line plain-text name recommended (avoid embedding raw HTML tags)."""
    bid = nid()
    box = ('<mxCell id="%s" value="%s" style="rounded=0;whiteSpace=wrap;html=1;'
           'fillColor=#dae8fc;strokeColor=#6c8ebf;fontStyle=1;fontSize=12;" vertex="1" parent="1">'
           '<mxGeometry x="%d" y="%d" width="180" height="40" as="geometry"/></mxCell>'
           % (bid, esc(name), x - 90, top))
    lineid = nid()
    line = ('<mxCell id="%s" value="" style="endArrow=none;dashed=1;html=1;strokeColor=#666666;" '
            'edge="1" parent="1" source="%s"><mxGeometry relative="1" as="geometry">'
            '<mxPoint x="%d" y="%d" as="sourcePoint"/><mxPoint x="%d" y="%d" as="targetPoint"/>'
            '</mxGeometry></mxCell>'
            % (lineid, bid, x, top + 40, x, bottom))
    return [box, line], x


def msg(x1, x2, y, label, kind="call"):
    """kind: call | return | create"""
    eid = nid()
    if kind == "call":
        style = "html=1;endArrow=block;endFill=1;endSize=10;fontSize=10;"
    elif kind == "return":
        style = "html=1;endArrow=open;endFill=0;endSize=10;dashed=1;fontSize=10;fontColor=#666666;"
    else:
        style = "html=1;endArrow=open;endFill=0;endSize=10;dashed=1;fontSize=10;strokeColor=#82b366;"
    return ('<mxCell id="%s" value="%s" style="%s" edge="1" parent="1">'
            '<mxGeometry relative="1" as="geometry"><mxPoint x="%d" y="%d" as="sourcePoint"/>'
            '<mxPoint x="%d" y="%d" as="targetPoint"/>'
            '<mxPoint x="0" y="-8" as="offset"/></mxGeometry></mxCell>'
            % (eid, esc(label), style, x1, y, x2, y))


def selfcall(x, y, label, loop_w=90, loop_h=26):
    eid = nid()
    style = "html=1;endArrow=block;endFill=1;endSize=10;fontSize=10;rounded=0;"
    pts = '<Array as="points"><mxPoint x="%d" y="%d"/><mxPoint x="%d" y="%d"/></Array>' \
          % (x + loop_w, y, x + loop_w, y + loop_h)
    return ('<mxCell id="%s" value="%s" style="%s" edge="1" parent="1">'
            '<mxGeometry relative="1" as="geometry"><mxPoint x="%d" y="%d" as="sourcePoint"/>'
            '<mxPoint x="%d" y="%d" as="targetPoint"/>%s</mxGeometry></mxCell>'
            % (eid, esc(label), style, x, y, x, y + loop_h, pts))


def frame(x, w, y, h, title):
    """A dashed UML combined-fragment box (alt/loop/opt). Put the condition in `title`."""
    fid = nid()
    return ('<mxCell id="%s" value="%s" style="shape=umlFrame;whiteSpace=wrap;html=1;'
            'fillColor=none;strokeColor=#666666;fontSize=11;width=80;height=30;align=left;verticalAlign=top;" '
            'vertex="1" parent="1"><mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
            % (fid, esc(title), x, y, w, h))


def divider(x, w, y, label):
    """The dashed horizontal split inside an alt frame, labeled with the else-condition."""
    did = nid()
    return ('<mxCell id="%s" value="%s" style="line;html=1;strokeColor=#666666;dashed=1;'
            'align=left;verticalAlign=top;fontSize=10;fontStyle=2;spacingLeft=6;" vertex="1" parent="1">'
            '<mxGeometry x="%d" y="%d" width="%d" height="14" as="geometry"/></mxCell>'
            % (did, esc(label), x, y, w))


def note(x, y, w, text, h=46):
    nid_ = nid()
    return ('<mxCell id="%s" value="%s" style="shape=note;whiteSpace=wrap;html=1;'
            'fillColor=#fff2cc;strokeColor=#d6b656;fontSize=10;align=left;spacingLeft=4;size=14;" '
            'vertex="1" parent="1"><mxGeometry x="%d" y="%d" width="%d" height="%d" as="geometry"/></mxCell>'
            % (nid_, esc(text), x, y, w, h))


# ---------------------------------------------------------------------------
# Page / file assembly + validation
# ---------------------------------------------------------------------------
def page(diagram_id, name, body, w=2000, h=2400):
    return ('<diagram id="%s" name="%s">'
            '<mxGraphModel dx="1200" dy="800" grid="1" gridSize="10" guides="1" tooltips="1" '
            'connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="%d" pageHeight="%d" math="0" shadow="0">'
            '<root><mxCell id="0"/><mxCell id="1" parent="0"/>%s</root></mxGraphModel></diagram>'
            % (diagram_id, esc(name), w, h, body))


def write_mxfile(pages, outpath):
    xml = ('<mxfile host="app.diagrams.net" agent="claude-code" version="24.7.5" type="device">'
           + "".join(pages) + "</mxfile>")
    with open(outpath, "w", encoding="utf-8") as f:
        f.write(xml)
    return outpath


def validate(path):
    """One terse well-formedness + dangling-edge-reference check. Prints a single PASS/FAIL line."""
    import xml.etree.ElementTree as ET
    root = ET.parse(path).getroot()
    problems = []
    for d in root.findall("diagram"):
        cells = d.find("mxGraphModel").find("root").findall("mxCell")
        ids = {c.get("id") for c in cells}
        for c in cells:
            if c.get("edge") == "1":
                for attr in ("source", "target"):
                    ref = c.get(attr)
                    if ref and ref not in ids:
                        problems.append("%s: dangling %s=%s on edge %s" % (d.get("name"), attr, ref, c.get("id")))
    if problems:
        print("VALIDATE FAIL:", "; ".join(problems[:5]))
        return False
    print("VALIDATE PASS: %d pages, well-formed, no dangling refs" % len(root.findall("diagram")))
    return True
