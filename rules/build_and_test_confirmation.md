# TeleBack Project Rules

## Execution Constraints & User Confirmations

1. **Explicit Confirmation for Builds and Tests (MANDATORY)**:
   - The agent is **STRICTLY FORBIDDEN** from running Gradle builds (`gradlew assembleDebug`, `gradlew build`, etc.), compilation tasks, unit tests, or end-to-end test suites automatically on its own initiative.
   - You **MUST ALWAYS** ask the user for explicit confirmation in chat and receive direct approval before executing any build or test command.
   - Proposing code changes, design drafts, or vector drawables does NOT grant permission to build or compile. Wait for the user's explicit command to test or build.

2. **Version Increment & Changelog Update on Every Release Build (MANDATORY)**:
   - Whenever the user requests a release build (`assembleRelease`), you **MUST** increment the version in `app/build.gradle.kts` **before** building:
     - Increment the decimal portion of `versionName` by `+0.1` (e.g., `"2.7"` → `"2.8"`), producing `TeleBack_v<versionName>.apk`.
     - Increment `versionCode` by `+1` (e.g., `9` → `10`).
   - Every version bump **MUST** be accompanied by a new `## [<versionName>.0] - YYYY-MM-DD` section at the top of `CHANGELOG.md` documenting all added, changed, and fixed items in that build while preserving all prior version sections intact.
