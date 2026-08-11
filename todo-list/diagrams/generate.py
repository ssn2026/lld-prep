# -*- coding: utf-8 -*-
"""Regenerates todo-list.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python todo-list/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — a Task and its own lifecycle state"))
y += 34
box, task_id, h1 = uml_box(COL[0], y, 340, "Task",
    attrs=["- id: String  {final}", "- title/description: String  {final}",
           "- priority: TaskPriority  {final}", "- dueInDays: int  {final}",
           "- createdOrder: int  {final}", "- state: TaskState"],
    methods=["+ start()/complete()/reopen()/archive(): void",
              "  // each delegates to state.xxx(this)", "+ getStatus(): TaskStatus"])
cells += box
box, status_id, h2 = uml_box(COL[1], y, 260, "TaskStatus", stereotype="enumeration",
    attrs=["TODO", "IN_PROGRESS", "DONE", "ARCHIVED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, prio_id, h3 = uml_box(COL[2], y, 240, "TaskPriority", stereotype="enumeration",
    attrs=["LOW", "MEDIUM", "HIGH"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
cells.append(edge(task_id, status_id, "dependency", "getStatus()", exitX="0.3", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(task_id, prio_id, "association", "priority", exitX="0.7", exitY="0", entryX="0.2", entryY="1"))
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "state — per-Task lifecycle (State), held on the Task itself"))
y += 34
box, tstate_id, hs0 = uml_box(COL[0], y, 340, "TaskState", stereotype="interface",
    methods=["+ getStatus(): TaskStatus", "+ start()/complete()/reopen()/archive(task): TaskState",
              "  // defaults throw InvalidTaskTransitionException;", "  // each state overrides only its legal moves"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, todo_id, hs1 = uml_box(COL[1], y, 260, "TodoState", stereotype="singleton",
    methods=["+ start(): InProgressState", "+ complete(): DoneState"])
cells += box
box, prog_id, hs2 = uml_box(COL[2], y, 260, "InProgressState", stereotype="singleton",
    methods=["+ complete(): DoneState"])
cells += box
box, done_id, hs3 = uml_box(COL[3], y, 260, "DoneState", stereotype="singleton",
    methods=["+ reopen(): TodoState", "+ archive(): ArchivedState"])
cells += box
box, arch_id, hs4 = uml_box(COL[1], y + max(hs0,hs1,hs2,hs3) + 20, 260, "ArchivedState", stereotype="singleton",
    methods=["  // terminal: every transition throws"])
cells += box
cells.append(edge(todo_id, tstate_id, "realize", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(prog_id, tstate_id, "realize", exitX="0.5", exitY="0", entryX="0.45", entryY="1"))
cells.append(edge(done_id, tstate_id, "realize", exitX="0.5", exitY="0", entryX="0.7", entryY="1"))
cells.append(edge(arch_id, tstate_id, "realize", exitX="0.5", exitY="0", entryX="0.3", entryY="1"))
cells.append(edge(todo_id, prog_id, "dependency", "start() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(prog_id, done_id, "dependency", "complete() →", exitX="1", exitY="0.3", entryX="0", entryY="0.3"))
cells.append(edge(done_id, arch_id, "dependency", "archive() →", exitX="0.3", exitY="1", entryX="0.7", entryY="0"))
cells.append(edge(done_id, todo_id, "dependency", "reopen() →", exitX="0", exitY="0.7", entryX="1", entryY="0.7"))
cells.append(edge(task_id, tstate_id, "composition", "state  1", exitX="0.7", exitY="1", entryX="0.5", entryY="0"))
row2_bottom = y + max(hs0,hs1,hs2,hs3) + 20 + hs4

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "strategy — how listTasks() orders results (Strategy)"))
y += 34
box, sort_id, ht0 = uml_box(COL[0], y, 320, "TaskSortStrategy", stereotype="interface",
    methods=["+ sort(tasks): List<Task>"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, due_id, ht1 = uml_box(COL[1], y, 280, "DueDateSortStrategy",
    methods=["+ sort(...): List<Task>  // by dueInDays asc"])
cells += box
box, pr_id, ht2 = uml_box(COL[2], y, 280, "PrioritySortStrategy",
    methods=["+ sort(...): List<Task>  // HIGH, MEDIUM, LOW"])
cells += box
box, created_id, ht3 = uml_box(COL[3], y, 280, "CreatedOrderSortStrategy",
    methods=["+ sort(...): List<Task>  // insertion order"])
cells += box
cells.append(edge(due_id, sort_id, "realize", exitX="0.5", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(pr_id, sort_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(created_id, sort_id, "realize", exitX="0.5", exitY="0", entryX="0.8", entryY="1"))
row3_bottom = y + max(ht0, ht1, ht2, ht3)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "observer — status-change notifications (Observer)"))
y += 34
box, listener_id, hl0 = uml_box(COL[0], y, 340, "TaskListener", stereotype="interface",
    methods=["+ onStatusChanged(taskId, from, to): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, hl1 = uml_box(COL[1], y, 300, "ConsoleTaskListener",
    methods=["+ onStatusChanged(...): void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row4_bottom = y + max(hl0, hl1)

y = row4_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)  +  exceptions"))
y += 34
box, repo_id, hr0 = uml_box(COL[0], y, 340, "TaskRepository",
    attrs=["- tasksById: LinkedHashMap<String,Task>"],
    methods=["+ save(task): void", "+ findById(taskId): Task  // throws TaskNotFoundException",
              "+ delete(taskId): void", "+ findAll(): List<Task>"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(repo_id, task_id, "aggregation", "0..*", exitX="1", exitY="0.5", entryX="0", entryY="0.8"))

box, rte_id, he0 = uml_box(COL[1], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, tnf_id, he1 = uml_box(COL[2], y, 300, "TaskNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, itt_id, he2 = uml_box(COL[3], y, 300, "InvalidTaskTransitionException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(tnf_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(itt_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row5_bottom = y + max(hr0, he0, he1, he2)

y = row5_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 580, "TodoListService",
    attrs=["- repository: TaskRepository", "- listeners: List<TaskListener>",
           "- idSeq/createdOrderSeq: AtomicInteger"],
    methods=["+ addTask(title, description, priority, dueInDays): String",
              "+ startTask/completeTask/reopenTask/archiveTask(taskId): void",
              "+ deleteTask(taskId): void", "+ getTask(taskId): Task",
              "+ listTasks(sortStrategy): List<Task>", "+ addListener(listener): void",
              "- transition(taskId, mutation): void  // diffs status, notifies on change"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box
cells.append(edge(svc_id, repo_id, "composition", "repository  1", exitX="0.2", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, sort_id, "dependency", "uses", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.8", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — ADD + COMPLETE (happy path)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: TodoListService", 380), ("repo: TaskRepository", 700),
                 ("task: Task", 1000), ("state: TaskState", 1300)]:
    box, xx = lifeline(x, name, bottom=760)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: TodoListService"], y, "addTask(\"BuyGroceries\", ..., MEDIUM, 2)"))
y += 40
cells2.append(msg(xs["svc: TodoListService"], xs["task: Task"], y, "«create» new Task(\"T1\", ..., MEDIUM, 2, order)", kind="create"))
y += 40
cells2.append(selfcall(xs["task: Task"], y, "state = TodoState.INSTANCE", loop_w=110, loop_h=20))
y += 50
cells2.append(msg(xs["svc: TodoListService"], xs["repo: TaskRepository"], y, "save(task)"))
y += 44
cells2.append(msg(xs["svc: TodoListService"], xs[":Main"], y, "return \"T1\"", kind="return"))
y += 60
cells2.append(msg(xs[":Main"], xs["svc: TodoListService"], y, "completeTask(\"T1\")"))
y += 40
cells2.append(msg(xs["svc: TodoListService"], xs["repo: TaskRepository"], y, "findById(\"T1\")"))
y += 40
cells2.append(msg(xs["repo: TaskRepository"], xs["svc: TodoListService"], y, "return task", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: TodoListService"], y, "before = task.getStatus()   // TODO", loop_w=130, loop_h=20))
y += 50
cells2.append(msg(xs["svc: TodoListService"], xs["task: Task"], y, "complete()"))
y += 40
cells2.append(msg(xs["task: Task"], xs["state: TaskState"], y, "state = state.complete(this)   // TodoState.complete()"))
y += 44
cells2.append(msg(xs["state: TaskState"], xs["task: Task"], y, "return DoneState.INSTANCE", kind="return"))
y += 44
cells2.append(selfcall(xs["svc: TodoListService"], y, "after = task.getStatus()   // DONE, differs -> notify listeners", loop_w=160, loop_h=22))
y += 60
cells2.append(msg(xs["svc: TodoListService"], xs[":Main"], y, "return", kind="return"))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — ILLEGAL TRANSITION ON A TERMINAL TASK
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 120), ("svc: TodoListService", 420), ("task: Task", 740), ("state: ArchivedState", 1060)]:
    box, xx = lifeline(x, name, bottom=560)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(note(xs[":Main"] - 60, y, 320, "T1 was already archived earlier in the script"))
y += 70
cells3.append(msg(xs[":Main"], xs["svc: TodoListService"], y, "startTask(\"T1\")"))
y += 40
cells3.append(msg(xs["svc: TodoListService"], xs["task: Task"], y, "start()"))
y += 40
cells3.append(msg(xs["task: Task"], xs["state: ArchivedState"], y, "state.start(this)   // ArchivedState overrides nothing"))
y += 44
cells3.append(selfcall(xs["state: ArchivedState"], y, "falls through to TaskState's default start()", loop_w=150, loop_h=22))
y += 60
cells3.append(msg(xs["state: ArchivedState"], xs["task: Task"], y, "throw InvalidTaskTransitionException(\"Cannot start a task that is ARCHIVED\")", kind="return"))
y += 60
cells3.append(msg(xs["task: Task"], xs["svc: TodoListService"], y, "propagates uncaught", kind="return"))
y += 44
cells3.append(msg(xs["svc: TodoListService"], xs[":Main"], y, "propagates uncaught -> caught in Main, printed as ERROR line", kind="return"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "todo-list.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1900, h=1800),
    page("seqComplete", "2 - Sequence - Add + Complete", PAGE2, w=1700, h=820),
    page("seqIllegal", "3 - Sequence - Illegal Transition", PAGE3, w=1500, h=620),
], outpath)
validate(outpath)
print("wrote", outpath)
