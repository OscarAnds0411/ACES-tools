# Plan 001: Make the test suite a reliable gate (fails loudly, skips cleanly without real data)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/com/validador/aces/tests build.xml`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S–M
- **Risk**: LOW
- **Depends on**: none
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

The project's only safety net is a hand-rolled runner (`TestRunner`) and every
later plan in this effort relies on it. Today it can report success while
running nothing, `ant test` can never fail the build (`failonerror="false"`),
test methods run in unspecified order, and ~15 tests read gitignored real
`.xlsx` files (`catalogoDb/`, `ACES/`) so they would *error* on any machine
without those files. After this plan a green run means something, and a run
without the real data reports those tests as SKIPPED instead of failing.

## Current state

Language/tooling: Java 11 source level, Ant build (`build.xml`), custom tests
in `src/com/validador/aces/tests/` (no JUnit). Baseline on the planning
machine: **95 tests, 95 passed**, JDK 25 (`javac --release 11` works). Ant is
**not** on PATH on this machine; `build.bat` hardcodes `C:\opt\ant\bin\ant.bat`.

`src/com/validador/aces/tests/TestRunner.java`:
- Lines 31-68: a hand-maintained list of `runSuite("...", XTest.class)` calls.
- Lines 88-100 (`runSuite`): iterates `suiteClass.getDeclaredMethods()` (order
  unspecified), keeps `public static void test*()` with 0 params, silently
  ignores everything else. A suite with zero matching methods prints nothing
  and counts as success.
- Lines 79-83: `System.exit(1)` only when `failed > 0`.
- Lines 102-118 (`runTest`): catches `InvocationTargetException` and any
  `Throwable` → counts as failed.

`src/com/validador/aces/tests/Assert.java`: assertion helpers
(`assertEquals`, `assertTrue`, `assertNotNull`, `assertThrows`, and the
`ThrowingRunnable` interface used by the runner). Read it fully before editing.

`build.xml:112-125` (`test` target): `<java classname="com.validador.aces.tests.TestRunner" fork="true" dir="${basedir}" failonerror="false">`.

Tests that need real data (paths are relative to the repo root, so cwd must
be the repo root):
- `CatalogParserTest.java:20` → `catalogoDb/Atributos ACES por línea de producto.xlsx`
- `ApplicationParserTest.java:17` and `EndToEndTest.java:30-32` → `ACES/ACES Keep on Green 23.09.2026 - Copy.xlsx` (and a sibling file under `ACES/`)
- `.gitignore` ignores `*.xlsx`, so these files are never in a clone.

Convention to match: tests are `public static void testXxx()` methods in
`public final class XxxTest` with a private constructor, using `Assert.*`;
round-trip tests build their own temp `.xlsx` with fastexcel (exemplar:
`CatalogRoundTripTest.java:95-127`).

## Commands you will need

Run these in **PowerShell** from the repo root (Git Bash splits the `;`
classpath separator).

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | no output, exit 0 |
| Tests (repo root cwd) | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | summary `TODOS PASAN`, `$LASTEXITCODE` = 0 |
| Tests, "fresh clone" simulation | `$abs=(Resolve-Path lib).Path; Push-Location $env:TEMP; java "-Dfile.encoding=UTF-8" -cp "$abs\*;$out" com.validador.aces.tests.TestRunner; Pop-Location` | real-data tests listed as SKIPPED, no failures, exit 0 |
| Ant (only if available) | `& C:\opt\ant\bin\ant.bat test` | BUILD SUCCESSFUL |

(Console may show `?` for accents because of the Windows code page; that is
cosmetic.) The "fresh clone" run uses a different working directory, so
relative paths like `ACES/...` do not resolve — it never touches the user's
data files.

## Scope

**In scope**:
- `src/com/validador/aces/tests/TestRunner.java`
- `src/com/validador/aces/tests/Assert.java` (add a skip mechanism)
- `src/com/validador/aces/tests/CatalogParserTest.java`, `ApplicationParserTest.java`, `EndToEndTest.java` (guard real-data access only)
- `build.xml` (the `test` target only)

