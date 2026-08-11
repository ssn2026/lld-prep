# TODO List

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A task tracker: add tasks with a priority and due date, move them through a
lifecycle (`TODO -> IN_PROGRESS -> DONE -> ARCHIVED`, with a reopen path
back from `DONE`), and list them sorted by whichever criterion you want.

## Happy flow

1. `TodoListService.addTask(title, description, priority, dueInDays)`
   creates a `Task` (starts in `TODO`) and stores it.
2. `startTask()`/`completeTask()`/`reopenTask()`/`archiveTask()` each ask
   the task's own current `TaskState` whether that move is legal; the state
   either returns the next state or throws
   `InvalidTaskTransitionException`.
3. Every successful transition notifies registered `TaskListener`s with
   the before/after status.
4. `listTasks(sortStrategy)` returns every task ordered however the caller
   wants (due date / priority / creation order) without the service itself
   knowing how to compare tasks.

## Design patterns used

- **State** — `state/TaskState.java` (interface with throwing defaults) plus
  `TodoState`/`InProgressState`/`DoneState`/`ArchivedState` singletons. The
  interesting contrast with the ATM's `AtmState`: the ATM holds **one**
  state on the single `AtmService` (one physical machine), while here
  **every `Task` holds its own** `TaskState` reference — many independent
  lifecycles sharing the same small set of stateless singleton state
  objects. `TodoListService` never branches on `TaskStatus` itself; it just
  calls `task.start()`/`complete()`/etc. and lets the task's current state
  decide what's legal.
- **Strategy** — `strategy/TaskSortStrategy.java` with `DueDateSortStrategy`,
  `PrioritySortStrategy`, `CreatedOrderSortStrategy`. `listTasks()` takes
  whichever strategy the caller picks per call, so a new ordering (e.g. by
  title) is one new class, not a new branch inside the service.
- **Observer** — `observer/TaskListener.java` with `ConsoleTaskListener`
  shipped as the default implementation. `TodoListService.transition()`
  diffs the status before/after every mutation and only notifies listeners
  when it actually changed, so callers never see a spurious "changed to the
  same status" event.

## Structure

```
todo-list/
  src/
    model/       Task, TaskStatus, TaskPriority
    state/       TaskState + Todo/InProgress/Done/ArchivedState
    strategy/    TaskSortStrategy + DueDate/Priority/CreatedOrderSortStrategy
    observer/    TaskListener, ConsoleTaskListener
    repository/  TaskRepository (in-memory, insertion-ordered)
    exceptions/  TaskNotFoundException, InvalidTaskTransitionException
    services/    TodoListService (the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Full lifecycle incl. quick-complete, reopen, illegal
                          transitions, delete, and an unknown-id error
    output/output.txt    Captured run transcript
  diagrams/
    generate.py       Data-only script that builds todo-list.drawio
    todo-list.drawio  Class diagram + 2 sequence diagrams (happy-path
                       complete, illegal transition on a terminal task)
  explainer/index.html   Interactive step-through: add a task, watch it land on the
                          board, then Start/Complete/Reopen/Archive any task (legal
                          or not) and watch the real State transition play out
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `todo-list/` folder
itself as the workspace root, then use the "Run Main (scenario.txt)" config.

## Known gaps (flagged, not fixed)

- No persistence — all state is in-memory and lost on process exit.
- No concurrency control — `TodoListService` isn't thread-safe (unlike
  `job-scheduler/`, which is built specifically around concurrent access).
- `dueInDays` is a plain `int` offset, not a real calendar date/time.
- Deletion is unconditional — an `ARCHIVED` task and a `TODO` task can both
  be deleted the same way; a real system might want to restrict deleting
  active work.
- No sub-tasks, tags, or assignees — a single flat list of tasks.
