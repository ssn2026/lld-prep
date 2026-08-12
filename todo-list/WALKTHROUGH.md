# TODO List — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

You keep a list of tasks. Each task has a title, a priority, and a number
of days until it's due. A task starts life in a `TODO` box. You can move
it to `IN_PROGRESS`, then to `DONE` — or straight from `TODO` to `DONE` if
you just want to check it off without ever marking it "in progress". If
you accidentally marked something done, you can `REOPEN` it back to
`TODO`. Once a task is `DONE`, you can `ARCHIVE` it, which is a one-way
door — an archived task can't be touched again. At any point you can list
every task sorted by due date, by priority, or by the order you created
them. That's the whole system: add a task, move it through a small set of
legal states, list them however you like.

---

## 2. The one door you're allowed to knock on

`src/services/TodoListService.java` is the **only** class anything outside
the package is meant to call. Everything else (`model`, `state`,
`strategy`, `observer`, `repository`, `exceptions`) is a helper this one
class uses internally.

| Method | What it does |
|---|---|
| `addTask(title, description, priority, dueInDays)` | Create a new task, starts in `TODO` |
| `startTask(taskId)` | Move a task to `IN_PROGRESS` |
| `completeTask(taskId)` | Move a task to `DONE` |
| `reopenTask(taskId)` | Move a `DONE` task back to `TODO` |
| `archiveTask(taskId)` | Move a `DONE` task to `ARCHIVED` (final) |
| `deleteTask(taskId)` | Remove a task entirely, any status |
| `getTask(taskId)` | Look up one task |
| `listTasks(sortStrategy)` | Get every task, ordered however you ask |
| `addListener(listener)` | Get notified whenever any task's status changes |

---

## 3. Read the code in this order

### Step 1 — the building blocks (`src/model/`)

Start here. `Task.java` is the central object. Look closely at its
constructor:

```java
public Task(String id, String title, String description, TaskPriority priority, int dueInDays, int createdOrder) {
    ...
    this.state = TodoState.INSTANCE;
}
```

Every task is born in `TodoState`. The `state` field is **not** a plain
enum field like `private TaskStatus status` — it's a reference to an
object (`TaskState`) that knows what's legal to do next. That's the whole
idea behind the design pattern this problem is built around (explained in
Step 2). `Task` itself has no `if` statements about status at all — look
at its four action methods:

```java
public void start()    { state = state.start(this); }
public void complete()  { state = state.complete(this); }
public void reopen()   { state = state.reopen(this); }
public void archive()   { state = state.archive(this); }
```

Each one just asks the *current* state object "what happens if I try to
do this?" and stores whatever comes back as the new state. `Task` never
has to know the rules itself.

`TaskStatus.java` and `TaskPriority.java` are plain enums — no logic, just
the set of allowed values (`TODO`/`IN_PROGRESS`/`DONE`/`ARCHIVED` and
`LOW`/`MEDIUM`/`HIGH`).

### Step 2 — the rulebook for what a task can do next (`src/state/`)

This is the **State** design pattern, and it's the most important idea in
this codebase, so let's slow down.

**The problem it solves:** a task's legal moves depend on where it
currently is. You can `start()` a `TODO` task, but not a `DONE` one. You
can `archive()` a `DONE` task, but not a `TODO` one. Without this pattern,
you'd end up with one big method full of `if (status == TODO) { ... }
else if (status == IN_PROGRESS) { ... }` — and that method would need to
grow every time you added a new status or a new action. The State pattern
avoids that by giving *each status its own small class* that only
implements the moves that are legal from there.

Look at `TaskState.java`, the interface every status class implements:

```java
public interface TaskState {
    TaskStatus getStatus();

    default TaskState start(Task task) {
        throw new InvalidTaskTransitionException(getStatus(), "start");
    }
    default TaskState complete(Task task) { throw ...; }
    default TaskState reopen(Task task) { throw ...; }
    default TaskState archive(Task task) { throw ...; }
}
```

Every method has a **default implementation that just throws an
exception**. That's the trick: instead of writing "reopen is illegal from
TODO" as a special case, you write nothing at all, and the interface's
default already throws for you. A concrete state class only needs to
*override* the moves it actually allows. Look at `TodoState.java` in
full:

```java
public class TodoState implements TaskState {
    public static final TodoState INSTANCE = new TodoState();
    private TodoState() {}

