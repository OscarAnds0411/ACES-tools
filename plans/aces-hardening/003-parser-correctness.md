# Plan 003: Make the parsers read the catalog and ACES files faithfully (no silent column shifts, case drift or dropped rows)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/parsers src/com/validador/aces/models/AttributeRequirement.java src/com/validador/aces/gui/CatalogLoadPanel.java src/com/validador/aces/gui/ApplicationLoadPanel.java src/com/validador/aces/tests`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: M
- **Risk**: LOW–MED (changes numbers users see; that is the point)
- **Depends on**: 001-reliable-test-gate.md
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

The compliance % is the product. Three parser defects silently falsify it:
(1) a blank header cell in the catalog shifts every attribute to its right onto
the wrong column, so Required/Optional flags land on the wrong attributes;
(2) requirement values are matched case-sensitively and anything unknown
becomes "Not Required", so `required` or `Req` removes an attribute from the
denominator and overstates compliance; (3) ACES rows with an empty `Make` are
dropped with only a `System.err` line (a desktop app has no console), so they
vanish from totals and reports. After this plan, (1) is fixed, (2) is matched
case-insensitively with unknown values reported, and (3) dropped-row counts and
catalog warnings are shown to the user.

## Current state

Domain: the catalog Excel has one row per product line; columns 0–3 are fixed
(ID, name, category, subcategory) and from column 4 each header is an attribute
whose cell is `Required` / `Optional` / `Not Required`. The ACES Excel has one
row per application (Make, Model, Year, Product, optional PartNumber,
MfrLabel, Position) plus attribute columns.

`src/com/validador/aces/parsers/ExcelCatalogParser.java`
- `readAttributeHeaders` (lines 129-141) returns a compacted `List<String>`;
  a blank header does `continue` after a `System.err` message:
  ```java
  for (int i = FIRST_ATTRIBUTE_COLUMN; i < cellCount; i++) {
      String header = headerRow.getCellAsString(i).orElse(null);
      if (header == null || header.trim().isEmpty()) { System.err.println(...); continue; }
      headers.add(header);
  }
  ```
- `buildProductLine` (lines 143-166) reads the cell with the **list index**, not
  the original column — this is the bug:
  ```java
  for (int i = 0; i < attributeHeaders.size(); i++) {
      String headerName = attributeHeaders.get(i);
      String cellValue  = row.getCellAsString(FIRST_ATTRIBUTE_COLUMN + i).orElse("Not Required");
      pl.addAttribute(new Attribute(headerName, headerName, cellValue));
  }
  ```
- Rows with no ID are skipped silently (line 144-145); rows with an ID but no
  name are skipped with a `System.err` line (148-151).

`src/com/validador/aces/models/AttributeRequirement.java` lines 29-37:
`fromCellValue` does `switch (cellValue.trim())` on the exact strings
`"Required"`, `"Optional"`, `"Not Required"`; null and anything else →
`NOT_REQUIRED`. `Attribute` (models) builds its requirement from the cell
string via this method (see its String-arg constructor).

`src/com/validador/aces/parsers/ExcelApplicationParser.java`
- Public API used by tests and the GUI: `parse(File)`, `parse(File, String)`,
  `parse(File, String, Set<String>)` returning `List<Application>` — **do not
  change these signatures**.
- `processRows` (lines ~199-210): loop `buildApplication(...)`; a `null` result
  increments `skipped`; after the loop:
  `System.err.println("ExcelApplicationParser: " + skipped + " filas omitidas (Make vacío).");`
- `buildApplication` (line 227-230): `String make = cell(row, columns.core.get("make")); if (make == null) return null;`
- The GUI creates the parser inline:
  `ApplicationLoadPanel.java:460` → `return new ExcelApplicationParser().parse(file, sheet, interest);`
  inside a `SwingWorker<List<Application>, Void>` (lines 455-489); success is
  shown via `showSuccess(apps, sheet)` and `window.setStatus("✔  ACES cargado — N aplicaciones.", ...)`.
- Catalog GUI: `CatalogLoadPanel.java:493` → `new ExcelCatalogParser().parse(file, sheet)`;
  success handling around line 500 (`window.setLoadedCatalog(catalog)`).

Test conventions: `public static void testXxx()` in `public final class XxxTest`,
`Assert.*`, temp `.xlsx` written with fastexcel — exemplar
`tests/CatalogRoundTripTest.java:95-127` (`writeTmpCatalog`) and the writer
helper in `tests/ApplicationRoundTripTest.java` (read it before writing new
application fixtures). New suites must be registered in
`tests/TestRunner.java` (list at lines 30-68; Plan 001 makes an empty suite a
failure).

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |

## Scope

**In scope**:
- `src/com/validador/aces/parsers/ExcelCatalogParser.java`
- `src/com/validador/aces/parsers/ExcelApplicationParser.java`
- `src/com/validador/aces/models/AttributeRequirement.java`
- `src/com/validador/aces/gui/CatalogLoadPanel.java`, `ApplicationLoadPanel.java` (display of warnings only)
- `src/com/validador/aces/tests/ParserRegressionTest.java` (new), `TestRunner.java` (register it)

**Out of scope**:
- Header detection beyond row 1, numeric/date cell formatting (`getCellText`), accent/whitespace normalization, duplicate catalog headers — all LOW-confidence audit leads, deferred.
- `Comparator` and validators (Plan 004).
- Treating unknown requirement text as an error: keep mapping unknown → `NOT_REQUIRED` (compat), but warn.

