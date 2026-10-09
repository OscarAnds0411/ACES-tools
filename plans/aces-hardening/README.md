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
| [002](002-clone-buildable-and-claude-md.md) | Fresh clone builds; `CLAUDE.md` with verified commands | S | 001 | DONE |
| [003](003-parser-correctness.md) | Parsers read catalog/ACES faithfully | M | 001 | DONE |
| [004](004-comparator-single-path.md) | Resolve product line once; fix duplicate-name divergence | M | 001 (run after 003) | DONE |
| [005](005-safe-report-export.md) | Safe export: confirm overwrite, protect ACES, atomic write | S–M | 001 | DONE |
| [006](006-invalidate-stale-results.md) | Invalidate results when inputs change | S–M | 005 | DONE |
| [007](007-remove-dead-code.md) | Remove uncalled production code | S | 001, 004 | DONE |
| [008](008-single-config-source.md) | One config source; `maxTableRows` works | S | 006 | DONE |
| [009](009-docs-consolidation.md) | Docs match the project; archive history | M | 002, 007, 008 | DONE |

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

- **2026-10-09**: Plan 009 closed (working tree, not committed) — all 9 plans
  done. Deviations: package READMEs (comparison/reporting/validation/parsers/gui)
  and `tests/README.md` were not just stale but described classes that never
  existed (all `[TODO]`) → rewritten, not patched; `EXCEL_FORMAT_GUIDE.md`
  archived (describes sheets "Catalog/ProductLines" that don't match the real
  file); `models/README.md` spot-checked only (enums match code), left as is.
  Remaining stale-term hits outside `archive/`/`plans/` are intentional
  negations or annotated history in `tasks.md`. Out of scope and still stale:
  `.kiro/specs/.../design.md`. All README commands run as written, incl.
  `java -jar dist/validador-aces.jar`. 13 docs moved with `git mv`; none deleted.
- **2026-10-09**: Plans 007 + 008 closed (working tree, not committed). 007:
  six files + `utils/` deleted (all tracked and unmodified → recoverable with
  `git checkout -- <path>`), clean compile, suite unchanged at 120. 008:
  `ConfigDialog.getMaxTableRows()` now drives `ValidationPanel` (read once per
  render); `src/application.properties` deleted (no reader). Verified with a
  throwaway program using `-Duser.home` temp dirs (never touched the real user
  config): default → 3000/3000 rows; `maxTableRows=1000` → 1000 rows + label
  "(mostrando primeros 1.000)"; HEAD ignores the setting (negative control).
  Docs still mentioning removed items (input for 009): GENERATED_ARTIFACTS,
  IMPLEMENTATION_GUIDE, INDEX, LOGICA_DEL_PROGRAMA, PROGRESS, PROJECT_STRUCTURE,
  QUICK_START, README, REFERENCE, SETUP_COMPLETE, START_HERE, tasks.md,
  `.kiro/specs/.../design.md`. Next: 009.
- **2026-10-09**: Plan 006 closed (working tree, not committed). Deviation:
  small edit in `StatisticsPanel.refresh()` (out of the listed scope) — clears
  the stale detail text/summaries in the empty state. Instead of only a manual
  smoke, the real `MainWindow`+`ValidationPanel`+`StatisticsPanel` were driven
  headlessly by a throwaway program outside the repo (21 checks: invalidation on
  new ACES/catalog/failed load, same-instance reassign keeps results, stats
  empty state): all pass; same program against HEAD fails 10 checks (negative
  control). Not clicked by the executor: file dialogs, real load buttons.
  Suite unchanged at 120 (no Swing unit tests exist); `ant test` OK. Next: 007.
- **2026-10-09**: Plan 005 closed (working tree, not committed). 8 new tests
  (plan asked ≥4); the truncation defect was reproduced before the fix (failed
  render left the existing file truncated), suite 112 → 120, `ant test` OK.
  Deviation: added `describeExportError` in `ValidationPanel` (open-in-Excel →
  `AccessDeniedException` previously showed only the path; observed on Windows:
  original intact, no temp left). GUI guards (a)(b)(c) NOT operated by the
  executor; app starts. `AuditPanel` CSV export untouched (deferred). Next: 006.
- **2026-10-09**: Plan 004 closed (working tree, not committed). 10 new tests
  (8 equivalence passed before AND after; 2 duplicate-name tests failed before,
  pass now); suite 102 → 112, `ant test` OK. Real data (38 265 lines × 81 886
  apps): results identical to HEAD (same checksum) and `compareAll` 10.6 s →
  0.18 s; synthetic 20k×40k: 6.7 s → 0.21 s, identical checksum. Real catalog
  has 0 duplicate names, so first-wins changes nothing on it. Real ACES is 100 %
  compliant (0 missing), so missing-attribute behavior is covered only by
  synthetic tests. Heads-up: `ParserRegressionTest.java` and
  `ComparatorEquivalenceTest.java` are untracked while the committed
  `TestRunner` references them → commit them or HEAD's `tests/` won't compile.
  Next: 005.
- **2026-10-09**: Plan 003 closed (working tree, not committed). 7 regression
  tests (plan asked ≥4), seen failing before the fix (shift reproduced: `C` read
  REQUIRED instead of OPTIONAL); suite 95 → 102, all pass. Real catalog
  unchanged by the fix (38 265 lines, 12 984 REQUIRED, identical checksum vs
  HEAD) and 0 parser warnings on it → no compliance shift expected from 003
  on the real catalog. GUI smoke (steps 5) NOT operated by the executor: app
  starts, fixtures in `%TEMP%\aces-smoke\` (`catalogo_smoke.xlsx`,
  `aces_smoke.xlsx`) for the owner to click through. Next: 004.
- **2026-10-09**: Plan 002 closed (working tree, not committed). Deviations:
  `fastexcel`/`fastexcel-reader` license not declared in the jars → marked
  "sin verificar" in `lib/README.md` (owner to confirm upstream before
  redistributing); `README.md:79` and `IMPLEMENTATION_GUIDE.md:213` still tell
  users to use the deleted `manifest.txt` (fixed by Plan 009); `!manifest.txt`
  line left in `.gitignore`. `ant clean jar` + `ant test` verified (Ant 1.10.18).
  Next: 003.
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
