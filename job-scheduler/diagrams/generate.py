# -*- coding: utf-8 -*-
"""Regenerates job-scheduler.drawio from docs/tooling/drawio_uml.py.

Run from the repo root: python job-scheduler/diagrams/generate.py
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

cells.append(group_title(COL[0], y, "model — a Job and one scheduled occurrence of it (Command)"))
y += 34
box, job_id, h1 = uml_box(COL[0], y, 300, "Job", stereotype="interface",
    methods=["+ execute(): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, status_id, h2 = uml_box(COL[1], y, 280, "JobStatus", stereotype="enumeration",
    attrs=["SCHEDULED", "RUNNING", "COMPLETED", "FAILED", "CANCELLED"],
    header_fill="#fff2cc", header_stroke="#d6b656")
cells += box
box, task_id, h3 = uml_box(COL[2], y, 380, "ScheduledTask", stereotype="Delayed",
    attrs=["- jobId: String", "- job: Job", "- trigger: Trigger",
           "- executionTimeMillis: long  {final}", "- sequence: long  {final}",
           "- status: AtomicReference<JobStatus>", "- cancelRequested: AtomicBoolean"],
    methods=["+ getDelay(unit): long", "+ compareTo(other): int",
              "+ cancel(): boolean  // CAS SCHEDULED->CANCELLED",
              "+ markRunning()/markCompleted()/markFailed(): void",
              "+ withNextExecution(nextTime): ScheduledTask",
              "  // new instance -- never mutate the delay key in place"])
cells += box
cells.append(edge(task_id, job_id, "composition", "job  1", exitX="0", exitY="0.3", entryX="1", entryY="0.5"))
cells.append(edge(task_id, status_id, "association", "status", exitX="0.3", exitY="0", entryX="0.7", entryY="1"))
row1_bottom = y + max(h1, h2, h3)

y = row1_bottom + 70
cells.append(group_title(COL[0], y, "strategy — when a job should next run (Strategy)"))
y += 34
box, trigger_id, ht0 = uml_box(COL[0], y, 340, "Trigger", stereotype="interface",
    methods=["+ firstExecutionTime(scheduledAt): long", "+ nextExecutionTime(previous): long",
              "+ isRecurring(): boolean"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, onetime_id, ht1 = uml_box(COL[1], y, 320, "OneTimeTrigger",
    attrs=["- delayMillis: long"],
    methods=["+ firstExecutionTime(...): long  // scheduledAt + delay", "+ isRecurring(): boolean  // false"])
cells += box
box, fixedrate_id, ht2 = uml_box(COL[2], y, 340, "FixedRateTrigger",
    attrs=["- initialDelayMillis: long", "- intervalMillis: long"],
    methods=["+ firstExecutionTime(...): long  // scheduledAt + initialDelay",
              "+ nextExecutionTime(prev): long  // prev + interval", "+ isRecurring(): boolean  // true"])
cells += box
cells.append(edge(onetime_id, trigger_id, "realize", exitX="0.5", exitY="0", entryX="0.25", entryY="1"))
cells.append(edge(fixedrate_id, trigger_id, "realize", exitX="0.5", exitY="0", entryX="0.75", entryY="1"))
cells.append(edge(task_id, trigger_id, "composition", "trigger  1", exitX="0.7", exitY="1", entryX="0.5", entryY="0",
                   ))
row2_bottom = y + max(ht0, ht1, ht2)

y = row2_bottom + 70
cells.append(group_title(COL[0], y, "observer — job outcome notifications (Observer)"))
y += 34
box, listener_id, hl0 = uml_box(COL[0], y, 340, "JobListener", stereotype="interface",
    methods=["+ onJobCompleted(jobId): void", "+ onJobFailed(jobId, error): void"],
    header_fill="#e1d5e7", header_stroke="#9673a6")
cells += box
box, console_id, hl1 = uml_box(COL[1], y, 320, "ConsoleJobListener",
    methods=["+ onJobCompleted(jobId): void  // println", "+ onJobFailed(jobId, error): void  // println"])
cells += box
cells.append(edge(console_id, listener_id, "realize", exitX="0.5", exitY="0", entryX="0.5", entryY="1"))
row3_bottom = y + max(hl0, hl1)

y = row3_bottom + 70
cells.append(group_title(COL[0], y, "repository (in-memory)  +  exceptions"))
y += 34
box, registry_id, hr0 = uml_box(COL[0], y, 360, "JobRegistry",
    attrs=["- tasksByJobId: ConcurrentHashMap<String,ScheduledTask>"],
    methods=["+ register(task): void", "+ contains(jobId): boolean",
              "+ findByJobId(jobId): ScheduledTask", "  // throws JobNotFoundException"],
    header_fill="#d5e8d4", header_stroke="#82b366")
cells += box
cells.append(edge(registry_id, task_id, "aggregation", "0..*", exitX="1", exitY="0.5", entryX="0", entryY="0.8"))

box, rte_id, he0 = uml_box(COL[1], y, 260, "RuntimeException", stereotype="java.lang", dashed=True,
    header_fill="#f5f5f5", header_stroke="#666666")
cells += box
box, jnf_id, he1 = uml_box(COL[2], y, 300, "JobNotFoundException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
box, dup_id, he2 = uml_box(COL[3], y, 300, "DuplicateJobIdException",
    header_fill="#f8cecc", header_stroke="#b85450")
cells += box
cells.append(edge(jnf_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.3"))
cells.append(edge(dup_id, rte_id, "inherit", exitX="0", exitY="0.5", entryX="1", entryY="0.7"))
row4_bottom = y + max(hr0, he0, he1, he2)

y = row4_bottom + 80
cells.append(group_title(COL[0], y, "services — the ONE public entry point"))
y += 34
box, svc_id, hsv = uml_box(COL[0], y, 620, "JobSchedulerService",
    attrs=["- delayQueue: DelayQueue<ScheduledTask>", "- registry: JobRegistry",
           "- workerPool: ExecutorService", "- dispatcherThread: Thread",
           "- listeners: CopyOnWriteArrayList<JobListener>", "- running: volatile boolean"],
    methods=["+ JobSchedulerService(workerThreads)", "+ scheduleJob(jobId, job, trigger): void",
              "+ cancelJob(jobId): boolean", "+ getJobStatus(jobId): JobStatus",
              "+ addListener(listener): void", "+ shutdown(): void",
              "- dispatchLoop(): void  // delayQueue.take() -> submit to workerPool",
              "- runTask(task): void  // execute, notify, reschedule if recurring"],
    header_fill="#dae8fc", header_stroke="#6c8ebf")
cells += box

cells.append(edge(svc_id, task_id, "dependency", "queues / dequeues", exitX="0.15", exitY="0", entryX="0.2", entryY="1"))
cells.append(edge(svc_id, registry_id, "composition", "registry  1", exitX="0.35", exitY="0", entryX="0.5", entryY="1"))
cells.append(edge(svc_id, trigger_id, "dependency", "reads", exitX="0.55", exitY="0", entryX="0.8", entryY="1"))
cells.append(edge(svc_id, listener_id, "dependency", "notifies", exitX="0.75", exitY="0", entryX="0.5", entryY="1"))

PAGE1 = "\n".join(cells)

# ===========================================================================
# PAGE 2: SEQUENCE — SCHEDULE + DISPATCH + EXECUTE (one-time job, happy path)
# ===========================================================================
cells2 = []
xs = {}
for name, x in [(":Main", 100), ("svc: JobSchedulerService", 380), ("registry: JobRegistry", 700),
                 ("queue: DelayQueue", 980), ("dispatcher: Thread", 1260), ("worker: ExecutorService", 1540)]:
    box, xx = lifeline(x, name, bottom=920)
    cells2 += box
    xs[name] = xx

y = 120
cells2.append(msg(xs[":Main"], xs["svc: JobSchedulerService"], y, "scheduleJob(\"job1\", job, new OneTimeTrigger(100))"))
y += 40
cells2.append(selfcall(xs["svc: JobSchedulerService"], y, "executionTime = trigger.firstExecutionTime(now)", loop_w=130, loop_h=22))
y += 50
cells2.append(msg(xs["svc: JobSchedulerService"], xs["registry: JobRegistry"], y, "register(task)   // ConcurrentHashMap.put(jobId, task)"))
y += 44
cells2.append(msg(xs["svc: JobSchedulerService"], xs["queue: DelayQueue"], y, "put(task)"))
y += 44
cells2.append(msg(xs["svc: JobSchedulerService"], xs[":Main"], y, "return", kind="return"))
y += 60
cells2.append(note(xs["dispatcher: Thread"] - 80, y, 340, "dispatcher thread was already blocked\ninside queue.take() before this call"))
y += 80
cells2.append(msg(xs["queue: DelayQueue"], xs["dispatcher: Thread"], y, "take() unblocks once 100ms elapse -> returns task", kind="return"))
y += 44
cells2.append(frame(xs["dispatcher: Thread"] - 60, xs["dispatcher: Thread"] - xs["queue: DelayQueue"] + 260, y, 70,
                     "alt  [task.getStatus() == CANCELLED]"))
cells2.append(selfcall(xs["dispatcher: Thread"], y + 24, "skip, loop back to take()", loop_w=100, loop_h=20))
cells2.append(divider(xs["dispatcher: Thread"] - 60, xs["dispatcher: Thread"] - xs["queue: DelayQueue"] + 260, y + 46, "[else: still pending]"))
y += 90
cells2.append(selfcall(xs["dispatcher: Thread"], y, "task.markRunning()", loop_w=90, loop_h=20))
y += 50
cells2.append(msg(xs["dispatcher: Thread"], xs["worker: ExecutorService"], y, "submit(() -> runTask(task))"))
y += 44
cells2.append(msg(xs["dispatcher: Thread"], xs["queue: DelayQueue"], y, "take()   // dispatcher loops immediately, doesn't wait for the job to finish"))
y += 60
cells2.append(selfcall(xs["worker: ExecutorService"], y, "job.execute()  ->  markCompleted()  ->  notifyCompleted(jobId)", loop_w=160, loop_h=22))

PAGE2 = "\n".join(cells2)

# ===========================================================================
# PAGE 3: SEQUENCE — RECURRING JOB: EXECUTE THEN RESCHEDULE, THEN CANCEL
# ===========================================================================
cells3 = []
xs = {}
for name, x in [(":Main", 100), ("svc: JobSchedulerService", 380), ("task: ScheduledTask", 700),
                 ("trigger: FixedRateTrigger", 1000), ("registry: JobRegistry", 1300), ("queue: DelayQueue", 1580)]:
    box, xx = lifeline(x, name, bottom=980)
    cells3 += box
    xs[name] = xx

y = 120
cells3.append(note(xs[":Main"] - 60, y, 340, "worker thread just finished executing\nthe due occurrence of a recurring job"))
y += 70
cells3.append(msg(xs["svc: JobSchedulerService"], xs["task: ScheduledTask"], y, "getTrigger().isRecurring()"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs["task: ScheduledTask"], y, "isCancelRequested()"))
y += 44
cells3.append(frame(xs["svc: JobSchedulerService"] - 60, xs["queue: DelayQueue"] - xs["svc: JobSchedulerService"] + 160, y, 190,
                     "alt  [recurring AND NOT cancelRequested]"))
y += 30
cells3.append(msg(xs["svc: JobSchedulerService"], xs["trigger: FixedRateTrigger"], y, "nextExecutionTime(task.executionTimeMillis)"))
y += 40
cells3.append(msg(xs["trigger: FixedRateTrigger"], xs["svc: JobSchedulerService"], y, "return previous + intervalMillis", kind="return"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs["task: ScheduledTask"], y, "withNextExecution(nextTime)   // NEW ScheduledTask, old one is done"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs["registry: JobRegistry"], y, "register(nextTask)   // overwrites jobId -> old task"))
y += 44
cells3.append(msg(xs["svc: JobSchedulerService"], xs["queue: DelayQueue"], y, "put(nextTask)"))
y += 40
cells3.append(divider(xs["svc: JobSchedulerService"] - 60, xs["queue: DelayQueue"] - xs["svc: JobSchedulerService"] + 160, y, "[else: one-time, or cancelled -- do nothing, task stays terminal]"))
y += 70

y += 40
cells3.append(note(xs[":Main"] - 60, y, 340, "later: caller cancels the job by id\nwhile the NEXT occurrence is still pending"))
y += 70
cells3.append(msg(xs[":Main"], xs["svc: JobSchedulerService"], y, "cancelJob(\"job4\")"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs["registry: JobRegistry"], y, "findByJobId(\"job4\")   // O(1) lookup, returns nextTask"))
y += 44
cells3.append(msg(xs["registry: JobRegistry"], xs["svc: JobSchedulerService"], y, "return nextTask", kind="return"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs["task: ScheduledTask"], y, "cancel()   // cancelRequested=true; CAS SCHEDULED->CANCELLED"))
y += 44
cells3.append(msg(xs["task: ScheduledTask"], xs["svc: JobSchedulerService"], y, "return true   // it hadn't started running yet", kind="return"))
y += 40
cells3.append(msg(xs["svc: JobSchedulerService"], xs[":Main"], y, "return true", kind="return"))
y += 60
cells3.append(note(xs["queue: DelayQueue"] - 140, y, 340, "when this task's delay eventually expires,\ndispatcher's take() returns it, sees status\n== CANCELLED, and skips it -- no reschedule"))

PAGE3 = "\n".join(cells3)

# ===========================================================================
outpath = os.path.join(os.path.dirname(__file__), "job-scheduler.drawio")
write_mxfile([
    page("classDiagram", "1 - Class Diagram", PAGE1, w=1900, h=1900),
    page("seqSchedule", "2 - Sequence - Schedule + Dispatch + Execute", PAGE2, w=1900, h=1020),
    page("seqRecurCancel", "3 - Sequence - Recurring Reschedule + Cancel", PAGE3, w=1900, h=1080),
], outpath)
validate(outpath)
print("wrote", outpath)
