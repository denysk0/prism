# Reproduce the benchmark

These are the exact prompts used for the Ktor #5412 context measurement.

## Setup

```bash
git clone https://github.com/ktorio/ktor.git /private/tmp/prism-demo/ktor-zstd
cd /private/tmp/prism-demo/ktor-zstd
git checkout 3fcfadd4508539bac0b102dd9c772e2522933d9d
```

## No-Prism run

"Fresh" Claude Code

```
cat reproduce/no_prism.txt | pbcopy
```

Open `claude` in `/private/tmp/prism-demo/ktor-zstd`, run `/context` (T0), paste the prompt, wait for Phase 1 plan, run `/context` again (T1). Record Messages tokens.

## Prism run

1. Install `prism-0.1.0.zip` into IntelliJ IDEA 2025.2+ (Settings → Plugins → gear → Install from Disk).
2. Open `/private/tmp/prism-demo/ktor-zstd` in IntelliJ. Wait for indexing.
3. Verify `/mcp` in Claude Code shows `mcp__jetbrains__get_context_capsule`.
4. Fresh Claude Code session:

```
cat reproduce/prism.txt | pbcopy
```

Run `/context` (T0), paste the prompt, wait for Phase 1 plan, run `/context` (T1). Record Messages tokens.

## What to record

- T0 and T1 `Messages` line from `/context`
- Number of `Read` / `Grep` / MCP calls in the transcript
- Whether the agent identified `Zstd.jvm.kt` + `decodeTo` and the test location

## Notes

- Paths are macOS (`/private/tmp` is canonical `/tmp`). Adjust for Linux.
- Both sessions used Claude Sonnet 4.6, Claude Pro subscription.
- The Prism prompt uses a minimal grep to locate the candidate line before calling `get_context_capsule`. It does not hardcode the line number, the agent finds it via one search, then calls Prism.