    public TaskStatus getStatus() { return TaskStatus.TODO; }
    public TaskState start(Task task) { return InProgressState.INSTANCE; }
    public TaskState complete(Task task) { return DoneState.INSTANCE; }
}
```

It only overrides `start()` and `complete()` — those are the two legal
moves from `TODO`. It does *not* override `reopen()` or `archive()`, so
calling either of those on a `TODO` task falls through to the interface's
default and throws `InvalidTaskTransitionException`. You never had to
write "you can't reopen a TODO task" anywhere — it's true by omission.

A few more details worth noticing:
- **`private TodoState() {}` plus a `public static final INSTANCE`** —
  this is the Singleton pattern nested inside the State pattern. There's
  never a reason to have two `TodoState` objects; they hold no per-task
  data, so every `TODO` task in the whole program shares the exact same
  `TodoState.INSTANCE`. Same for `InProgressState`, `DoneState`, and
  `ArchivedState`.
- **`InProgressState.java`** only overrides `complete()` (→
  `DoneState.INSTANCE`). Notice there's no `start()` override — you can't
  "start" a task that's already in progress, and that's enforced for free
  by the same throwing default.
- **`DoneState.java`** overrides `reopen()` (→ back to `TodoState.INSTANCE`)
  and `archive()` (→ `ArchivedState.INSTANCE`). This is the one state with
  two legal exits.
- **`ArchivedState.java`** overrides nothing at all. Every single method
  call on an archived task's state falls through to a throwing default.
  That's what "terminal state" means in code: a class that adds no new
  behavior on purpose.

The code comment inside `TaskState.java` points out something worth
re-reading once you've also looked at other problems in this repo: the
ATM's `AtmState` is held **once**, directly on the single `AtmService`
(because there's only one physical ATM machine — one state for the whole
system). Here, State is used completely differently: **every individual
`Task` object holds its own state reference.** There isn't "the current
state of the todo list" — there are potentially hundreds of tasks, each
independently `TODO`, `IN_PROGRESS`, `DONE`, or `ARCHIVED` at the same
time. Same pattern, opposite scope: one shared machine vs. many
independent little machines.

### Step 3 — how the list gets sorted (`src/strategy/`)

`TaskSortStrategy.java` is a one-method interface:

```java
public interface TaskSortStrategy {
    List<Task> sort(List<Task> tasks);
}
```

Three classes implement it, and each is a couple of lines because Java's
`Stream` API does the heavy lifting:

- `DueDateSortStrategy` — `Comparator.comparingInt(Task::getDueInDays)`,
  soonest due date first.
- `PrioritySortStrategy` — has a small private `rank()` helper that maps
  `HIGH → 0`, `MEDIUM → 1`, `LOW → 2`, then sorts by that rank, so `HIGH`
  tasks come first. (Plain alphabetical order would put `HIGH` before
  `LOW` before `MEDIUM`, which is *not* what you want — that's exactly why
  this needs its own tiny mapping instead of just sorting the enum
  directly.)
- `CreatedOrderSortStrategy` — sorts by `Task::getCreatedOrder`, an `int`
  that's simply assigned in increasing order every time a task is created
  (see `TodoListService.addTask()` below). This is what "insertion order"
  means when you want it to survive being handed a re-shuffled list.

This is the **Strategy** pattern: `TodoListService.listTasks()` doesn't
contain any sorting logic itself — it just takes whichever strategy object
the caller hands it and calls `.sort(...)` on it. Adding a fourth sort
order (say, alphabetical by title) means writing one new small class; it
never means touching the service.

### Step 4 — who gets told when something changes (`src/observer/`)

```java
public interface TaskListener {
    void onStatusChanged(String taskId, TaskStatus from, TaskStatus to);
}
```

One method, and `ConsoleTaskListener` is the only implementation shipped
— it just prints the change. This is the **Observer** pattern: the
service doesn't know or care who's listening, or how many listeners there
are. It just loops over a list and calls `onStatusChanged` on each one
(see `TodoListService.notifyStatusChanged()` below). `Main.java` actually
registers a *second*, different listener alongside the console one (more
on that in Step 7) — proof that you can plug in as many independent
listeners as you want without changing the service at all.

### Step 5 — where tasks are stored (`src/repository/TaskRepository.java`)

A thin wrapper around a `LinkedHashMap<String, Task>` — nothing fancy,
just:
- `save(task)` — put it in the map.
- `findById(taskId)` — get it back, or throw `TaskNotFoundException` if
  the id doesn't exist.
- `delete(taskId)` — remove it, or throw the same exception if it was
  never there.
- `findAll()` — return every task as a fresh `ArrayList` (a defensive
  copy, so callers can't accidentally mutate the repository's internal
  map).

`LinkedHashMap` specifically (not a plain `HashMap`) is what makes
`CreatedOrderSortStrategy` meaningful — a `LinkedHashMap` remembers the
order things were inserted in, so `findAll()` naturally comes back in
creation order even before any sorting happens.

### Step 6 — errors (`src/exceptions/`)

Two exception classes, each doing one job:
- `TaskNotFoundException` — thrown by the repository when a task id
  doesn't exist.
- `InvalidTaskTransitionException` — thrown by `TaskState`'s default
  methods (see Step 2) when you try an illegal move. Its constructor takes
  the *current* status and the *attempted* action, so the message reads
  naturally: `"Cannot reopen a task that is TODO"`.

### Step 7 — the orchestrator (`src/services/TodoListService.java`)

Now that you've seen every piece, this class just wires them together.
The one part worth reading closely is the private `transition()` helper,
because all four action methods (`startTask`, `completeTask`,
`reopenTask`, `archiveTask`) are one-line calls into it:

```java
public void startTask(String taskId)   { transition(taskId, Task::start); }
public void completeTask(String taskId) { transition(taskId, Task::complete); }
public void reopenTask(String taskId)  { transition(taskId, Task::reopen); }
public void archiveTask(String taskId)  { transition(taskId, Task::archive); }

