# TODOs

Future improvements and follow-ups. Not commitments — a parking lot of ideas worth picking up later.

## Testing

1. ~~**Tests for `JTLContext` manual-section state machine.** `inManualCode`, `skipUntilManualSectionEnd`, `manualCodeKey`, and the interaction with `skippedLines` are subtle and currently untested.~~ Done — `JTLContextTest` (15 tests).
2. ~~**Tests for `JTLTemplate`** — `file`/`close`, `folder`, `manual_begin`/`manual_end`, `load_file`, `disable_backup`/`enable_backup`. These touch the filesystem so will need temp dirs.~~ Done — `JTLTemplateTest` (14 tests).
3.  **Test edge cases in the parsers** the current suite doesn't cover: nested arrays of objects in JSON; quoted values containing escape sequences in DEF; CSV with trailing semicolons; comments (`//`, `/* */`) in DEF input; UTF-8 non-ASCII identifiers.
4.  **Negative-path tests for `JTLC`** — unmatched `@>`, nested `@<`, unmatched `@[ … ]@`. Some of these currently print to stderr and continue rather than failing fast; decide which behavior is correct, then lock it in with a test.

