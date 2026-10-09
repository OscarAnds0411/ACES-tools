# Plan 009: Make the documentation match the project (one README, one logic doc, history archived)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree. This plan changes only
> documentation; **do not edit any `.java` file.**
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- '*.md' tasks.md`
> Expect changes from Plans 002 (adds `CLAUDE.md`, `lib/README.md`), 007 and
> 008 (deleted files). If other docs changed, re-read them before editing.

## Status

- **Effort**: M
- **Risk**: LOW
- **Depends on**: 002-clone-buildable-and-claude-md.md, 007-remove-dead-code.md, 008-single-config-source.md
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

The repo root has 13 overlapping Markdown files (README, START_HERE, INDEX,
QUICK_START, REFERENCE, SETUP_COMPLETE, ANT_SETUP, GENERATED_ARTIFACTS,
PROGRESS, PROJECT_STRUCTURE, IMPLEMENTATION_GUIDE, XLSX_ANALYSIS_REPORT,
XLSX_STRUCTURE_ANALYSIS) plus `tasks.md` and an untracked
`LOGICA_DEL_PROGRAMA.md` (the accurate description of the program's logic).
Several are wrong: `PROGRESS.md` says 22/54 tasks and "next: Reportes" though
reports, GUI and tests exist; `README.md` tells users to run `App.java`
(removed; the entry point is `com.validador.aces.Launcher`) with a classpath
that does not work on Windows; `tests/README.md` marks everything `[TODO]` and
cites JUnit; `tasks.md` claims classes are integrated that nothing calls. A
newcomer cannot tell what is true. After this plan the docs agree with the code.

## Current state

(Facts from the audit; re-confirm each before editing.)

- `README.md`: structure tree lists `src/application.properties` (line ~29,
  and a section at ~193 describing it); says "Java 11 or higher" (line ~43);
  compile snippet `javac -cp lib/*:. ... src/**/*.java` (line ~73, invalid on
  Windows and plain sh); "Ejecuta `App.java`" (line ~87); lists testing and
  caching as future work (lines ~151-166) though tests exist (95) and
  `ComparisonCache` was removed in Plan 007.
- `PROJECT_STRUCTURE.md` lines ~16, 257, 312 list `src/App.java` as launcher.
- `SETUP_COMPLETE.md:5` and `ANT_SETUP.md:9` record JDK 25.0.4.1; build target is Java 11.
- `src/com/validador/aces/tests/README.md`: all tests `[TODO]`, cites JUnit 4/5 and `resources/test-data/`; reality: custom `TestRunner`, no JUnit, tests registered in `TestRunner.java`.
- `tasks.md`: line ~894 says "47/54"; only TASK-048/049/050 are unchecked (so 51/54). Marks `ComparisonCache`, `ExcelExporter`, `ReportFilter`, `ReportPrinter` as done/verified (deleted in Plan 007) and `FileDialogHelper` as used by `ValidationPanel` (only `AuditPanel` uses it), `ConfigDialog.getMaxTableRows` as readable everywhere (wired in Plan 008).
- `LOGICA_DEL_PROGRAMA.md` (untracked): accurate and Spanish; sections 4.3 and 6 mention validators "no conectados" and `application.properties` (now deleted) and `ComparisonCache` (deleted).
- Module READMEs under `src/com/validador/aces/*/README.md` (comparison, gui, models, parsers, reporting, validation) — read before declaring them stale; some may reference deleted classes.
- No `.md` is referenced by `build.xml`/`build.bat` (grep to confirm in Step 1).
- Archive location: **`archive/`** at repo root. Do **not** use `docs/` — it is
  gitignored and `ant clean` deletes it.

## Commands you will need

| Purpose | Command | Expected on success |
|---|---|---|
| Find stale mentions | `Select-String -Path (Get-ChildItem -Recurse -Filter *.md -Exclude plans -File).FullName -Pattern 'App\.java','application\.properties','ComparisonCache','ExcelExporter','ReportPrinter','ReportFilter','JUnit','\[TODO\]'` | after the plan: hits only under `archive/` |
| Compile sanity (no Java edits expected) | `git status --short -- '*.java'` | no `.java` changes from this plan |

## Scope

**In scope**:
- `README.md` (rewrite to be correct and short), `tasks.md` (status/count fixes), `src/com/validador/aces/tests/README.md`, the module READMEs that mention deleted/nonexistent classes
- `LOGICA_DEL_PROGRAMA.md` (fix sections 4.3/6; it is untracked — leave it untracked unless the user says to add it)
- Move to `archive/` (with `git mv` for tracked files): `START_HERE.md`, `INDEX.md`, `QUICK_START.md`, `REFERENCE.md`, `SETUP_COMPLETE.md`, `GENERATED_ARTIFACTS.md`, `PROGRESS.md`, `PROJECT_STRUCTURE.md`, `IMPLEMENTATION_GUIDE.md`, `ANT_SETUP.md`
- Keep at root: `README.md`, `CLAUDE.md` (Plan 002), `LOGICA_DEL_PROGRAMA.md`, `tasks.md`, and `EXCEL_FORMAT_GUIDE.md` if Step 1 finds it accurate for the real input formats
- `XLSX_ANALYSIS_REPORT.md` and `XLSX_STRUCTURE_ANALYSIS.md`: move to `archive/` and link them from README as "análisis histórico de los archivos de entrada"

**Out of scope**:
- Deleting any documentation outright (archive, don't delete).
- Any `.java` or build file.
- `.kiro/specs/*` (design/requirements history; leave).

## Steps

### Step 1: Inventory and truth-check
For each root `.md` and each module README, read it and tag: KEEP-FIX,
ARCHIVE, or OK. Run the "Find stale mentions" command to build the list of
lines to fix. Confirm no build script references a `.md`.

**Verify**: you have a table (put it in your final report) of file → decision →
reason. Anything ambiguous → STOP condition below.

### Step 2: Rewrite `README.md`
Keep it ≤ ~120 lines, Spanish: what the tool does (point to
`LOGICA_DEL_PROGRAMA.md` for details); requirements (JDK 11+; Ant optional);
**how to build and test** — copy the exact verified commands from `CLAUDE.md`
(PowerShell compile/test, Ant equivalents); how to run (`com.validador.aces.Launcher`,
or `java -jar dist/validador-aces.jar` after `ant jar`); that `lib/` jars come
from the repo (see `lib/README.md`); that tests needing real data are skipped
without `catalogoDb/`/`ACES/`; configuration = the Settings dialog
(`~/.validador_aces_config.properties`); project layout (actual packages); link
to `archive/` for history.

**Verify**: the commands in README run as written (run them: compile, tests,
Launcher starts).

### Step 3: Fix the logic doc and the tests README
`LOGICA_DEL_PROGRAMA.md`: update the paragraph on unwired validators/cache
(cache removed; validators exist for tests/extension) and section 6
(configuration). `tests/README.md`: describe the real runner, how to add a test
(public static `testXxx()` in a `*Test` class + register in `TestRunner`), the
skip behavior, and delete the `[TODO]`/JUnit content.

**Verify**: the stale-mentions command shows no hits for these two files.

### Step 4: Fix `tasks.md` and module READMEs
`tasks.md`: correct the totals (recount unchecked tasks), mark removed
classes as "implementado y retirado (Plan 007, sin integrar)", and fix the
`FileDialogHelper`/`ConfigDialog` claims. Module READMEs: remove references to
deleted classes (`ComparisonCache`, `ReportFilter`, `ExcelExporter`,
`ReportPrinter`, the `utils` package).

**Verify**: stale-mentions command → hits only under `archive/` and `plans/`.

### Step 5: Archive the rest
Create `archive/` and move the files marked ARCHIVE (use `git mv` for tracked
files so history follows). Add a 5-line `archive/README.md` saying these are
historical planning/setup notes, possibly outdated.

**Verify**: `git status --short` shows renames (R) into `archive/`, no deletions
of documentation; `Get-ChildItem *.md` at root lists only the KEEP set.

## Test plan

Docs only. Verification is: every command written in README/CLAUDE.md was
executed once and worked; stale-mention search is clean outside `archive/` and
`plans/`; no `.java` changes.

## Done criteria

- [ ] Stale-mention search → hits only in `archive/` and `plans/`
- [ ] README commands executed successfully (compile, tests, launcher starts)
- [ ] Root contains only the KEEP set of `.md` files; archived files moved (not deleted)
- [ ] `git status --short -- '*.java'` → no changes from this plan
- [ ] `tasks.md` totals match a recount of checked/unchecked boxes

## STOP conditions

Stop if:
- A root document turns out to be the only record of a requirement or decision not found elsewhere (e.g. `.kiro/specs`) — hand back before archiving it.
- README facts you need are unverifiable (a command doesn't run) — report instead of documenting a guess.
- The owner might want the many onboarding files kept for non-technical users (hand back with the inventory table).

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

New decisions should go in `CLAUDE.md` (conventions/commands) or a short ADR
under `docs-adr/` (not `docs/`, which `ant clean` deletes and `.gitignore`
ignores). A reviewer should skim `archive/` moves for accidental content
changes (renames only). Deferred: consolidating module READMEs into Javadoc,
publishing the logic doc under README.
