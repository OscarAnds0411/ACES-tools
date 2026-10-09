# Plan 004: Resolve each application's product line once in the Comparator (fixes duplicate-name divergence and the O(apps × lines) scan)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/comparison/Comparator.java src/com/validador/aces/validation src/com/validador/aces/models/Catalog.java src/com/validador/aces/tests`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: M
- **Risk**: MED (core compliance path; guarded by characterization tests)
- **Depends on**: 001-reliable-test-gate.md (003 is independent but both register tests in `TestRunner`; execute 003 first to avoid merge noise)
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

`Comparator.compareAll` builds a name→`ProductLine` index and a
name→`ValidationSchema` cache so it never rescans the catalog per application —
but then calls `CompositeValidator.validate(application, catalog)`, whose
`AttributeValidator` re-resolves the line with a linear
`Catalog.findProductByName` and builds a fresh `ValidationSchema` for every
application. With the real data (~38k catalog lines × ~82k applications, per
`tasks.md`) that is on the order of billions of `equalsIgnoreCase` calls: the
optimization exists only on paper. The same split causes a correctness bug:
when two product lines share a name, the index keeps the **last**, while the
validator keeps the **first**, so `total` (from one line) and `invalid` (from
the other) can disagree and `total - invalid` can go negative, which makes
`ComplianceMetrics` throw. After this plan each application is resolved
exactly once and total/invalid come from the same line.

## Current state

`src/com/validador/aces/comparison/Comparator.java`
- `compareAll` (≈ lines 150-175): builds `productIndex` and `schemaCache`, then
  per application:
  ```java
  String key = normalizeKey(application.getProductName());
  ProductLine line = key == null ? null : productIndex.get(key);
  ValidationSchema schema = key == null ? null : schemaCache.get(key);
  results.add(compareInternalOptimized(application, catalog, line, schema));
  ```
