# Plan 005: Make report export safe (confirm overwrite, never overwrite the ACES input, write atomically)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/reporting/ExcelReportGenerator.java src/com/validador/aces/gui/ValidationPanel.java src/com/validador/aces/tests/ExcelReportGeneratorTest.java`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S–M
- **Risk**: LOW
- **Depends on**: 001-reliable-test-gate.md
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

The "export report" button lets the user pick any path. A plain `JFileChooser`
does not ask before overwriting, the generator opens the target with
`new FileOutputStream(outputFile)` (truncating it immediately), and nothing
stops the user from choosing the very ACES `.xlsx` they just loaded — which
would destroy their source data. The write is also not atomic: a failure
half-way leaves a truncated, unreadable file. The Javadoc even says the output
"must never be the original ACES" but nothing enforces it. After this plan,
overwriting asks first, the loaded ACES file cannot be chosen, and a failed
export leaves any existing target untouched.

## Current state

`src/com/validador/aces/gui/ValidationPanel.java` (export handler, lines ≈ 436-488):
```java
File acesFile = window.getLoadedAcesFile();
File defaultOut = acesFile != null ? ExcelReportGenerator.defaultReportFileFor(acesFile)
                                   : new File(System.getProperty("user.home"), "Reporte_Auditoria_ACES.xlsx");
JFileChooser chooser = new JFileChooser(defaultOut.getParentFile());
... chooser.setSelectedFile(defaultOut); filter .xlsx; setAcceptAllFileFilterUsed(false);
if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return;
File outputFile = chooser.getSelectedFile();
if (!outputFile.getName().toLowerCase().endsWith(".xlsx"))
    outputFile = new File(outputFile.getAbsolutePath() + ".xlsx");
final File finalOut = outputFile;
... new SwingWorker<Void, Void>() { doInBackground: new ExcelReportGenerator().generateAndWriteBatch(snapshot, finalOut); ... }
```
Errors from the worker are already surfaced (`ExecutionException` branch shows
a dialog with the cause message) — keep that.

`src/com/validador/aces/reporting/ExcelReportGenerator.java:295-316`
`writeReportToFile(Report, File)`: validates args, then
`try (OutputStream os = new FileOutputStream(outputFile)) { Workbook workbook = new Workbook(os, ...); ... workbook.finish(); }`.
`generateAndWriteBatch` (325-329) calls it. `defaultReportFileFor` (340-349)
returns a sibling `<base>_Reporte_Auditoria.xlsx`.

`MainWindow.getLoadedAcesFile()` returns the loaded ACES `File` (or null). The
catalog file is **not** stored in `MainWindow` (out of scope to add it).

`AuditPanel.java:171-198` has its own CSV export using `FileDialogHelper`
(`FileDialogHelper.java:42-51`, also without overwrite confirmation) — see
"Out of scope".

Test conventions: `tests/ExcelReportGeneratorTest.java` (temp `.xlsx` in the
system temp dir, `Assert.*`, `public static void testXxx()`).

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |

## Scope

**In scope**:
- `src/com/validador/aces/reporting/ExcelReportGenerator.java` (`writeReportToFile` atomic write; a small public static same-path helper)
- `src/com/validador/aces/gui/ValidationPanel.java` (export handler only)
- `src/com/validador/aces/tests/ExcelReportGeneratorTest.java` (new tests)

**Out of scope**:
- `AuditPanel` CSV export and `FileDialogHelper` (a separate CSV-quality follow-up; note it in your report, don't touch).
- Storing/refusing the catalog file path.
- Report content/format.

## Steps

### Step 1: Atomic write in the generator
`writeReportToFile`: write the workbook to a temp file in the **same
directory** as `outputFile` (e.g. `Files.createTempFile(dir, base, ".tmp")`),
then `Files.move(tmp, outputFile, REPLACE_EXISTING)` (try `ATOMIC_MOVE`, fall
back to plain replace if the filesystem refuses). On any exception, delete the
temp file and rethrow; the original `outputFile` (if any) must be untouched.
Keep the method signature and the existing argument validation/messages.

**Verify**: Compile → exit 0; existing `ExcelReportGeneratorTest`/`EndToEndTest`/
`ReportCompletenessTest` still pass.

### Step 2: Same-file guard helper
Add `public static boolean isSameFile(File a, File b)` (null-safe; compare
`toPath().toRealPath()` when both exist, else normalized absolute paths;
case-insensitive on Windows is handled by `Path.equals`/`Files.isSameFile` —
prefer `Files.isSameFile` when both exist). Place it in `ExcelReportGenerator`
so the GUI and tests share it.

**Verify**: Compile → exit 0.

### Step 3: GUI guards
In the `ValidationPanel` export handler, after the `.xlsx` extension fix and
before starting the worker:
1. if `isSameFile(outputFile, window.getLoadedAcesFile())` → error dialog
   "No se puede sobrescribir el archivo ACES original…", return (do not start
   the worker, do not call `setButtonsEnabled(false)`);
2. else if `outputFile.exists()` → `JOptionPane.showConfirmDialog` (YES/NO,
   default NO) "El archivo ya existe. ¿Desea reemplazarlo?"; on NO return.
Spanish text, same dialog style as neighbours (`JOptionPane` with `window`
as parent).

**Verify**: Compile → exit 0. Manual smoke (GUI has no automated tests; report
the outcome): run `java -cp "lib\*;$out" com.validador.aces.Launcher`, load a
small catalog+ACES (fixtures from the test writers, written to `$env:TEMP`),
run the audit, export: (a) choose the ACES file → refused; (b) choose an
existing report → confirm dialog, NO leaves it intact; (c) new name → succeeds.

## Test plan

New tests in `ExcelReportGeneratorTest`:
- writing to a path that already holds a (dummy) file replaces it with a valid
  workbook (re-open with the project's `ExcelCatalogParser`-style reader or
  `ReadableWorkbook` and assert the sheet exists);
- writing into a non-existent directory throws `IOException` and leaves no
  stray `*.tmp` in the parent;
- failed write to an existing target leaves the original bytes intact
  (e.g. induce failure by passing a `Report` whose rendering throws, or an
  `outputFile` whose parent is a read-only/nonexistent directory — pick what
  is reliable on Windows and explain it in a comment);
- `isSameFile` true for the same path spelled two ways (`a\.\b.xlsx` vs
  `a\b.xlsx`) and for a real file vs itself; false for different files; false
  when either is null.
**Verify**: Tests → `TODOS PASAN`, ≥ previous count + 4.

## Done criteria

- [ ] Compile → exit 0; Tests → `TODOS PASAN` with the new tests
- [ ] `FileOutputStream(outputFile)` no longer appears in `writeReportToFile`
- [ ] Manual smoke (a)(b)(c) performed and reported
- [ ] No files outside the in-scope list modified

## STOP conditions

Stop if:
- `Files.move` replace semantics are unreliable on the target filesystem
  (e.g. network drive) — describe what happened.
- The export handler has changed so the guard point is unclear.
- You find other save paths (besides `ValidationPanel` and `AuditPanel`) that
  write user-chosen files — report them, don't expand scope.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

Follow-up (deferred): same overwrite confirmation and UTF-8/quoting fixes for
the audit-history CSV export (`AuditPanel.java:171-198`), and a
`FileDialogHelper`-based shared save dialog (Plan 007+ cleanup candidate).
Reviewer should check Windows-specific behavior of `Files.move` with a target
open in Excel (fails with an `IOException` — the existing error dialog shows it).