private void transition(String taskId, Consumer<Task> mutation) {
    Task task = repository.findById(taskId);
    TaskStatus before = task.getStatus();
    mutation.accept(task);
    TaskStatus after = task.getStatus();
    if (before != after) {
        notifyStatusChanged(taskId, before, after);
    }
}
```

`Task::start` here is a **method reference** — a compact way of passing
"the `start()` method itself" as a value, without calling it yet.
`transition()` receives it as a `Consumer<Task>` (a functional interface
meaning "something that takes a `Task` and does something with it,
returning nothing") and calls `mutation.accept(task)`, which is exactly
the same as calling `task.start()`. This is why there's only *one* copy
of the "look up the task, remember its status before, do the thing,
compare status after, maybe notify" logic instead of four near-identical
copies.

Notice the `if (before != after)` guard: if `mutation.accept(task)` had
thrown (an illegal transition), execution never reaches this line at all
— the exception propagates straight out of `transition()` and out of
`startTask()`/etc. And if it succeeds but *doesn't* change the status,
there'd be nothing to guard against anyway (every allowed transition in
this design always changes status), but keeping the check honest means a
future transition that *could* be a no-op would never fire a spurious
"changed to the same thing" notification.

`addTask()` is worth a quick look too:

```java
public String addTask(String title, String description, TaskPriority priority, int dueInDays) {
    String taskId = "T" + idSeq.incrementAndGet();
    Task task = new Task(taskId, title, description, priority, dueInDays, createdOrderSeq.incrementAndGet());
    repository.save(task);
    return taskId;
}
```

`idSeq` and `createdOrderSeq` are `AtomicInteger`s. There's no real
concurrency happening in this problem (nothing here runs on multiple
threads), but `AtomicInteger.incrementAndGet()` is just a convenient,
already-thread-safe way to say "give me a fresh number, one higher than
last time" — the first task becomes `T1` with `createdOrder=1`, the
second `T2` with `createdOrder=2`, and so on.

### Step 8 — the runner (`src/Main.java`)

Not part of the "real" system — a test harness. It reads a text file
line by line (`test/input/scenario.txt`), turns each line into a call on
`TodoListService`, and writes a transcript to
`test/output/output.txt`. Look at how it registers listeners:

```java
service.addListener(new ConsoleTaskListener());
service.addListener(new TranscriptTaskListener(output));
```

`ConsoleTaskListener` is the "real" shipped implementation from
`observer/`. `TranscriptTaskListener` is a small private class defined
*inside* `Main.java` itself — it exists purely so the listener's output
also lands in the saved transcript file, not just the terminal. Both are
registered at once, and both fire on every single status change, proving
the Observer pattern really does support multiple independent listeners
with zero coupling between them.

---

## 4. Picture of one full flow: add → start → complete → archive

```
Main.java (reads "ADD BuyGroceries MEDIUM 2")
   |
   v
TodoListService.addTask("BuyGroceries", "BuyGroceries", MEDIUM, 2)
   |  taskId = "T1"  (idSeq was 0, now 1)
   |  new Task("T1", ..., createdOrder=1)   <- Task's constructor sets state = TodoState.INSTANCE
   |  repository.save(task)
   v
returns "T1"
Main.java prints: "OK added T1 (BuyGroceries, MEDIUM, due in 2d)"


... later ...

Main.java (reads "START T1")
   |
   v
TodoListService.startTask("T1")
   |
   v
