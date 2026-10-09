# Plan 008: Have exactly one configuration source, and make its setting actually work

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on.
> If anything in "STOP conditions" occurs, stop and write a handback —
> do not improvise. When done, update this plan's status row in
> `plans/aces-hardening/README.md`. Do not `git commit`/`git push` unless the
> user asks; leave changes in the working tree.
>
> **Drift check (run first)** (PowerShell, repo root):
> `git diff --stat d185ef8 -- src/application.properties src/com/validador/aces/gui/ValidationPanel.java src/com/validador/aces/gui/ConfigDialog.java`
> If in-scope files changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Effort**: S
- **Risk**: LOW
- **Depends on**: 006-invalidate-stale-results.md (both edit `ValidationPanel.java`)
- **Planned at**: revision `d185ef8` (branch `fix/UI-UX`), 2026-10-09

## Why this matters

`src/application.properties` declares ~40 settings (validators, cache, theme,
audit file, max file size, API/DB placeholders…) and says "Este archivo se
carga automáticamente al iniciar la aplicación". **Nothing loads it.** The
real configuration is the Settings dialog, which stores two keys in
`~/.validador_aces_config.properties`; one of them (`maxTableRows`) is also
ignored because `ValidationPanel` hardcodes `10_000`. Users and admins who
edit either file see no effect, and settings such as `file.max-size-mb` and
`validation.active-validators=…range,enum,date-format` suggest protections
and features that do not exist. After this plan there is one real config
source and the one setting the UI exposes works.

## Current state

- `src/application.properties` (181 lines): sections for validation, cache,
  report, GUI, audit, logging, files, export, DB/API (commented), advanced,
  app info. A grep for `application.properties` and `getResourceAsStream` in
  `src/**/*.java` finds **no reader**. `build.xml:71` would include
  `**/*.properties` from `bin/` into the jar, but `javac` does not copy
  resources, so the file is not even packaged.
- `src/com/validador/aces/gui/ConfigDialog.java`:
  - lines 48-56: `CONFIG_FILE = new File(user.home, ".validador_aces_config.properties")`,
    keys `maxTableRows` (default 10 000) and `defaultExportDir`.
  - lines 89-97: `public static int getMaxTableRows()` (**zero callers**) and
    `public static String getDefaultExportDir()` (used by `AuditPanel.java:174`).
  - the spinner is `new SpinnerNumberModel(maxRows, 1_000, 50_000, 1_000)`.
- `src/com/validador/aces/gui/ValidationPanel.java:63`:
  `private static final int MAX_TABLE_ROWS = 10_000;` used at lines ~373, 390,
  423, 425 (row cap for the results table and its "mostrando primeros N" label).
- Docs that mention `application.properties`: README.md (lines 29, 193), and
  many root `.md` files (Plan 009 rewrites/archives docs — do **not** edit docs
  here).

**Decision already taken**: keep the Settings-dialog file as the single source;
delete `src/application.properties`. (It is recoverable from git history if the
owner later wants a classpath-defaults file.)

## Commands you will need

PowerShell, repo root.

| Purpose | Command | Expected on success |
|---|---|---|
| Compile | `$out="$env:TEMP\aces-out"; New-Item -ItemType Directory -Force $out \| Out-Null; javac --release 11 -encoding UTF-8 -cp "lib/*" -d $out (Get-ChildItem -Recurse src -Filter *.java).FullName` | exit 0 |
| Tests | `java "-Dfile.encoding=UTF-8" -cp "lib\*;$out" com.validador.aces.tests.TestRunner` | `TODOS PASAN`, exit 0 |
| No reader check | `Select-String -Path (Get-ChildItem -Recurse src -Filter *.java).FullName -Pattern 'application\.properties','getResourceAsStream'` | no matches |

## Scope

**In scope**:
- `src/com/validador/aces/gui/ValidationPanel.java` (use the configured row cap)
- `src/application.properties` (delete)
- `src/com/validador/aces/gui/ConfigDialog.java` (only if needed: a Javadoc note that this is the single config source)

**Out of scope**:
- Implementing any other declared setting (theme, audit log persistence, size limit, validators) — those are product directions, not bugs.
- Documentation edits (Plan 009).
- Changing the dialog's UI.

## Steps

### Step 1: Re-verify nothing reads `application.properties`
Run the "No reader check" command, and `Grep` `build.xml`/`build.bat` for it.

**Verify**: no matches in Java sources; `build.xml:71` (`**/*.properties`) is a
generic include and is fine. If any code reads it → STOP.

### Step 2: Make `maxTableRows` effective
Replace the `MAX_TABLE_ROWS` constant in `ValidationPanel` by a value read once
per results rendering from `ConfigDialog.getMaxTableRows()` (read at the start
of `populateUI`, store in a local `final int maxRows`, use it in all four
places, including the "mostrando primeros N" label). Do not read the file per
row. Keep the 10 000 default (already `DEFAULT_MAX_ROWS`).

**Verify**: Compile → exit 0; `Select-String` for `MAX_TABLE_ROWS` in
`ValidationPanel.java` → no matches (or only the Javadoc, updated).

### Step 3: Delete the dead file
Delete `src/application.properties`.

**Verify**: Compile → exit 0; Tests → `TODOS PASAN`; the app starts
(`java -cp "lib\*;$out" com.validador.aces.Launcher` opens the window — it never
loaded the file).

### Step 4: Manual smoke (record the result)
Launch the app, run an audit with a result set larger than the cap (use test
writers to produce, e.g., 3 000 applications with a low cap by setting
`maxTableRows` to 1 000 in the Settings dialog): the table shows 1 000 rows and
the label says "(mostrando primeros 1 000)". Restore the setting afterwards.

**Verify**: observed as described; note that the dialog spinner bounds are
1 000–50 000.

## Test plan

No unit test (GUI code). If you extract the "cap rows" decision into a pure
function, add a test in `tests/`; otherwise the smoke check in Step 4 is the
verification and you say so. **Verify**: Tests → `TODOS PASAN`.

## Done criteria

- [ ] Compile → exit 0; Tests → `TODOS PASAN`
- [ ] No code reads `application.properties`; the file is gone
- [ ] `ValidationPanel` honors `ConfigDialog.getMaxTableRows()` (smoke Step 4 reported)
- [ ] No files outside the in-scope list modified (docs untouched)

## STOP conditions

Stop if:
- Any code path (including scripts or docs the build relies on) loads `application.properties`.
- Reading config on every `populateUI` is slow (it re-reads the user file each call; should be negligible) — report if measurable.
- The owner's intent for the declared-but-unimplemented settings is unclear and
  they might want the file kept as a roadmap — hand back; the deletion is the
  only irreversible-looking step (recoverable from git).

On stopping, write a **handback**: current state, desired outcome, lingering
questions. Descriptive, not prescriptive.

## Maintenance notes

Future settings go through `ConfigDialog` (add a key + default + dialog field +
static getter). If a real defaults file is wanted later, load it explicitly at
startup from the classpath and add an `ant` step that copies resources into
`bin/`. Docs still describing `application.properties` are fixed in Plan 009.
