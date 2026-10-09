# Plan 006: Invalidate audit results when the catalog or ACES file changes

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/gui src/com/validador/aces/Launcher.java`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S–M
- **Risk**: MED (GUI has no automated tests; manual smoke required)
- **Depends on**: 005-safe-report-export.md (both edit `ValidationPanel.java`; do 005 first)
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

After an audit run, loading a different catalog or ACES file leaves the old
results in place: the Statistics tab, the Validation results table and the
Export button keep using the *previous* run, so a user can export a report that
does not correspond to the files currently loaded. Also, when an ACES load
fails, the stale `loadedAcesFile` remains, so the suggested export filename is
derived from the old file. After this plan, changing either input clears the
previous results everywhere and the UI returns to its idle state.

## Current state

`src/com/validador/aces/gui/MainWindow.java` (lines 572-625) holds shared state:
```java
private Catalog loadedCatalog;
private List<Application> loadedApplications;
private File loadedAcesFile;
private Map<String, List<ComparisonResult>> lastComparisonResults;
public void setLoadedCatalog(Catalog catalog)       { this.loadedCatalog = catalog; checkCanRun(); }       // 586-589
public void setLoadedApplications(List<Application> apps) { this.loadedApplications = apps; checkCanRun(); } // 602-606
public void setLoadedAcesFile(File file)            { this.loadedAcesFile = file; }                       // 614
public void setLastComparisonResults(Map<...> results) { this.lastComparisonResults = results; }            // 618-621
```
`lastComparisonResults` is never cleared. Panels are wired in `Launcher.java:37-42`
(and `MainWindow.main` at ~671-674): `new ValidationPanel(mainWindow); new StatisticsPanel(mainWindow);`
and `mainWindow.setAuditPanel(auditPanel)` — the existing pattern for panel→window
registration is `setAuditPanel` (line 629).

`ValidationPanel.java`: states `ST_IDLE` (line 66), `ST_RUNNING`, `ST_RESULTS`;
`private void switchState(String state)` (line 492); fields `lastResults`,
`lastByProductLine`, `tableModel`; results are published with
`window.setLastComparisonResults(lastByProductLine)` (line 331).

`StatisticsPanel.java`: `refresh()` (line 108) reads
`window.getLastComparisonResults()` and is invoked from `componentShown`
(line 101), so clearing the window-level results is enough for it.

`ApplicationLoadPanel.java:455-489`: on success calls `setLoadedAcesFile(file)`
then `setLoadedApplications(apps)`; on failure (`ExecutionException`, line
478) only `setLoadedApplications(null)` — `loadedAcesFile` stays stale.
`CatalogLoadPanel.java:~500-514`: `setLoadedCatalog(catalog)` / `setLoadedCatalog(null)`.

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |
| Launch for smoke | `java -cp "lib\*;$out" com.validador.aces.Launcher` | window opens |

## Scope

**In scope**:
- `src/com/validador/aces/gui/MainWindow.java`
- `src/com/validador/aces/gui/ValidationPanel.java`
- `src/com/validador/aces/gui/ApplicationLoadPanel.java`, `CatalogLoadPanel.java` (only if needed to call the invalidation)

**Out of scope**:
- Worker cancellation / sheet-selection race (audit lead BUG-04), error-message detail in workers — deferred.
- Extracting the state out of `MainWindow` into a testable class (larger refactor).
- Changing what is displayed on success.

## Steps

### Step 1: Central invalidation in `MainWindow`
Add a private `invalidateResults()` that sets `lastComparisonResults = null`
and notifies registered listeners. Add a listener registration following the
`setAuditPanel` precedent (e.g. `addResultsInvalidatedListener(Runnable)`).
Call `invalidateResults()` from `setLoadedCatalog` and `setLoadedApplications`
**whenever the value changes** (a no-op re-set to the same instance should not
wipe results; compare by reference). In `setLoadedApplications(null)` also null
`loadedAcesFile`, or have `ApplicationLoadPanel` do it on failure — one place,
not both.

**Verify**: Compile → exit 0.

### Step 2: `ValidationPanel` resets itself
Register a listener in the `ValidationPanel` constructor that: clears
`tableModel`, nulls/clears `lastResults` and `lastByProductLine`, and calls
`switchState(ST_IDLE)`. It must run on the EDT (the setters are called from
`SwingWorker.done()`, already on the EDT; if you add any call from elsewhere,
wrap in `SwingUtilities.invokeLater`). Guard against the listener firing while
`ST_RUNNING` (an audit in flight): decide and document in a comment — the
simplest safe rule is that load panels already disable buttons while loading
(`window.setButtonsEnabled(false)`), so just confirm that and do nothing
special.

**Verify**: Compile → exit 0.

### Step 3: Statistics and export follow
`StatisticsPanel.refresh()` must render its empty/"no results" state when
`getLastComparisonResults()` is null (read it; if it already does, no change).
The export handler already returns early when there are no results — confirm it
checks `lastByProductLine`/results emptiness (it did at the top of the handler,
before line 436) and that it reads the cleared state.

**Verify**: Compile → exit 0; Tests → `TODOS PASAN`.

### Step 4: Manual smoke (record results)
Using small fixtures written to `$env:TEMP` (reuse the test writers), launch the
app and check:
1. Load catalog A + ACES → run audit → results table, Statistics tab populated.
2. Load a different ACES file → Validation panel returns to idle, Statistics
   tab empty, Export has nothing to export; run again → results appear.
3. Load a different catalog → same invalidation.
4. Load a *corrupt* ACES file (e.g. a renamed text file) → error dialog; the
   suggested export name no longer derives from the previous ACES file.

**Verify**: all four behave as described; report any that do not.

## Test plan

No automated GUI tests exist (the Swing classes are not unit-tested). The unit
suite must stay green. If you can isolate the "should invalidate?" decision
(same instance vs different) into a tiny pure method, add a test for it in
`tests/`; otherwise manual smoke is the verification and you say so.
**Verify**: Tests → `TODOS PASAN`, count ≥ previous.

## Done criteria

- [ ] Compile → exit 0; Tests → `TODOS PASAN`
- [ ] Manual smoke steps 1–4 performed and reported
- [ ] `lastComparisonResults` is nulled when catalog or applications change
- [ ] No files outside the in-scope list modified

## STOP conditions

Stop if:
- Another component besides `ValidationPanel`/`StatisticsPanel` caches results
  (grep `getLastComparisonResults` / `setLastComparisonResults` callers; the
  plan expects only those two).
- Invalidation causes an audit in progress to lose its results (race) and the
  fix is not a one-liner.
- The panels' construction order in `Launcher` vs `MainWindow.main` means a
  listener can be registered too late — describe the ordering.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

The "stale state" class of bug exists because shared state lives in a Swing
frame. Deferred cleanup: move the four state fields into a plain
`AuditSession` class that panels observe (makes this testable). A reviewer
should scrutinize the reference-equality check in the setters (reloading the
same file produces a *new* list, so it invalidates — that is intended).
