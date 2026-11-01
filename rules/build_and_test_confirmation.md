# TeleBack Project Rules

## Execution Constraints & User Confirmations

1. **Explicit Confirmation for Builds and Tests (MANDATORY)**:
   - The agent is **STRICTLY FORBIDDEN** from running Gradle builds (`gradlew assembleDebug`, `gradlew build`, etc.), compilation tasks, unit tests, or end-to-end test suites automatically on its own initiative.
   - You **MUST ALWAYS** ask the user for explicit confirmation in chat and receive direct approval before executing any build or test command.
   - Proposing code changes, design drafts, or vector drawables does NOT grant permission to build or compile. Wait for the user's explicit command to test or build.
