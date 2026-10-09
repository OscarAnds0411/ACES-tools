# Plan 002: Make a fresh clone buildable and document the verified commands in CLAUDE.md

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- build.xml build.bat manifest.txt .gitignore`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S
- **Risk**: LOW
- **Depends on**: 001-reliable-test-gate.md
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

A fresh `git clone` cannot compile: the 8 jars in `lib/` are untracked
(`.gitignore` has `*.jar`, and the later `!lib/` line does not re-include the
files inside), and nothing documents where they come from. The jar names are
also duplicated by hand in `build.xml:57` and `manifest.txt:6`, `build.bat`
only works if Ant lives at `C:\opt\ant`, and there is no `CLAUDE.md`, so an
agent (or newcomer) has no authoritative build/test command. After this plan,
a clone builds, the jar list lives in one place, and the commands verified in
Plan 001 are written down.

## Current state

- `lib/` contains: `aalto-xml-1.4.0.jar`, `commons-compress-1.28.0.jar`,
  `commons-io-2.20.0.jar`, `commons-lang3-3.18.0.jar`, `fastexcel-0.20.2.jar`,
  `fastexcel-reader-0.20.2.jar`, `opczip-1.2.0.jar`, `stax2-api-4.2.2.jar`.
  `git ls-files lib` prints nothing today.
- `.gitignore`: contains `*.jar`, `bin/`, `dist/`, `docs/`, `*.xlsx`; near the
  end it has `!build.xml`, `!lib/`, `!manifest.txt`, `!build.bat`. Git cannot
  re-include a file whose parent *directory pattern* is not the exclusion, so
  `!lib/` does nothing for `lib/*.jar`.
- `build.xml`:
  - line 8: `<property name="test.dir" value="tests"/>` — never referenced.
  - ~line 57: the `jar` target writes a manifest whose `Class-Path` is a
    hardcoded string of the 8 jar names.
  - the `jar` target also copies `lib/*.jar` into `dist/lib`.
  - `clean` deletes `bin`, `dist` **and `docs`**.
- `manifest.txt` (root): static manifest with `X-Build-Date: 2024-01-01`; the
  Ant build generates its own manifest, so this file looks unused — confirm by
  grep before deleting.
- `build.bat:7`: `set "ANT_BIN=C:\opt\ant\bin\ant.bat"`; the script exits with
  an error if that path is missing.
- Target Java level: 11 (`source`/`target` in `build.xml`). Planning machine
  has JDK 25; `--release 11` works.
- No `CLAUDE.md` or `AGENTS.md` exists. No CI.

Convention: Spanish user-facing text in Ant `echo`/`description` and `.bat`
output; keep that.

## Commands you will need

PowerShell, repo root. `$out` as defined below.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |
| Jars visible to git | `git status --short lib` | 8 lines starting `??` (not hidden) |
| Jars not ignored | `git check-ignore -v lib/fastexcel-0.20.2.jar; $LASTEXITCODE` | no match output, exit code 1 |
| Ant (if present) | `& C:\opt\ant\bin\ant.bat jar` | BUILD SUCCESSFUL, `dist\validador-aces.jar` exists |

## Scope

**In scope**:
- `.gitignore` (add the jar exception only)
- `build.xml` (Class-Path generation, drop unused `test.dir`, fix stale "(cuando estén listos)" text if present)
- `build.bat` (Ant discovery)
- `manifest.txt` (delete, if unused)
- `lib/README.md` (new: jar names, versions, upstream project, "licenses: Apache-2.0 for commons-*/fastexcel/aalto/opczip — verify before redistribution")
- `CLAUDE.md` (new)

**Out of scope**:
- Upgrading any jar version; Maven/Ivy migration (deferred, see Maintenance notes).
- `build.xml` `test` target (Plan 001 owns it).
- Documentation consolidation (Plan 009).

## Steps

### Step 1: Let git see the jars
Add `!lib/*.jar` **after** the `*.jar` rule (put it with the existing `!lib/`
line at the end). Do not `git add`/commit.

**Verify**: `git status --short lib` → 8 `??` lines; `git check-ignore -v lib/fastexcel-0.20.2.jar` → no output, exit code 1.

### Step 2: One source of truth for the jar list
In `build.xml`, generate the manifest `Class-Path` from the contents of
`lib/*.jar` (Ant `<pathconvert>` over a `<fileset>` with a space separator and
a `lib/` prefix mapper, or equivalent) instead of the hardcoded string. Then
confirm `manifest.txt` is not referenced anywhere (`Grep manifest.txt` across
the repo, ignoring `plans/`) and delete it. Remove the unused `test.dir`
property.

**Verify**: if Ant is available, `ant jar` → BUILD SUCCESSFUL and
`jar xf`/`unzip -p dist\validador-aces.jar META-INF/MANIFEST.MF` shows all 8
jars in `Class-Path`. If Ant is not available, state that and verify by
reading the XML carefully; do not claim it ran.

### Step 3: Make `build.bat` find Ant
Resolve Ant in this order: `%ANT_HOME%\bin\ant.bat`, `ant.bat` on PATH
(`where ant.bat`), then the current `C:\opt\ant\bin\ant.bat`. Keep the Spanish
help/error text and the existing targets table.

**Verify**: `cmd /c build.bat help` → prints help, exit 0. With none of the
three present the script must still print the "no encontrado" error and exit 1
(check by reading; do not uninstall anything).

### Step 4: Write `lib/README.md` and `CLAUDE.md`
`CLAUDE.md` (short, factual) must contain: project one-liner; Java 11 source
level; the exact Compile and Tests commands from this plan (PowerShell) and
the Ant equivalents; "tests expect cwd = repo root; tests that need
`catalogoDb/`/`ACES/` real data are SKIPPED when absent (Plan 001)"; package
map (`models, parsers, validation, comparison, reporting, gui, tests`);
conventions (Spanish UI/Javadoc, tests are `public static void testXxx()` in
`*Test` classes registered in `TestRunner`, no JUnit); "do not edit generated
dirs `bin/`, `dist/`".

**Verify**: Compile and Tests commands copied from `CLAUDE.md` run as written
→ exit 0 / `TODOS PASAN`.

## Test plan

No new tests. Verification is the command table above.

## Done criteria

- [ ] `git status --short lib` → 8 `??` entries and `git check-ignore` exit code 1
- [ ] Compile and Tests succeed (`TODOS PASAN`)
- [ ] `build.xml` no longer contains the literal jar-name string; `manifest.txt` gone; `test.dir` gone
- [ ] `build.bat help` works and Ant lookup follows the order above
- [ ] `CLAUDE.md` and `lib/README.md` exist; commands in `CLAUDE.md` were run as written
- [ ] No files outside the in-scope list modified

## STOP conditions

Stop if:
- `manifest.txt` is referenced by `build.xml`, `build.bat` or any script.
- Committing the jars looks unwelcome (repo owner policy unknown): leave them
  untracked-but-visible and say so; do **not** `git add` them.
- A jar's license cannot be established — still write `lib/README.md` but
  mark it "unverified" and hand back.
- Ant generation of `Class-Path` requires a newer Ant than the user's.

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

Upgrading a jar now means replacing one file in `lib/` and updating
`lib/README.md`. Deferred: moving to Ivy/Maven (bigger change), a vulnerability
check of the jar versions (not verified during planning; commons-lang3 3.18.0
is believed to contain a CVE-2025-48924 fix — confirm), and CI (blocked on the
real-data dependency being synthetic — Plan 001 only skips those tests).
