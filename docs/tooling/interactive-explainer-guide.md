# Interactive Explainer Guide

How to build the "step through the code" interactive artifact for an LLD
problem, the way `parking-lot/explainer/index.html` does it — reusable
process, **not** a reusable visual template (each problem still gets its
own palette/vocabulary; see `artifact-design` skill).

## What actually worked (read this before building)

Three earlier shapes were tried and rejected before landing on this one —
skip straight to the last one:

1. ❌ **Reference doc with an embedded widget** — sections of prose + code
   blocks + a control-panel simulator. Correct but not fun; felt like
   documentation wearing an interactive costume.
2. ❌ **Auto-playing story/game** (click a choice → animation plays out in
   ~1-2s → next screen) — fun, but too fast to actually read the code
   commentary. Optimized for delight over understanding.
3. ✅ **Manual step-through trace, synced to a visual** — user picks
   setup choices (vehicle, strategy), then taps **"Next step →"** once per
   real method call. Each tap appends one line to a persistent trace log:
   the real code line + real live values for *this* run + a visual side
   effect (tile highlights, ticket fills in). User controls pace entirely.

The winning formula: **real code, real values, one small reveal per
click, user-paced, log persists so they can scroll back.** Nothing else
mattered as much as getting that loop right.

## Token-efficient build process

1. **Read the real source first** — every method body, field name, and
   test-scenario value you'll show. Do this once, up front, in parallel
   `Read` calls. No invented numbers (same rule as the `.drawio` diagrams).
2. **Skip the design-plan back-and-forth** — reuse the CSS token block
   below almost verbatim (concrete/paper palette, amber accent, system
   font stack). Only change: the accent hue and 2-3 domain nouns, if the
   subject genuinely calls for a different visual world. Don't re-derive
   a palette from scratch each time.
3. **Write the whole file in ONE `Write` call.** Don't iterate with `Edit`
   after `Edit` — plan the full step sequence mentally first (see
   "Mapping code to steps" below), then write it once.
4. **Publish once.** Don't loop Artifact-publish → screenshot → tweak →
   republish. That loop is what burned the most tokens/turns in earlier
   attempts (browser automation is also flaky against Claude's own
   artifact-viewer chrome — clicks sometimes need a plain re-screenshot to
   confirm state, see below). Publish, do a quick manual sanity read of
   the code, hand it to the user, fix only what they flag.
5. **If you do need to verify in-browser**: `find`/`read_page` will match
   the *host page's* chrome (title bar, share menu) before it reaches into
   the artifact's iframe — don't trust element refs here. Use pixel
   `computer` clicks against a screenshot, and if a screenshot looks
   "stuck" right after a click, take one more plain screenshot before
   concluding it's broken — the tool has shown a stale frame on the first
   call after an action more than once.

## Mapping code to steps

For each user-facing operation on the service (e.g. `parkVehicle`,
`unparkVehicle`), turn its real method body into an ordered list of
step objects: `{ code: "<real line>", detail: "<live values>", effect: fn }`.
One step per meaningful line/branch — a loop iteration, a strategy
dispatch, a field mutation, an object construction. ~4-8 steps per
operation is the right grain (fewer and you're hiding the interesting
part; more and it's tedious to tap through).

```js
// skeleton — see parking-lot/explainer/index.html for the full working version
function buildStepsFor<Operation>(...) {
  var steps = [];
  steps.push({ code: 'exact real line of code',
               detail: 'field/variable → concrete value for THIS run',
               effect: function(){ /* mutate state + update one DOM element */ } });
  // ...one entry per line/branch worth narrating...
  return steps;
}

function screen<Operation>Steps(...) {
  var steps = buildStepsFor<Operation>(...), idx = 0;
  render();
  function render() {
    // draw the visual + a single "Next step →" button
    document.getElementById('next-step').addEventListener('click', function(){
      var s = steps[idx]; logStep(s.code, s.detail, s.effect); idx++;
      idx >= steps.length ? advanceToNextScreen() : render();
    });
  }
}
```

`logStep` appends to a persistent trace log (never cleared between
operations — use `logDivider("— Car #2 —")`-style separators instead of
wiping it), so the user can scroll back through the whole run.

## Structure that repeats

1. **Setup screens** — 1-2 short choice-card screens (pick the entities
   involved, e.g. vehicle type + strategy). Each choice logs its own
   construction line (`new Car("...")`) immediately, before stepping ever
   starts.
2. **Step screen(s)** — one per service operation, "Next step →" driven,
   as above.
3. **Outcome screen** — shows the settled result (ticket, fee, whatever
   the operation produced), with a way to loop back to setup.
4. Keep a live visual (the domain's own board/diagram — a garage grid, a
   ledger, a board state) rendered from the SAME state object the trace
   steps mutate, never a separate fake copy.

## CSS token skeleton (copy, adjust hue only if the subject demands it)

```css
:root {
  --paper:#e6e8ea; --surface:#f6f7f7; --surface-2:#eceeef;
  --ink:#14171a; --ink-dim:#565d61; --ink-faint:#878d90;
  --line:#c7cbcd; --line-strong:#a8adaf;
  --amber:#e0982f; --amber-strong:#c67f1e; --on-amber:#1a1206;
  --open:#3f8f63; --open-fill:#dcece2; --taken:#b6503f; --taken-fill:#f3ddd8;
}
/* + dark theme: :root:not([data-theme="light"]) under prefers-color-scheme,
   and :root[data-theme="dark"] again, both redefining the same tokens.
   Full block: parking-lot/explainer/index.html lines ~1-30. */
```

Only swap the accent hue (`--amber*`) and the two semantic state colors if
the new subject's own vocabulary genuinely calls for it (e.g. a card game
might want a felt-green instead of concrete-grey as `--paper`). Don't
redesign from zero — that's the token cost this doc exists to avoid.

## Where the reference lives

`parking-lot/explainer/index.html` is the canonical, working example —
copy its `<script>` structure (`state`, `logStep`/`logDivider`,
`screen*()` functions, vehicle-glyph-style small inline SVG helpers) and
swap in the new problem's domain logic. Don't rebuild the trace-log
mechanics from scratch per problem.