**Out of scope**:
- Any production code under `src/com/validador/aces/` outside `tests/`.
- Adding JUnit or any dependency.
- Rewriting weak tests (e.g. `ExcelReportGeneratorTest.testDefaultReportFileFor_correctSuffix`) — deferred.
- `.gitignore`, `lib/`, `build.bat`, `CLAUDE.md` — Plan 002.

## Steps

### Step 1: Record the baseline
Run Compile then Tests. Note the exact final line (`95 tests: 95 passed`).

**Verify**: Tests → `TODOS PASAN`, 95 passed, 0 failed. If not 95/0, STOP.

### Step 2: Add a skip mechanism
In `Assert.java` add an unchecked `TestSkippedException` (nested static class)
and a helper such as `Assert.assumeFileExists(File f)` that throws it with a
message naming the missing file. In `TestRunner.runTest`, catch it (also
unwrapped from `InvocationTargetException.getCause()`) → print `⊘ name (omitido: <msg>)`,
increment a new `skipped` counter, do **not** increment `failed`. Include the
skipped count in the summary line.

**Verify**: Compile → exit 0.

### Step 3: Guard real-data tests
At the start of each test method (or in one shared private helper) in
`CatalogParserTest`, `ApplicationParserTest`, `EndToEndTest` that opens
`catalogoDb/...` or `ACES/...`, call `Assert.assumeFileExists(...)` for the
file it needs. Do not change any assertion.

**Verify**: Compile → exit 0; Tests (repo root cwd) → same 95 passed, 0 skipped.
Then the "fresh clone" simulation → those tests show as SKIPPED (expect roughly
15), `failed` = 0, exit 0.

### Step 4: Make the runner fail on silent-nothing and run deterministically
In `TestRunner.runSuite`:
- sort the discovered methods by name before running;
- if a suite finds zero runnable `test*` methods, record a failure
  ("suite sin tests: <name>") — this catches renamed/non-static methods;
- additionally print a `WARNING` (not failure) for any `public` method named
  `test*` that was skipped because it is non-static or has parameters.

**Verify**: Tests → still 95 passed. Temporarily (in your head, not in the
repo) confirm by reading that an empty suite would now `failed++`; do not
commit a deliberately empty suite.

### Step 5: Make `ant test` fail on test failure
`build.xml` `test` target: `failonerror="true"` on the `<java>` task. Update
the target's description/comment (drop the stale "(opcional)").

**Verify**: if Ant is available, `ant test` → BUILD SUCCESSFUL. If Ant is not
available, state that explicitly in your final report; do not claim this step
was run. The runner's exit code behavior is already verified by
`$LASTEXITCODE` in Step 1/3.

## Test plan

No new product tests. Behavior is verified by the command runs above
(repo-root run, fresh-clone simulation) plus one deliberate-failure check:
add a throwaway `public static void testZZ_fails() { Assert.assertTrue(false, "x"); }`
to any existing suite in your working tree, recompile, run the tests and
confirm `$LASTEXITCODE` is 1; then remove it, recompile and confirm exit 0.

## Done criteria

- [ ] Compile → exit 0
- [ ] Tests from repo root → `TODOS PASAN`, 95 passed, 0 failed, exit 0
- [ ] Fresh-clone simulation → 0 failed, real-data tests reported as skipped, exit 0
- [ ] A deliberately failing test makes the runner exit 1 (then removed)
- [ ] `git status --short` shows changes only under `src/com/validador/aces/tests/` and `build.xml` (plus `plans/`)

## STOP conditions

Stop if:
- The baseline is not 95/0 (someone else broke something — hand back, don't fix).
- A "real-data" test turns out to need more files than the ones listed (record which and hand back).
- You find test classes not registered in `TestRunner` that you believe should be — report, do not silently register (could change the baseline).
- Fixing a step seems to require touching production code.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

Every later plan adds tests and registers new classes in `TestRunner`; the
zero-test check makes forgetting a method visible. Reviewers should check that
SKIPPED stays rare on a machine that has the data. Deferred: scanning the
package to auto-register suites; replacing the runner with JUnit.
