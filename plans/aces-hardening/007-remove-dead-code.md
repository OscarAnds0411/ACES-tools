# Plan 007: Remove production code that nothing calls

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/comparison src/com/validador/aces/reporting src/com/validador/aces/utils DebugSheets.java`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S
- **Risk**: LOW
- **Depends on**: 004-comparator-single-path.md (004 edits `Comparator`; this plan must see its final shape), 001-reliable-test-gate.md
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

About 700 lines of production classes have no caller in the app: they cost
reading time, drift from the real code, and `tasks.md` claims they are
"COMPLETADO / Verificado" features users can reach. Deleting them applies the
"deletion test": removing them concentrates no complexity anywhere else.
Removing them also stops contributors from extending the wrong report class.
(Everything stays recoverable from git history.)

## Current state

Verified by grep over `src/` during planning (re-run before deleting — see
Step 1):

| File | Lines | Referenced by |
|---|---|---|
| `src/com/validador/aces/comparison/ComparisonCache.java` | 170 | only itself |
| `src/com/validador/aces/reporting/ReportFilter.java` | 198 | only itself (no test) |
| `src/com/validador/aces/reporting/ExcelExporter.java` | 195 | only itself; `ReportPrinter` mentions it in a comment |
| `src/com/validador/aces/reporting/ReportPrinter.java` | 166 | only itself and a comment in `ExcelExporter` (no test) |
| `src/com/validador/aces/utils/README.md` | 114 | package has **no** `.java` files; README documents `Logger`, `StringUtils`, … that do not exist |
| `DebugSheets.java` (repo root) | ~20 | nothing; default package; hardcodes `ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx` |

**Keep (do not delete)**: `EnumValidator`, `RangeValidator`, `DateFormatValidator`
(`validation/`) — used only by tests today but they are the planned
extension point for value-level validation (a direction the owner has not
decided yet); `ReportGenerator` and `ExcelReportGenerator` (the live report
path, called from `ValidationPanel`); `Report`/`ReportSection` (used by the
generator); `ComplianceCalculator`/`ComplianceMetrics`/`ProductLineSummary`.

Conventions: nothing special; this is deletion. Tests live in
`src/com/validador/aces/tests/` and register in `TestRunner.java`.

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; Remove-Item -Recurse -Force $out -ErrorAction SilentlyContinue; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 (clean output dir so no stale `.class` hides a missing reference) |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`; same pass count as before |

## Scope

**In scope** (delete only): the six files in the table above (and the now
empty `utils/` directory).

**Out of scope**:
- Any file in "Keep".
- Updating `tasks.md`, READMEs and other docs that mention these classes — Plan 009 does all doc updates (leave dangling doc references for now; note them).
- Wiring validators or caches instead of deleting (a product decision).

## Steps

### Step 1: Re-verify there are no callers
For each class name (`ComparisonCache`, `ReportFilter`, `ExcelExporter`,
`ReportPrinter`) `Grep` across `src/` (all `*.java`, including `tests/`),
excluding the file itself. Also `Grep` the root and `build.xml` for
`DebugSheets` and `utils`.

**Verify**: the only hits are the files themselves and comments. If any
`new X(`, `X.` static call, `import`, or test references exist → STOP.

### Step 2: Record the baseline
Clean compile and run Tests; note the passed count N (95 + tests added by
earlier plans).

**Verify**: `TODOS PASAN`.

### Step 3: Delete
Delete the six files listed (use `git rm` is **not** needed; plain deletion is
fine — do not commit). Remove the empty `utils` directory.

**Verify**: clean Compile → exit 0; Tests → `TODOS PASAN` with the same N.

## Test plan

No new tests; the guard is "same count, still green, clean compile". Because
`ReportFilter` and `ReportPrinter` had no tests, no test removal occurs. If a
test *does* reference a deleted class, that is a STOP.

## Done criteria

- [ ] Clean Compile → exit 0; Tests → `TODOS PASAN`, count == baseline N
- [ ] The six files no longer exist; `src/com/validador/aces/utils/` gone
- [ ] `git status --short` shows only those deletions (plus `plans/`)
- [ ] Final report lists docs that still mention deleted classes (input for Plan 009)

## STOP conditions

Stop if:
- Any caller/test/reference to a to-be-deleted class exists.
- Plan 004 changed `Comparator` to use `ComparisonCache` (then the premise is false).
- `build.xml` or a script references `DebugSheets` or `utils`.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

If the owner later wants an LRU result cache, a report filter, or a console /
single-application Excel export, recover the code from git history
(`git log --diff-filter=D -- <path>`) rather than keeping unreachable code
around. A reviewer should check that no reflection-based loading
(`Class.forName`) refers to these names (grep showed none).
