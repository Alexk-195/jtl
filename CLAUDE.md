# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JTL (Java Template Language) is a template processor that generates arbitrary textual output (typically source code) from `.jtl` template files combined with data definition files (`.def`, `.json`, `.csv`). Projects are described in `.jtlp` files.

## Build & Run

```bash
# Compile all source and package JTL.jar
./build/build.sh

# Run a project (execute all templates in the project file)
./build/jtl.sh examples/project_1.jtlp

# Run with flags
./build/jtl.sh --verbose examples/project_1.jtlp
./build/jtl.sh --skip_compile examples/project_1.jtlp   # skip template-to-Java compilation
./build/jtl.sh --skip_generate examples/project_1.jtlp  # skip Java execution
```

There are no automated tests. Manual verification is done by running the examples and inspecting their output.

## Architecture

### Two-stage pipeline

1. **Compile**: `JTLC` converts each `.jtl` template to a Java source file (a class extending `JTLTemplate`).
2. **Generate**: The compiled class is run; it reads the definition data and writes output files.

`JTL.java` orchestrates both stages for every template listed in the `.jtlp` project file.

### Source files (`src/`)

| File | Role |
|---|---|
| `JTL.java` | Entry point; reads `.jtlp`, drives compile+run loop |
| `JTLC.java` | Translates `.jtl` → Java source |
| `JTLTemplate.java` | Base class for all generated templates; exposes `println`, `file`, `folder`, `manual_begin/end`, etc. |
| `JTLEntity.java` | Tree node for parsed definition data (`name`, `params`, `children`, `parent`) |
| `JTLDefinitionParser.java` | Parses DEF / JSON / CSV into `JTLEntity` trees |
| `JTLContext.java` | Shared execution state (version, manual-section tracking, output writer) |
| `JTLResultWriter.java` | Writes output files; extracts and re-inserts preserved manual sections, creates `.bak` backups |
| `JTLOut.java` | UTF-8 wrapper around `System.out`/`System.err` |

### Template syntax (`.jtl` files)

- `@<` … `@>` — multi-line Java control block
- `@[` … `@]` — inline Java expression (result is printed)
- `@` at start of line — single-line Java statement
- All other lines are emitted verbatim as output

An optional `.jtl_header` file next to a template can inject extra Java `import` statements into the generated class.

### Definition formats

All formats parse into the same `JTLEntity` tree:

- **DEF** (native): `entity("p1","p2") { child { ... } }`
- **JSON**: Arrays become `_array_` entities; elements become `_elem_` children
- **CSV**: Root is `csv`; each row is an `_elem_` with positional params

### Manual sections

`JTLResultWriter` preserves blocks between `//--jtl--@id@--begin--` and `//--jtl--@id@--end--` markers across regenerations. Templates can customize the markers with `manual_patterns(beginPattern, endPattern)`.

## Build system notes

- Pure `javac` — no Maven/Gradle. Java 8 target (`--release 8`).
- `build.sh` compiles every `.java` in `src/` then packages them with `MANIFEST.MF` into `JTL.jar`.
- At runtime `jtl.sh` locates `JTL.jar` relative to the script, then calls `javac`/`java` for each template.
