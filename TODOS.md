# TODOs

Future improvements and follow-ups. Not commitments — a parking lot of ideas worth picking up later.

## Testing

- **Golden-file integration tests.** Drive `examples/project_1.jtlp` end-to-end (compile + generate) into a temp output dir and diff against a checked-in `expected/` tree. Provide a `-Dupdate=true` (or `./build/test.sh --update`) escape hatch to regenerate goldens after intentional changes. Highest-value next step — catches integration bugs the current unit tests miss (codegen + classloading + writer interaction).
- **Tests for `JTLContext` manual-section state machine.** `inManualCode`, `skipUntilManualSectionEnd`, `manualCodeKey`, and the interaction with `skippedLines` are subtle and currently untested.
- **Tests for `JTLTemplate`** — `file`/`close`, `folder`, `manual_begin`/`manual_end`, `load_file`, `disable_backup`/`enable_backup`. These touch the filesystem so will need temp dirs.
- **Test edge cases in the parsers** the current suite doesn't cover: nested arrays of objects in JSON; quoted values containing escape sequences in DEF; CSV with trailing semicolons; comments (`//`, `/* */`) in DEF input; UTF-8 non-ASCII identifiers.
- **Negative-path tests for `JTLC`** — unmatched `@>`, nested `@<`, unmatched `@[ … ]@`. Some of these currently print to stderr and continue rather than failing fast; decide which behavior is correct, then lock it in with a test.
- **CI integration.** A GitHub Actions workflow that runs `./build/build.sh && ./build/test.sh` on push would be a small lift now that there's something to run.

## Known quirks worth fixing or documenting more loudly

- **`JTLResultWriter.identicalFiles()` is whitespace-sensitive.** It compares the in-memory `Lines` vector (exact strings passed to `write`) against the file content re-read via `readLine()` (terminators stripped). Works today only because `JTLContext.println` happens to call `twriter.append(c)` without a trailing `\n`. If anything ever passes embedded newlines through `write`, every regeneration will look "Modified" and produce a spurious `.bak`. Possible fixes:
  - Normalize both sides (strip trailing newlines per element before comparing), or
  - Compare the final byte content that would be written instead of the in-memory line lists.
- **`@[expr]@` is silently ignored inside `@< … @>` blocks.** The README and CLAUDE.md now document this, but the compiler could detect it and emit a warning rather than producing broken Java.
- **`JTLC` reports parse errors to stderr but does not stop the pipeline.** A malformed `.jtl` produces a half-written `.java` file that then fails at `javac` time with a less obvious message. Surfacing parse errors as a hard failure in `JTL.java` would shorten the feedback loop.

## Refactoring opportunities (low priority)

- `JTLDefinitionParser` and `JTLResultWriter` only accept filenames. Adding `Reader`/`Writer` constructors would make them easier to unit-test without temp files (and easier to embed). The current tests use temp files, which works but is slower and clutters `/tmp`.
- `JTLEntity` exposes its `Vector<String> params` and `Vector<JTLEntity> children` as public mutable fields. Encapsulating these behind methods would make refactoring (e.g. switching to `ArrayList`) a non-breaking change.
- Consider replacing the hand-rolled `TestHarness` with JUnit 5 once a build system (Gradle/Maven) is in scope. Not worth the dependency churn today.
