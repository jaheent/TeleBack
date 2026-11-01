<!-- trail:start -->
## Trail — repo context graph

This repo is indexed in `.trail/cache/`: small linked markdown nodes that explain each
system and carry exact file:line spans, kept in sync with the code through git.

For ANY task here — understanding how something works, finding where code lives,
or scoping a change — get context from the graph before grepping or opening
source files. Re-ask freely (it's cheap) and reuse literal identifiers you
already have (symbol, error string, file name) as the query. New to this repo?
Run `trail map` first — a token-budgeted orientation (dir clusters, hubs,
hotspots), no LLM, no key.

- Run `trail ask "<your question>" --source` → ranked nodes with the relevant
  code spans inlined (each hit's ≤8-line crux by default; `--full` for whole
  definitions when the crux isn't enough). Match the tool to the task shape:
  for understanding or editing, the top node IS the answer — cite its
  `covers:` file:line spans and edit straight from `--source`. For
  exhaustive tasks ("every occurrence / every caller of this pattern"), ranked
  results are top-N, not complete — run `trail grep "<literal>"` instead
  (exhaustive over indexed files, grouped by enclosing symbol), falling back
  to raw `grep -rn` only for unindexed files.
- `trail skeleton <file>` → every definition's signature + span, ~10× cheaper
  than reading the file; use it to skim an API surface.
- `trail callers <symbol>` gives precomputed, exact edges — who calls this.
  Add `--direction out` for what it calls, or `--depth N` to walk
  transitively for the full blast radius. For structural questions, skip
  ranking and use this directly.
- Or browse: `.trail/cache/INDEX.md` lists every node; follow the links.
- Monorepos and folders of multiple repos rank fairly across sub-projects —
  hits carry `[scope/]` labels naming which one they're from. Narrow with
  `trail ask "<task>" --in <scope>/` once you know where you're working.

If a returned span is truncated ("+N more lines"), open the file at that exact
range before finalizing. Only open source files when a node genuinely lacks a
needed detail, and then at the exact file:line the node points to — never
re-read whole files.

After big code changes, refresh the graph with `trail build` (deterministic,
no API key, $0).
<!-- trail:end -->

---

## Workspace Rules & Constraints

### Explicit Confirmation for Builds and Tests (MANDATORY)
- The agent is **STRICTLY FORBIDDEN** from running Gradle builds (`gradlew assembleDebug`, `gradlew build`, etc.), compilation tasks, unit tests, or end-to-end test suites automatically on its own initiative.
- The agent **MUST ALWAYS** ask the user for explicit confirmation in chat and receive direct approval before executing any build or test command.
- Proposing code changes, design drafts, or vector drawables does NOT grant permission to build or compile. Wait for the user's explicit command to test or build.

### Version Increment & Changelog Update on Every Release Build (MANDATORY)
- Whenever the user requests a release build (`assembleRelease`), the agent **MUST** increment the version in `app/build.gradle.kts` **before** building:
  - Increment the decimal portion of `versionName` by `+0.1` (e.g., `"2.7"` → `"2.8"`), producing `TeleBack_v<versionName>.apk`.
  - Increment `versionCode` by `+1` (e.g., `9` → `10`).
- Every version bump **MUST** be accompanied by a new `## [<versionName>.0] - YYYY-MM-DD` section at the top of `CHANGELOG.md` documenting all added, changed, and fixed items in that build while preserving all prior version sections intact.

