# Logger Service — Simple Walkthrough

This file explains the whole codebase in plain language, in the order you
should read it. No prior context assumed.

---

## 1. The story in one paragraph

Somewhere in a program, code wants to say "something happened" — at
different severities (`DEBUG`, `INFO`, `WARN`, `ERROR`) — without caring
where that message actually ends up. This logger lets you configure a
minimum severity (anything below it is silently dropped), a text format
(plain text or JSON), and any number of destinations (print to the
console, write to a real file, or both at once). Every single call to
`log()` — no matter which destination is configured — goes through the
exact same three steps: check if it's severe enough to bother with,
format it into one line of text, and hand that line to every destination
that's currently registered.

---

## 2. The one door you're allowed to knock on

`src/services/LoggerService.java` is the **only** class anything outside
the package is meant to call — and it's a little different from every
other problem in this repo, because it's a **Singleton** (explained in
Step 5). You never construct it with `new`; you always ask for the one
shared instance.

| Method | What it does |
|---|---|
| `getInstance()` | The one and only way to get a `LoggerService` (static method) |
| `setMinLevel(level)` | Anything below this severity gets dropped |
| `setFormatter(formatter)` | Choose how a log line renders as text |
| `addAppender(appender)` | Register one more destination for log lines |
| `log(level, message)` | Log one message at a given severity |
| `debug(message)` / `info(message)` / `warn(message)` / `error(message)` | Shortcuts for `log(LEVEL, message)` |

---

## 3. Read the code in this order

### Step 1 — one log event (`src/model/`)

- **`LogLevel.java`** — a plain 4-value enum: `DEBUG, INFO, WARN, ERROR`,
  declared in that exact order. That order matters a lot — Java enums get
  a free `ordinal()` method that returns their position (`DEBUG` is 0,
  `INFO` is 1, `WARN` is 2, `ERROR` is 3), and this codebase leans on that
  directly to compare severity (see Step 5).
- **`LogEvent.java`** — bundles a sequence number, a level, and a
  message. Read its doc comment: it deliberately uses an incrementing
  `int seq` instead of a real timestamp. The reason given is that this
  keeps the captured test transcript byte-for-byte identical every time
  you rerun it — a real timestamp would be a different value on every
  run, which would make the "known good" `test/output/output.txt` file
  impossible to compare against a fresh run.

### Step 2 — turning an event into one line of text (`src/strategy/`)

```java
public interface LogFormatter {
    String format(LogEvent event);
}
```

Two implementations:

- **`PlainTextFormatter`** — `"[" + seq + "] " + level + " " + message`,
  e.g. `[1] INFO server started on port 8080`.
- **`JsonFormatter`** — builds a JSON object by hand
  (`{"seq":1,"level":"INFO","message":"..."}`), including a small private
  `escape()` helper that backslash-escapes any `\` or `"` characters
  already inside the message, so a message containing a quote can't
  accidentally break the JSON.

This is the **Strategy** pattern: `LoggerService` never has any
`if (format == JSON)` logic anywhere. It just holds one `LogFormatter`
reference and calls `.format(event)` on it — whichever one is currently
plugged in. Adding a third format (say, a syslog-style format) means
writing one more small class implementing the same one method.

### Step 3 — where a formatted line actually goes (`src/observer/`)

```java
public interface LogAppender {
    void append(String formattedLine);
}
```

Two implementations:

- **`ConsoleLogAppender`** — `System.out.println("[console] " +
  formattedLine)`.
- **`FileLogAppender`** — genuinely writes to a real file on disk:

  ```java
  Files.writeString(path, formattedLine + System.lineSeparator(),
          StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  ```

  `CREATE` means "make the file if it doesn't already exist";
  `APPEND` means "add to the end, don't overwrite". If this fails for any
  reason (bad path, permissions, disk full), the checked `IOException`
  gets wrapped and rethrown as `LogWriteException` — a `RuntimeException`,
  so callers of `LoggerService.log()` don't need a `try/catch` just to log
  something.

This is the **Observer** pattern: `LoggerService` holds a *list* of
appenders (not just one), and fans every formatted line out to every
appender currently registered (Step 5). It has no idea how many there
are or what they each do with the line.

### Step 4 — errors (`src/exceptions/`)

- **`InvalidLogLevelException`** — thrown when a level string doesn't
  match any `LogLevel` constant (see `Main.java`'s `parseLevel()` helper
  in Step 6).
