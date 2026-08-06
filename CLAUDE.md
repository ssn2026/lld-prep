# LLD Interview Prep — Operating Rules

This repo is used to practice Low-Level Design (LLD) interview questions.
Cadence: 2–3 sessions/week. Every session runs in one of two modes below —
if the user doesn't specify a mode, ask which one before doing anything.

## Public API Convention

Every problem must have exactly one primary service class named
`<ProblemName>Service.java` (e.g. `ParkingLotService.java`) inside `services/`.
This class is the single public-facing entry point — think of the whole
design as a library, and this service class as its public API surface.

- All user-facing operations (e.g. `parkVehicle()`, `unparkVehicle()`,
  `generateTicket()`, `processPayment()`) must be exposed as methods on
  this class.
- Internal classes (model, strategy, factory, observer, repository, etc.)
  should generally not be called directly by outside code — they're
  implementation details the service class orchestrates.
- If a design naturally needs a second service class, ask the user
  before introducing one — the default is one service class per problem.

## Project Structure

Each problem lives in its own top-level folder (e.g. `parking-lot/`, `splitwise/`),
with Java source organized as:

```
<problem-name>/
  src/
    model/       # domain entities
    strategy/    # strategy pattern implementations (if used)
    services/    # business logic / orchestration
    exceptions/  # custom exceptions
    factory/     # factory pattern implementations (if used)
    observer/    # observer pattern implementations (if used)
    repository/  # in-memory data storage/access (if used)
  test/
    input/       # test input files
    output/      # expected/actual output files
  diagrams/
    <problem-name>.drawio  # UML class diagram + sequence diagram(s), see below
    generate.py             # script that produced it, kept for future regeneration
  README.md      # short problem description + patterns used; starts with a
                  # "Mode: Learning" or "Mode: Interview" banner (see Git
                  # Conventions below) so design ownership is clear at a glance
```

Only create the folders actually needed for a given problem — don't scaffold
empty unused folders.

Language: **Java only**. No other languages unless explicitly asked.

## Diagram Generation (both modes, after implementation)

Every problem gets a `.drawio` file with a UML class diagram + at least one
sequence diagram, generated via the shared module at
`docs/tooling/drawio_uml.py` — **do not** hand-write mxGraph XML or re-derive
its escaping/geometry logic inline; that module already exists and is
tested specifically to avoid burning tokens on that per problem.

Workflow:
1. Write a short `<problem-name>/diagrams/generate.py` that imports
   `docs/tooling/drawio_uml.py` and supplies only the data: class
   attrs/methods (pulled from the real source, not invented), edges, and
   sequence messages. Use an existing `diagrams/generate.py` (e.g.
   `parking-lot/diagrams/generate.py`) as the template — copy its structure,
   don't redesign it.
2. Run it, then call `drawio_uml.validate(path)` (already wired into the
   template's last line) to confirm well-formed XML with no dangling edge
   references before reporting the diagram as done.
3. Keep the generator script itself in the repo (not just its output) so the
   diagram can be regenerated after a design change instead of hand-edited.
4. If `docs/tooling/drawio_uml.py` is genuinely missing a primitive you need
   (e.g. a new UML relationship type), add it to the shared module rather
   than duplicating one-off XML in the per-problem script.

## Mode: Interview

Triggered by: `/interview` or explicit request for "interview mode."

The user has already:
1. Written a happy-flow (e.g. `Car enters → generate ticket → park → exit → payment`)
2. Derived classes from that flow
3. Finalized a class design in draw.io with 2–3 design patterns

The user will describe or attach this design. Rules:
- **Do not modify the design.** Implement classes, methods, and relationships
  exactly as given. If something in the design won't compile or is ambiguous,
  ask before changing structure — don't silently "fix" it by redesigning.
- Implementation must actually run.
- After implementation, generate realistic test input files under `test/input/`,
  run the program against them, and save/validate output under `test/output/`.
- Generate at least a sequence diagram for the implemented flow (see
  "Diagram Generation" below) — the class diagram already exists as the
  user's draw.io design, so don't regenerate that one, just the runtime view.
- Report clearly which design patterns were implemented and where.
- Do not commit/push without explicit confirmation from the user.

## Mode: Learning

Triggered by: `/learning` or explicit request for "learning mode."

Claude owns the full process for a given problem:
1. Design the happy flow
2. Derive and design classes (choose 2–3 fitting design patterns)
3. Implement in Java following the Project Structure above
4. Generate test inputs, run, and validate output
5. Generate the UML class diagram + sequence diagram(s) (see
   "Diagram Generation" below)
6. Explain everything simply:
   - Walk the flow against the actual code, referencing file/class names
   - Explain *why* each design pattern was chosen, not just what it is
   - Suggest specific breakpoints/lines to inspect while stepping through
   - Keep explanations concrete and grounded in the code just written, not generic

## Testing & Validation (both modes)

- Every implementation needs at least one runnable test input/output pair.
- Show the actual run output, don't just claim it works.
- Flag any edge cases the current design doesn't handle (without fixing them
  unless asked — especially in Interview Mode).

## Git Conventions

- Never push without explicit user approval.
- Commit messages: `<problem-name>: <short description>` e.g.
  `parking-lot: implement strategy pattern for pricing`
- One commit per meaningful milestone (design implemented, tests added, bug fixed)
  rather than one giant commit per problem.
- **Design-ownership marker**: every commit message gets a mode tag suffix —
  `[learning-mode]` if Claude authored the design (Mode: Learning), or
  `[interview-mode]` if the user's own draw.io design was implemented as-is
  (Mode: Interview). E.g. `parking-lot: implement singleton/factory/strategy
  parking lot design [learning-mode]`. This lets `git log` distinguish whose
  design decisions are whose. The matching problem's `README.md` gets the
  same marker as a one-line banner at the top (see Project Structure above)
  so it's visible without checking git history.

## Reference

See `docs/TRACKER.md` for the full list of design patterns and problems being
tracked, and their completion status.
