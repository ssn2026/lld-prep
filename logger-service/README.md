# Logger Service

> **Mode: Learning** — this design (happy flow, class breakdown, pattern
> choices) was authored by Claude, not the user. See `CLAUDE.md` → "Mode:
> Learning" for what that implies about ownership of the decisions below.

A minimal logging library: one process-wide logger, a configurable minimum
severity level, a swappable line format, and fan-out to any number of
output destinations (console, file, ...).

## Happy flow

1. Callers get the one shared logger via `LoggerService.getInstance()`.
2. `setMinLevel(...)` configures the severity floor; `setFormatter(...)`
   configures how a `LogEvent` becomes one line of text; `addAppender(...)`
   registers a destination for that text.
3. `log(level, message)` (or the `debug()`/`info()`/`warn()`/`error()`
   shortcuts) first checks the level against the floor — anything below is
   dropped before it's even formatted. Anything at-or-above gets wrapped in
   a `LogEvent`, formatted once, and handed to every registered appender.

## Design patterns used

- **Singleton** — `services/LoggerService.java` has a private constructor
  and a single static `INSTANCE`. This is the one problem in the repo
  where the *service itself* enforces there's only one instance, rather
  than the caller choosing to create one (contrast with every other
  problem's `services/` class, which callers `new` up themselves) — a
  deliberate, textbook use: a logger is inherently process-wide state, not
  something you'd want two independent copies of silently splitting output.
- **Strategy** — `strategy/LogFormatter.java` with `PlainTextFormatter` and
  `JsonFormatter`. How a `LogEvent` renders to text is fully decoupled from
  both the event itself and from `LoggerService`, so switching formats
  mid-run (see the test scenario) touches nothing but the formatter field.
- **Observer** — `observer/LogAppender.java` with `ConsoleLogAppender` and
  `FileLogAppender` (real file I/O, not a simulated sink).
  `LoggerService` doesn't know or care how many appenders are registered
  or what they do with a line — it just fans the same formatted string out
  to all of them.

## Structure

```
logger-service/
  src/
    model/       LogLevel, LogEvent
    strategy/    LogFormatter + PlainTextFormatter, JsonFormatter
    observer/    LogAppender + ConsoleLogAppender, FileLogAppender
    exceptions/  InvalidLogLevelException, LogWriteException
    services/    LoggerService (Singleton, the only public entry point)
    Main.java    Reads a command script, drives the service, writes a transcript
  test/
    input/scenario.txt   Level filtering, both appenders, a formatter swap
                          mid-run, a DUMPFILE readback proving real bytes
                          landed on disk, and both error paths
    output/output.txt    Captured run transcript
  diagrams/
    generate.py            Data-only script that builds logger-service.drawio
    logger-service.drawio  Class diagram + 1 sequence diagram (filter, format, fan-out)
  explainer/index.html   Interactive step-through: configure the min level, log at
                          any severity, and watch the real filter/format/fan-out logic
                          play out against a live console panel and file panel
```

## Running it

```
javac -d out $(find src -name "*.java")
java -cp out Main test/input/scenario.txt test/output/output.txt
```

The test scenario's `FILE` appender path (`test/output/app.log`) is
relative to the process's working directory, so run it (or the
`.vscode/launch.json` config) with the `logger-service/` folder itself as
the working directory — same assumption every problem's structure already
makes for `test/input`/`test/output`.

Requires the VS Code "Extension Pack for Java" (Microsoft) for the
`.vscode/launch.json` run config to work — open the `logger-service/`
folder itself as the workspace root, then use the "Run Main (scenario.txt)"
config.

## Known gaps (flagged, not fixed)

- No log rotation or size limits — `FileLogAppender` appends forever;
  re-running the test scenario without clearing `test/output/app.log`
  first will accumulate duplicate lines across runs.
- `LogEvent` uses a monotonic sequence number instead of a real timestamp,
  purely to keep the captured test transcript byte-for-byte reproducible —
  a real logger would want wall-clock or logical time.
- No structured/contextual fields (request id, thread name, etc.) — a
  `LogEvent` is just a level and a plain string message.
- The Singleton is genuinely global process state: nothing resets it
  between "logical runs" within the same JVM, which is correct for a real
  logger but means the interactive explainer and the Java test harness
  each simulate/hold their own independent state rather than sharing one.