- **`LogWriteException`** — thrown by `FileLogAppender` when the
  underlying file write fails, wrapping the original `IOException` as its
  cause (so you don't lose the real reason if you print the stack trace).

### Step 5 — the Singleton itself (`src/services/LoggerService.java`)

This is the one class in the whole repo that's genuinely a **Singleton**,
and it's worth understanding exactly what that means and why it's
different from every other problem here. Look at the top of the class:

```java
public final class LoggerService {
    private static final LoggerService INSTANCE = new LoggerService();

    private LoggerService() {
    }

    public static LoggerService getInstance() {
        return INSTANCE;
    }
    ...
}
```

The constructor is `private` — nothing outside this class can ever call
`new LoggerService()`. The *only* way to get a reference to a
`LoggerService` at all is the static method `getInstance()`, which always
hands back the exact same object (`INSTANCE`, created once, the moment
the class is first loaded). Every part of the program that calls
`LoggerService.getInstance()` gets the identical logger, with the
identical configured appenders/formatter/level.

Contrast this with, say, `todo-list/`'s `TodoListService` — any caller is
free to write `new TodoListService()` and get a brand-new, independent
todo list. A logger genuinely shouldn't work that way: if two different
parts of a real program each created their *own* `LoggerService`, they'd
each have their own separate list of appenders, and you could easily end
up with half your log lines going to one file and half going to another
by accident. Making it a Singleton removes that whole failure mode by
construction — there is only ever one.

Now the actual logging logic, `log()`:

```java
public void log(LogLevel level, String message) {
    if (level.ordinal() < minLevel.ordinal()) {
        return;
    }
    LogEvent event = new LogEvent(++seq, level, message);
    String formatted = formatter.format(event);
    for (LogAppender appender : appenders) {
        appender.append(formatted);
    }
}
```

Read this as four steps:
1. **Filter.** Compare `level.ordinal()` against `minLevel.ordinal()` —
   this is exactly why the enum's declaration order in Step 1 matters:
   `DEBUG.ordinal()` (0) is less than `INFO.ordinal()` (1), so if
   `minLevel` is `INFO`, any `DEBUG` call returns immediately, before an
   event is even created, before anything is formatted, before any
   appender is touched.
2. **Wrap.** Only once a message survives the filter does it become a
   real `LogEvent`, tagging it with the next sequence number
   (`++seq` — pre-increment, so the very first logged message is `seq=1`,
   not `0`).
3. **Format once.** `formatter.format(event)` runs exactly once per
   log call, regardless of how many appenders are registered — the same
   formatted string is reused for all of them, not re-formatted per
   appender.
4. **Fan out.** Loop over every registered `LogAppender` and hand it the
   same formatted line.

`debug()`/`info()`/`warn()`/`error()` are one-line convenience wrappers
around `log(LEVEL, message)` — nothing more.

### Step 6 — the runner (`src/Main.java`)

A test harness reading `test/input/scenario.txt`, driving
`LoggerService.getInstance()`, and writing a transcript to
`test/output/output.txt`. Two things worth knowing if you want to run
this yourself:

- Its `APPENDER FILE <path>` command's path (`test/output/app.log`) is a
  **relative** path, resolved against wherever the `java` process's
  working directory happens to be — this only resolves correctly if you
  run it from inside the `logger-service/` folder itself (which is
  exactly what `.vscode/launch.json`'s `"cwd": "${workspaceFolder}"`
  ensures). Running it from the repo root instead would try to create
  `test/output/app.log` relative to the repo root, which doesn't exist,
  and the run would fail.
- `FileLogAppender` always **appends**, never truncates. If you rerun the
  scenario without first deleting `test/output/app.log`, the file keeps
  growing with duplicate lines from every previous run. The `README.md`'s
  "Known gaps" section flags this explicitly.

---

## 4. Picture of one full flow: a filtered message vs. a logged one

```
Main.java (reads "LOG DEBUG this debug line should be filtered out")
   |  minLevel is currently INFO (set by an earlier "LEVEL INFO" command)
   v
LoggerService.log(DEBUG, "this debug line should be filtered out")
   |  DEBUG.ordinal() (0) < INFO.ordinal() (1)   -> TRUE
   v
return immediately -- no LogEvent created, formatter never called, no appender touched
Main.java prints: "OK logged"   (the caller can't tell from the return value alone
                                  whether the message was actually kept or dropped)


... later ...

Main.java (reads "LOG INFO server started on port 8080")
   |
   v
LoggerService.log(INFO, "server started on port 8080")
   |  INFO.ordinal() (1) < INFO.ordinal() (1)   -> FALSE, proceed
   |  event = new LogEvent(++seq, INFO, "server started on port 8080")   <- seq becomes 1
   |  formatted = formatter.format(event)   <- PlainTextFormatter: "[1] INFO server started on port 8080"
   |  for each appender:
   |       ConsoleLogAppender.append(formatted)   -> prints "[console] [1] INFO server started on port 8080"
   |       FileLogAppender.append(formatted)      -> appends "[1] INFO server started on port 8080\n" to app.log
   v
Main.java prints: "OK logged"
```

---

## 5. Reading the actual captured run (`test/output/output.txt`)

```
> APPENDER CONSOLE
OK console appender added
> APPENDER FILE test/output/app.log
OK file appender added -> test\output\app.log
> LEVEL INFO
OK min level = INFO
> LOG DEBUG this debug line should be filtered out
OK logged
```

Both appenders are registered before anything is logged, and the minimum
level is set to `INFO`. The `DEBUG` line right after prints `"OK logged"`
just like every other successful call — but as traced in Section 4, it
never actually reached either appender. You can't tell from this line
alone that it was filtered; you have to know the code (or check the file)
to confirm it.

```
> LOG INFO server started on port 8080
OK logged
> LOG WARN cache miss rate above 5 percent
OK logged
> LOG ERROR failed to connect to database
OK logged
```

Three messages that *do* pass the filter. Notice this transcript doesn't
show `[console] ...` lines — that's because `ConsoleLogAppender` prints
straight to `System.out` and `Main.java`'s own transcript-writing only
captures what it explicitly writes via its own `log()` helper, not
everything the whole process printed. (If you run this yourself in a
terminal, you'll see the `[console] ...` lines interleaved live — they
just don't get saved into the file.)

```
> FORMAT JSON
OK formatter = JSON
> LOG INFO switched to JSON formatting
OK logged
> LOG ERROR disk usage at 91 percent
OK logged
```

The formatter is swapped **mid-run**, proving the Strategy pattern really
does let you change behavior without restarting anything — the exact
same `LoggerService` instance, the exact same registered appenders, now
producing a completely different line shape for every message logged
from this point on.

```
> DUMPFILE test/output/app.log
DUMPFILE test\output\app.log
  [1] INFO server started on port 8080
  [2] WARN cache miss rate above 5 percent
  [3] ERROR failed to connect to database
  {"seq":4,"level":"INFO","message":"switched to JSON formatting"}
  {"seq":5,"level":"ERROR","message":"disk usage at 91 percent"}
```

This is the proof that `FileLogAppender` genuinely wrote real bytes to a
real file — `DUMPFILE` is a `Main.java` command that reads the file back
off disk and echoes it. Notice the first three lines are in
`PlainTextFormatter`'s shape and the last two are JSON — exactly matching
when `FORMAT JSON` was issued. Also notice the `seq` numbers: they're
**1 through 5**, not 1 through 8 — because the `DEBUG` message never made
it past the filter, `++seq` was never reached for it, so it never
consumed a sequence number at all.

```
> LOG TRACE this level does not exist
ERROR InvalidLogLevelException: Unknown log level: TRACE
> LEVEL VERBOSE
ERROR InvalidLogLevelException: Unknown log level: VERBOSE
```

Both of these come from `Main.java`'s `parseLevel()` helper trying
`LogLevel.valueOf(text)` and catching the resulting
`IllegalArgumentException` to rethrow it as the friendlier
`InvalidLogLevelException` — neither `"TRACE"` nor `"VERBOSE"` is one of
the four real `LogLevel` constants.

---

## 6. Things to try / test yourself

Edit `test/input/scenario.txt` and re-run (from inside `logger-service/`,
per Step 6's note about relative paths):
```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

1. **Delete `test/output/app.log` before rerunning** and compare
   `DUMPFILE`'s output to what's shown above — it should match exactly.
   Then rerun *again without deleting it* and watch the file grow with a
   second, duplicated copy of every line — proof of the "always appends,
   never truncates" gap called out in Step 6.
2. **Set `LEVEL ERROR`** partway through the script and confirm every
   `LOG INFO` / `LOG WARN` call after that point silently returns `"OK
   logged"` while contributing nothing to either appender — check the
   `seq` numbers in a subsequent `DUMPFILE` to confirm they skip exactly
   the filtered ones.
3. **Add a second `APPENDER FILE <different-path>`** and confirm a single
   `LOG` call now writes to *both* files — proof that `LoggerService`
   really does fan out to every registered appender, not just the first
   one.
4. **Try `LEVEL DEBUG`** (the lowest level) and then `LOG DEBUG
   something`. Since `DEBUG.ordinal()` (0) is never less than
   `minLevel.ordinal()` (0) when `minLevel` is also `DEBUG`, this should
   now pass the filter — confirming the filter's `<` comparison (not
   `<=`) means "equal to the minimum" always counts as legal.
5. **Trace what `getInstance()` returning the same object means for
   `Main.java` specifically.** Every command in the script calls
   `LoggerService.getInstance()`'s methods on the identical object — try
   imagining (or actually writing) a second, unrelated class elsewhere in
   the program that also calls `LoggerService.getInstance()` and confirm
   it would see the exact same appenders/formatter/level already
   configured, with no way to get an independent logger instead.