transition("T1", Task::start)
   |  task = repository.findById("T1")
   |  before = task.getStatus()          -> TODO
   |  task.start()  ->  state = state.start(this)
   |       TodoState.start(task) returns InProgressState.INSTANCE
   |  after = task.getStatus()           -> IN_PROGRESS
   |  before != after  ->  notifyStatusChanged("T1", TODO, IN_PROGRESS)
   v
every registered TaskListener.onStatusChanged("T1", TODO, IN_PROGRESS) fires
Main.java prints: "OK T1 -> IN_PROGRESS"


... COMPLETE T1 and ARCHIVE T1 follow the exact same shape, just calling
    task.complete() (InProgressState -> DoneState) and then
    task.archive() (DoneState -> ArchivedState) instead of task.start() ...
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

A few real lines from the run, annotated:

```
> START T1
  [listener] T1: TODO -> IN_PROGRESS
OK T1 -> IN_PROGRESS
```

Notice the order: the `[listener]` line appears **before** the `OK`
line. That's not a coincidence — look back at `Main.java`'s loop: it
prints `"> " + line` first, *then* calls `execute(service, line)`, and
the listener fires synchronously *inside* that `execute()` call (because
`notifyStatusChanged` is called directly from `transition()`, on the same
thread, before `startTask()` even returns). So by the time
`execute()` hands back the `"OK T1 -> IN_PROGRESS"` string for `Main` to
print, the listener has already run and already printed its own line.

```
> COMPLETE T2
  [listener] T2: TODO -> DONE
OK T2 -> DONE
```

This is task `T2` (`FileTaxes`) going straight from `TODO` to `DONE`,
skipping `IN_PROGRESS` entirely — proof that `TodoState.complete()`
really does allow a direct jump, exactly as coded in Step 2.

```
> REOPEN T2
  [listener] T2: DONE -> TODO
OK T2 -> TODO
```

The reopen path: `DoneState.reopen()` sends it back to
`TodoState.INSTANCE`. `T2` is now, once again, a completely ordinary
`TODO` task — you can see two lines later it gets `START`ed again just
like a brand new task could.

```
> START T1
ERROR InvalidTaskTransitionException: Cannot start a task that is ARCHIVED
```

`T1` was archived earlier in the script. This line is
`ArchivedState`'s throwing default catching a `start()` call — remember,
`ArchivedState` overrides *nothing*, so every action on it throws with a
message built from `InvalidTaskTransitionException`'s constructor
(`currentStatus`, `"start"`).

```
> REOPEN T3
ERROR InvalidTaskTransitionException: Cannot reopen a task that is TODO
> ARCHIVE T3
ERROR InvalidTaskTransitionException: Cannot archive a task that is TODO
```

`T3` (`ReadBook`) is still sitting in plain `TODO` at this point in the
script. `TodoState` only overrides `start()` and `complete()`, so both
`reopen()` and `archive()` correctly fall through and throw.

```
> START ghost-task
ERROR TaskNotFoundException: No task found with id: ghost-task
```

A completely different failure path — this one never even reaches the
`TaskState` logic. `repository.findById("ghost-task")` fails first, since
that id was never `save()`d.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run:
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Try every illegal transition on purpose.** Add a task, then
   immediately try `REOPEN` and `ARCHIVE` on it while it's still `TODO`
   (both should fail), then `START` it, then try `START` again on the
   now-`IN_PROGRESS` task (should also fail — `InProgressState` never
   overrides `start()`).
2. **Delete a task and then try to act on it.** `DELETE T1` then
   `START T1` — expect `TaskNotFoundException`, proving delete really
   does remove the task from the repository rather than just marking it.
3. **Watch `LIST PRIORITY` reorder itself as tasks change status.**
   Sorting by priority doesn't care about status at all — a `DONE` `HIGH`
   task still sorts before an `ACTIVE` `LOW` task. Confirm this by adding
   tasks of mixed priority and status and checking the `LIST PRIORITY`
   output order.
4. **Add a task, reuse its id logic mentally, delete it, add another.**
   Because `idSeq` only ever increments and is never reused, the new
   task will get the next number up (e.g. `T4`), never reusing `T1` even
   though `T1` might have been deleted. Confirm this in the output.
5. **Remove one of the two `addListener(...)` calls in `Main.java`** and
   re-run. You should see exactly one `[listener]` line per status
   change instead of the current single line (there's only one console
   listener printing to begin with — try adding a *third* listener
   instead, e.g. another `ConsoleTaskListener`, and confirm you now get
   two nearly-identical `[listener]` lines per change, proving multiple
   independent observers really do all fire).
