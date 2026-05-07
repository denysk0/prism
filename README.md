# Prism

IntelliJ plugin that gives coding agents a token-budgeted view of the code they're actually working on. Aider's repomap, but IDE-native.

## What it does

`get_context_capsule(filePath, line, budget)` returns a JSON capsule with the target method, its class skeleton, callees, callers, and relevant types. Sections are priority-ranked and greedy-fit to the token budget. Anything that doesn't fit goes into `omitted` with a reason.

The Tool Window shows what the agent received so you can see it too.

Java and Kotlin are fully supported via [PSI](https://plugins.jetbrains.com/docs/intellij/psi.html) and [UAST](https://plugins.jetbrains.com/docs/intellij/uast.html).


## Benchmark

Ktor issue [#5412](https://github.com/ktorio/ktor/issues/5412) - `ClosedByteChannelException` in the zstd decoder. ~250k-LOC repo, bug in one 130-line file.

Two fresh Claude Code sessions, Sonnet 4.6. `Messages` tokens from `/context` after localization, before any edits:

| | No-Prism | Prism |
|---|---:|---:|
| Messages at T1 | 30.4k | 15.0k |
| Full-file reads | 6 | 0 |
| MCP calls | 0 | 1 |
| Found target method | yes | yes |
| Found test location | no | yes |

The Prism session used one grep to locate the candidate line, called `get_context_capsule` once, and stopped. Reproduce: [`reproduce/`](reproduce/).

### Capsule size (jtokkit cl100k\_base)

| File | LOC | Budget | Capsule tokens | Naive tokens | Saved | Omitted |
|---|---:|---:|---:|---:|---:|---|
| `SampleService.java` | 124 | 2000 | 515 | 807 | 36% | — |
| `SampleService.java` | 124 | 500 | 490 | 807 | 39% | callees |
| `LargeService.java` | 1676 | 2000 | 203 | 13143 | 98% | skeleton |
| `SampleKotlinService.kt` | 86 | 2000 | 552 | 781 | 29% | — |
| `SampleKotlinService.kt` | 86 | 500 | 493 | 781 | 37% | callees |

Naive baseline is the full source file. On large files the skeleton alone exceeds budget, so only the target method is returned regardless of budget.

## Install

Download `prism-*.zip` from [Releases](https://github.com/denysk0/prism/releases) or build from source: `./gradlew buildPlugin` → `build/distributions/prism-0.1.0.zip`.

Install in IDEA 2025.2+: Settings → Plugins → gear → Install Plugin from Disk.

Requires the bundled MCP Server plugin (IDEA 2025.2+ ships it).

## Usage

Once the JetBrains MCP server is connected in Claude Code, `get_context_capsule` is in the tool list. Pass `projectPath` explicitly if you have multiple IDE windows open.

```json
{
  "projectPath": "/path/to/repo",
  "filePath": "/path/to/repo/src/Zstd.jvm.kt",
  "line": 67,
  "budget": 2000
}
```

### Making the agent actually use it

The tool being available doesn't mean the agent will call it - by default agents reach for `Read`. You need to tell it to use Prism. Two options:

**Per-task prompt** - add one line to your task:

```
Use get_context_capsule before reading any production file.
```

**Project CLAUDE.md**. Put it there and the agent picks it up automatically in every session for that repo:

```markdown
## Context
When investigating code in this repo, call `get_context_capsule` before
reading production files. Pass the file and line of the suspicious method.
```

## Limitations

- JVM only (Java, Kotlin). Python/JS/Go/Rust/C++ are out of scope as UAST doesn't cover them.
- During IDE indexing callers unavailable, target + skeleton still work.

## Future work

- Non-JVM backends via tree-sitter.
- PageRank-style ranking on the PSI reference graph.
- Capsule diff between two calls.

---

License: MIT.
