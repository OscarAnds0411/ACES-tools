# ACES-tools hardening (audit follow-up)

Goal: make the compliance numbers trustworthy, give the project a verification
gate and a buildable clone, protect the user's files on export, and remove code
and docs that do not match reality. Planned from an `improve` audit (standard
effort) of the repo at revision `d185ef8` (branch `fix/UI-UX`, 2026-10-09).
Audit tracks → plans: **A** (verification baseline) = 001–002, **B** (compliance
correctness) = 003, **C** (performance) = 004, **D** (data safety in the GUI) =
005–006, **E** (cleanup) = 007–009. Track order is deliberate: nothing else is
safe to change until the test gate is trustworthy (A); correctness fixes (B, C)
change the numbers users see, so they land before anything cosmetic.

Execute in the order below unless dependencies say otherwise. Each executor:
read the plan fully before starting, honor its STOP conditions, and update
your row when done. Do not commit or push unless the user asks.

Common commands (PowerShell, repo root; verified on JDK 25 with `--release 11`;
baseline **95 tests passing**):

```
$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out | Out-Null
javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName
java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner
```

Ant is not on PATH on the planning machine; `ant` steps are verified only if it
is available (`C:\opt\ant\bin\ant.bat`).

## Execution order & status

| Plan | Title | Effort | Depends on | Status |
|------|-------|--------|------------|--------|
| [001](001-reliable-test-gate.md) | Make the test suite a reliable gate | S–M | — | DONE |
| [002](002-clone-buildable-and-claude-md.md) | Fresh clone builds; `CLAUDE.md` with verified commands | S | 001 | TODO |
| [003](003-parser-correctness.md) | Parsers read catalog/ACES faithfully | M | 001 | TODO |
| [004](004-comparator-single-path.md) | Resolve product line once; fix duplicate-name divergence | M | 001 (run after 003) | TODO |
| [005](005-safe-report-export.md) | Safe export: confirm overwrite, protect ACES, atomic write | S–M | 001 | TODO |
| [006](006-invalidate-stale-results.md) | Invalidate results when inputs change | S–M | 005 | TODO |
| [007](007-remove-dead-code.md) | Remove uncalled production code | S | 001, 004 | TODO |
| [008](008-single-config-source.md) | One config source; `maxTableRows` works | S | 006 | TODO |
| [009](009-docs-consolidation.md) | Docs match the project; archive history | M | 002, 007, 008 | TODO |

Status values: TODO | IN PROGRESS | DONE | BLOCKED (one-line reason) |
SUPERSEDED (one-line pointer to what replaced it)

## Dependency notes

- **001 → everything**: later plans add tests and rely on a runner that fails
  loudly and skips real-data tests cleanly.
- **001 → 002**: `CLAUDE.md` documents the commands/skip behavior 001 establishes.
- **003 before 004**: both register new test classes in `TestRunner`; 003 is
  independent of 004 logically but keeping order avoids merge noise.
- **004 → 007**: 004 rewrites `Comparator`; 007 must confirm `ComparisonCache`
  is still uncalled after that.
- **005 → 006 → 008**: all three edit `ValidationPanel.java`; sequential to avoid conflicts.
- **002, 007, 008 → 009**: docs describe the final state (commands, deleted
  classes, config).
- **Behavior change to announce**: 003 and 004 can change compliance numbers
  on real data (shifted columns fixed, case-insensitive requirements, duplicate
  names first-wins). Tell the user before they compare with old reports.

## Reconciliation log

(Newest first. A few lines per entry, hard cap.)

- **2026-10-09**: Plan 001 closed (changes in working tree, not committed).
  Deviations: 19 real-data tests guarded (plan estimated ~15); fresh-clone
  simulation = 76 passed / 19 skipped / 0 failed; negative checks (failing test,
  empty suite) were run on a temp copy of `src`, not in the repo; `ant test`
  verified (Ant 1.10.18, BUILD SUCCESSFUL, 95/95). Next: 002.
- **2026-10-09**: Effort planned from the `improve` audit.

## Considered and rejected

- Wiring Enum/Range/DateFormat validators now: product decision (where do rules
  live?) — kept as a direction, not planned; validators are kept in 007.
- Merging duplicate product names instead of first-wins: first-wins matches
  `Catalog.findProductByName` and the single-compare path; revisit only if real
  catalogs show conflicting duplicates (004 STOP condition).
- Replacing the custom `TestRunner` with JUnit: adds a dependency; out of
  proportion to the problem (001 fixes the actual gaps).
- Ant→Maven/Gradle migration: large change for 8 jars (002 centralizes the list).

## Deferred

- Audit leads needing evidence: numeric cell text formatting (leading zeros /
  scientific notation), header row detection beyond row 1, accent/whitespace
  normalization, `Locale.ROOT`, duplicate catalog headers, large-file memory
  profile — see the audit findings in the conversation that created this effort.
- GUI worker cancellation / sheet-selection race; audit-history CSV export
  (quoting, UTF-8, overwrite) — noted in 005/006 maintenance notes.
- Directions for the owner to decide: value-level validation, per-row Excel
  location + highlighted fix-up export, headless/CLI mode, persisted audit
  history.
- `AuditSession` extraction from `MainWindow` (makes GUI state testable) — 006.