- `buildProductLineIndex` (185-194): `index.put(key, line)` → **last wins**.
- `compareInternalOptimized` (222-249): creates a new `CompositeValidator` +
  `AttributeValidator` per application, calls
  `compositeValidator.validate(application, catalog)` (re-resolves), then
  `total = schema.getRequiredAttributes().size()` (from the *index* line) and
  `missingCount = countErrorsByCode(allErrors, MISSING_REQUIRED_ATTRIBUTE)`
  (from the *validator's* line); `valid = total - missingCount`.
- `compare(...)` / `compareInternal` (≈ lines 115 and 258-300) is a second,
  near-duplicate path used for single applications.
- `normalizeKey` (251-256): `value.trim().toLowerCase()` or `null` if blank.

`src/com/validador/aces/validation/AttributeValidator.java:35-71`
`validate(Application, Catalog)`: null checks; `catalog.findProductByName(app.getProductName())`;
if `null` → one `WARNING` with code `PRODUCT_LINE_NOT_FOUND` (constant on the
class) and return; else `new ValidationSchema(line)`,
`schema.findMissingRequiredAttributes(application)` → one `ERROR` per name with
code `MISSING_REQUIRED_ATTRIBUTE` (message
`"El atributo requerido '<name>' no está presente en la aplicación"`,
`attributeName`, `line.getName()`).

`src/com/validador/aces/models/Catalog.java:120-126` `findProductByName`: stream,
`equalsIgnoreCase` on trimmed input, **`findFirst`** (first wins).

`src/com/validador/aces/comparison/ComplianceMetrics.java:47-49` throws
`IllegalArgumentException` if satisfied < 0 or > total.

Existing tests to keep green and imitate: `tests/ComparatorTest.java`
(in-memory data; helpers `buildMultiRequiredCatalog()`, `appWithAttrs(...)`;
shared `CMP = new Comparator("test")`), `tests/AttributeValidatorTest.java`,
`tests/CompositeValidatorTest.java`, `tests/ValidationIdempotenceTest.java`,
`tests/EndToEndTest.java`.

**Decision already taken (do not re-litigate):** duplicate product-line names
resolve **first-wins**, matching `Catalog.findProductByName` and the
single-application `compare` path.

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |

## Scope

**In scope**:
- `src/com/validador/aces/comparison/Comparator.java`
- `src/com/validador/aces/validation/AttributeValidator.java` (add an overload; keep the existing `validate(Application, Catalog)` behavior byte-for-byte)
- `src/com/validador/aces/tests/ComparatorTest.java` or a new `ComparatorEquivalenceTest.java` (+ `TestRunner.java` registration)

**Out of scope**:
- `Catalog.findProductByName` (keep as is, still used by the single-app path and tests).
- Wiring Enum/Range/DateFormat validators, `ComparisonCache`, any GUI code.
- Changing compliance semantics (invalid = count of MISSING_REQUIRED errors stays).
- Adding progress/cancel to `compareAll`.

## Steps

### Step 1: Pin current behavior with equivalence tests
Add tests (in-memory data, no files) that compare `CMP.compare(app, catalog)`
against the corresponding entry of `CMP.compareAll(apps, catalog)` for the
same inputs and assert equal `totalAttributes`, `validAttributes`,
`invalidAttributes`, error codes/attribute names, `partNumber`, optional
attribute lists. Cover: all required present; some missing; unclassified
product (code `PRODUCT_LINE_NOT_FOUND`, totals 0); blank product; product with
different case/padding (`"  mulTi PRODUCT "`).

**Verify**: Tests → new tests pass on the unmodified code (this is a
characterization baseline). If any fails, that is a pre-existing divergence:
STOP and hand back.

### Step 2: Add the failing duplicate-name test
Catalog with two lines named `"Dup"` (different ids) whose required sets
differ (line 1: `A`; line 2: `A`,`B`,`C`). Application with product `"dup"`
missing `A` only. Assert (first-wins): total = 1, invalid = 1, valid = 0 via
both `compare` and `compareAll`, and no exception from building
`ComplianceMetrics` from the result.

**Verify**: Tests → this new test FAILS on current code (total/invalid come
from different lines). Record the failure text in your report.

### Step 3: Resolve once, count from the same source
- `buildProductLineIndex`: first wins (`putIfAbsent`).
- Add an `AttributeValidator` method taking the already-resolved
  `ProductLine` and `ValidationSchema` (name it e.g. `validateResolved`) that
  holds the logic currently inline in `validate`; make
  `validate(Application, Catalog)` resolve via `findProductByName`, build the
  schema, and delegate. Behavior of the old method must not change.
- In `compareAll`'s per-application work, call the resolved-variant directly
  (no `CompositeValidator`, no `catalog.findProductByName`, no
  `new ValidationSchema` per app). Derive `invalid` from the same `missing`
  list that produced the errors, and `total` from the same schema, so
  `0 ≤ invalid ≤ total` by construction.
- Merge `compareInternal` and `compareInternalOptimized` into one private
  method taking `(application, catalog, line, schema)` where `line`/`schema`
  may be null; the single-application `compare` resolves them via the same
  normalization (`normalizeKey`) used by `compareAll` — keep `compare`'s
  public behavior and its exception messages for null args.
- Keep `stopOnFirstCriticalError` working if it has a public setter/getter
  that tests use: if you can no longer honor it because there is only one
  validator, keep the API and document it as a no-op for a single validator —
  do not delete public methods used by tests.

**Verify**: Compile → exit 0; Tests → step-1 tests still pass, the duplicate
test now passes, all pre-existing tests pass.

### Step 4: Prove the hot path is gone
Structural check: `Grep` for `findProductByName` and `new ValidationSchema`
under `src/com/validador/aces/comparison/` and `src/com/validador/aces/gui/`.

**Verify**: neither appears in the per-application code path of `compareAll`
(only in index/cache building and the single-app `compare` resolution).
Then run a one-off timing **outside the repo**: write a scratch class in
`$env:TEMP` that builds a catalog of 20 000 lines × 94 required attributes and
50 000 applications and times `compareAll`; report the number in your final
summary (no assertion, no committed benchmark). If it exceeds ~30 s, STOP and
hand back with the profile.

## Test plan

Step 1 equivalence tests (≥ 5), Step 2 duplicate-name test (1), pattern:
`ComparatorTest.java` (in-memory fixtures). Register any new class in
`TestRunner`. **Verify**: Tests → `TODOS PASAN`, no test removed or weakened.

## Done criteria

- [ ] Compile → exit 0; Tests → `TODOS PASAN`; test count = previous + new, none removed
- [ ] The duplicate-name test failed before Step 3 and passes after
- [ ] `AttributeValidator.validate(Application, Catalog)` outputs unchanged (its existing tests untouched and green)
- [ ] No per-application `findProductByName` / `new ValidationSchema` / `new CompositeValidator` in `compareAll`
- [ ] No files outside the in-scope list modified; timing figure reported

## STOP conditions

Stop if:
- Step 1 tests expose a divergence between `compare` and `compareAll` today.
- Dropping `CompositeValidator` from the batch path changes any existing test
  result (this suggests behavior depended on it — describe, don't paper over).
- `ComparisonResult`/`ValidationError` need changes to carry the same data.
- Real catalogs contain duplicate names with different required sets and the
  owner might prefer "merge" over "first wins" — that is a product decision:
  hand back with an example from the real catalog if you can detect one
  (read-only).

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

First-wins is now the single rule for duplicate names; the duplicate should
ideally be reported to the user (catalog warning) — deferred to a follow-up
since it needs a place in the GUI. `AttributeValidator.validateResolved`
exists so future validators (Enum/Range) can also consume pre-resolved lines
without re-scanning. A reviewer should check that totals for non-duplicate
data are bit-identical to before (the Step 1 tests do exactly this).