## Steps

### Step 1: Write the failing regression tests first
Create `ParserRegressionTest` with temp-file fixtures (delete in `finally`):
1. Catalog with a blank header cell at column 5 (headers `A`, blank, `C`) and
   one product row (`Required`, `Optional`, `Required`... choose values so a
   shift is detectable) → `C` must have the requirement written under `C`'s own
   column; the blank column produces no attribute.
2. Catalog cells `required`, `REQUIRED`, ` Optional `, `not required` →
   REQUIRED, REQUIRED, OPTIONAL, NOT_REQUIRED.
3. Catalog cell `Req` (non-empty, unrecognized) → attribute is NOT_REQUIRED **and**
   the parser exposes a warning mentioning the product, attribute and value.
4. ACES file with 5 data rows of which 2 have empty Make → 3 applications and
   the parser reports 2 skipped rows.
Register the suite in `TestRunner`.

**Verify**: Compile → exit 0; Tests → the new tests 1–4 FAIL (or do not compile
against the missing API — adding the minimal API stubs in Step 2 first is
acceptable), all pre-existing tests still pass.

### Step 2: Fix the catalog column mapping
In `ExcelCatalogParser`, keep each header's original column index alongside its
name (e.g. a small private holder or parallel list) and read each row's cell
by that original index. Blank headers are still omitted, but no longer shift
anything. Surface the omission as a warning (Step 4), not only `System.err`.

**Verify**: test 1 passes; Tests → all pass.

### Step 3: Match requirements case-insensitively and flag unknown values
`AttributeRequirement.fromCellValue`: compare with `equalsIgnoreCase` after
trim; keep null/unknown → `NOT_REQUIRED`. Add a small public static
`isRecognized(String)` (true for blank/null "treated as Not Required" **and**
the three known labels, false otherwise) so the catalog parser can detect
unknown non-empty values. In `ExcelCatalogParser.buildProductLine` collect a
warning for each unrecognized non-empty cell.

**Verify**: tests 2 and 3 pass; Tests → all pass.

### Step 4: Expose warnings and skipped-row counts
Give each parser instance a read-only accessor for what happened during its
last parse — e.g. `List<String> getWarnings()` on `ExcelCatalogParser` (blank
headers, rows skipped for missing name, unrecognized requirement values,
capped at a sensible number with a "+N more" entry) and
`int getSkippedRowCount()` on `ExcelApplicationParser`. Existing `parse(...)`
signatures stay unchanged. Reset the state at the start of each `parse`.

**Verify**: test 4 passes; Tests → all pass.

### Step 5: Show them to the user
`ApplicationLoadPanel`: create the parser instance in a local `final` before
the worker so `done()` can read `getSkippedRowCount()`; when > 0 add one line
to the success summary / status text ("N filas omitidas por Make vacío").
`CatalogLoadPanel`: when warnings exist, add a count line to the success
summary and make the full list reachable (tooltip, or a `JOptionPane` with a
scrollable text area — match what the panel already uses for messages). Keep
Spanish text and the panels' existing visual style.

**Verify**: Compile → exit 0. Manual smoke (record the result in your final
report; GUI has no automated tests): launch with
`java -cp "lib\*;$out" com.validador.aces.Launcher`, load a catalog written
with a blank header cell and an ACES file with blank-Make rows (generate them
with the test fixtures' writers into `$env:TEMP`), confirm the messages appear.

## Test plan

New `ParserRegressionTest` as above (4 tests minimum). Existing tests are the
regression net: `CatalogParserTest`, `ApplicationParserTest`, the two
round-trip suites, `EndToEndTest` must stay green (real-data ones may show as
SKIPPED without the data after Plan 001 — on the planning machine they run).
**Verify**: Tests → `TODOS PASAN`; count ≥ 99 (95 baseline + 4).

## Done criteria

- [ ] Compile → exit 0; Tests → `TODOS PASAN`, ≥ 99 passed, 0 failed
- [ ] `ParserRegressionTest` tests 1–4 exist, were seen failing before Steps 2–4, and pass now
- [ ] `parse(File)`, `parse(File,String)`, `parse(File,String,Set)` signatures unchanged (`git diff` shows no change to those lines' signatures)
- [ ] No files outside the in-scope list modified

## STOP conditions

Stop if:
- Fixing the column index mapping changes the output of `CatalogParserTest`
  against the real catalog (`catalogoDb/...`) — that means the real file has
  blank headers *and* the numbers legitimately move; hand back with the
  before/after counts of Required attributes per line for the owner to judge.
- `Attribute`'s requirement construction does not go through
  `AttributeRequirement.fromCellValue` (then the case fix lands in the wrong
  place — describe what you found).
- The GUI success panels cannot show extra text without restructuring them.
- A design fork appears on how to expose warnings (instance accessor vs result
  object) that makes existing callers change.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

Reviewer should scrutinize: that unknown requirement values still map to
`NOT_REQUIRED` (intentional, compat) and are only *reported*. Any real catalog
that previously had shifted columns will now produce different compliance
numbers — tell the user before they compare against old reports. Deferred:
header-row detection (banner rows), `Locale.ROOT` for `toLowerCase`,
whitespace/accent normalization shared by catalog and ACES, duplicate catalog
headers, numeric cell formatting.
