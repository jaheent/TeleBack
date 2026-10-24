# TeleBack — Completed Phases Roadmap (v2.6)

> **TeleBack** is an Android 15+ (API 35) CLI-inspired decentralized personal backup, restore, and sync engine powered by private Telegram groups with authenticated on-device AES-256-GCM encryption.

---

## Phase 1: Core Engine, Storage & Telegram Ingestion `[COMPLETED]`

- **1.1 Telegram Bot API Integration**
  - Direct OkHttp streaming client (`TelegramApiClient.kt`).
  - Multipart document streaming for chunk uploads up to Telegram limits.
  - Streaming file byte download with real-time transfer progress callbacks.
- **1.2 Android Storage Access Framework (SAF)**
  - Scoped storage directory tree traversal (`SafStorageHelper.kt`).
  - Persistable URI permissions across app restarts and device reboots.
  - Recursive file scanner calculating file paths, sizes, and SHA-256 digests.
- **1.3 Cryptography & Security Engine**
  - Authenticated AES-256-GCM encryption with 12-byte IVs (`CryptoEngine.kt`).
  - PBKDF2 key derivation (100,000 iterations, SHA-256) for passphrase-based master keys.
  - Standard binary file header with 4-byte magic signature (`TBK1`) and prepended IV.
- **1.4 Local Persistence & Metadata**
  - Room database architecture (`AppDatabase.kt`).
  - Snapshot manifest cache table (`SnapshotDao.kt`, `SnapshotEntity.kt`).
  - Jetpack DataStore Preferences for tokens, IDs, paths, and preferences (`AppPreferences.kt`).

---

## Phase 2: CLI Terminal Interface & Interaction `[COMPLETED]`

- **2.1 Monospace Terminal Log Stream**
  - Categorized log levels (`INFO`, `OK`, `WARN`, `ERR`, `SNAP`, `PLAIN`) (`TerminalLog.kt`).
  - Jetpack Compose ANSI-inspired terminal log viewport with auto-scroll pinning (`TerminalLogView.kt`).
  - Built-in compact 3dp retro scrollbar with dynamic offset positioning.
- **2.2 Centralized Command Grid & Sub-Menus**
  - Numbered and alias CLI command routing (`[1]` to `[8]`, `[0]`) (`CliCommand.kt`).
  - High-density 2-column command grid (`CommandGrid.kt`).
  - Centralized `Settings` command `[8]` with a 0.5x retro cyber-glitch transition and dedicated `[<] Back` navigation.
  - Interactive snapshot tagging prompt (`SnapshotPromptDialog.kt`) for human-readable restore identifiers.
- **2.3 Status Header & Telemetry Badges**
  - Live network connectivity monitor (`ConnectivityObserver.kt`).
  - Read-only horizontally scrollable status badges (`BOT`, `GROUP`, `DIR`, `AUTO`, `CIPHER`) (`TerminalHeader.kt`).

---

## Phase 3: Large File Streaming & Network Resilience `[COMPLETED]`

- **3.1 19 MB Chunk Streaming**
  - Fixed 19 MB chunk partitioning with memory-safe buffer streaming (`FileChunker.kt`).
  - Per-chunk SHA-256 verification and atomic multi-part reassembly.
  - `.tbpart` chunk naming convention for multi-part file representations.
- **3.2 Resume Journal & Crash Recovery**
  - Room persistent pending upload journal (`PendingUploadDao.kt`, `PendingUploadEntity.kt`).
  - Individual chunk checkpoints enabling transfer resumption without re-uploading completed parts.
- **3.3 Network Fault Tolerance & Throttling**
  - Telegram HTTP 429 flood-wait delay parsing and exponential backoff retry policy (`RetryPolicy.kt`).
  - Offline auto-wait listener: pauses on connectivity loss and automatically resumes when restored.

---

## Phase 4: Onboarding, Setup Wizard & Visual Walkthrough `[COMPLETED]`

- **4.1 First-Run Welcome Modal**
  - Welcome dialog outlining decentralized personal cloud architecture (`WelcomeDialog.kt`).
- **4.2 Interactive 3-Step Setup Wizard**
  - Bot token entry with direct `@BotFather` intent links (`OnboardingDialog.kt`).
  - Auto-detection of group Chat ID from recent message updates.
  - SAF target folder binding with persistent permissions.
- **4.3 Vector-Based Visual Setup Guide**
  - Visual setup walkthrough dialog (`SetupWalkthroughDialog.kt`) illustrating bot creation, private group creation, admin permissions, and initialization.

---

## Phase 5: Storage Intelligence & Snapshot Retention `[COMPLETED]`

- **5.1 Offline Cloud Usage Telemetry**
  - Local usage calculator (`UsageCalculator.kt`) computing stored bytes, deduplicated files, snapshot count, and encryption overhead entirely offline.
- **5.2 Rolling 10-Snapshot Retention & Orphan Cleanup**
  - 10-snapshot rolling limit enforcement in `SyncManager.kt`.
  - Manifest message unpinning and deletion via Telegram API.
  - Reference-counted garbage collector deleting only cloud chunk messages unreferenced by active snapshots.

---

## Phase 6: Visual & Theme Overhaul `[COMPLETED]`

- **6.1 Pure Vector Launcher Icons (Zero-Squircle)**
  - Cleaned launcher icons centered strictly within the 66dp safe zone over `#070A0F` dark background (`ic_launcher_foreground.xml`, `ic_launcher_foreground_b.xml`).
  - Dual approved icon options (*Dynamic Fluid Contour* as Default, *Dual-Contour Cyber Cloud* as Secondary) switched dynamically via `IconSwitcher.kt`.
- **6.2 Dynamic Theme Engine (2 Modes × 3 Accents)**
  - 6 reactive theme variations: 2 Modes (`DARK`, `LIGHT`) × 3 Accents (`GREEN`, `AMBER`, `CYAN`) bound to `MaterialTheme.colorScheme` (`Theme.kt`).
  - Clean customization suite (`CustomizationDialog.kt`).

---

## Phase 7: Cloud Snapshot Manager `[COMPLETED]`

- **7.1 Command Integration**
  - Dedicated `[4] cloud` command (`CliCommand.CLOUD`).
- **7.2 Multi-Device Remote Snapshot Discovery**
  - `previousManifestMessageId` pointer chaining in `SnapshotManifest` (`BackupManifest.kt`).
  - Automated cloud snapshot scanning (`scanCloudSnapshots`) recovering all remote history on fresh device installs.
- **7.3 Cloud Snapshot Manager Dialog**
  - Interactive management sheet (`CloudManagerDialog.kt`) with file preview, selective snapshot deletion, orphan cleanup, and full vault wipe confirmation.

---

## Phase 8: Passphrase-First Recovery & Task Alerts `[COMPLETED]`

- **8.1 Passphrase-First Vault Unlock**
  - "New Vault" vs. "Existing Vault" setup modes deriving AES master keys via PBKDF2 passphrases (`KeysDialog.kt`).
- **8.2 Decryption Race Condition Resolution**
  - Key persistence synchronization ensuring cryptographic keys are verified on disk before manifest scanning starts.
  - Magic header validation preventing raw ciphertext leaks into JSON parsers (`ManifestManager.kt`).
- **8.3 Post-Setup Cloud Scan & Notifications**
  - Automatic background cloud catalog sync upon setup completion.
  - Android 13+ system completion alerts via `NotificationHelper.kt`.

---

## Phase 9: Automated Background Sync & Scheduling `[COMPLETED]`

- **9.1 WorkManager Background Architecture**
  - `CoroutineWorker` implementation (`TeleBackSyncWorker.kt`) with charging, Wi-Fi/unmetered, and battery constraints (`SyncWorkScheduler.kt`).
- **9.2 Configurable Sync Intervals & External Triggers**
  - 1h, 6h, 12h, 24h, and 7d intervals configured via `ScheduleDialog.kt`.
  - External broadcast intent trigger (`com.teleback.action.TRIGGER_SYNC`) via `SyncBroadcastReceiver.kt`.
- **9.3 Background Conflict Detection & Ongoing Notifications**
  - Non-blocking folder diff scans skipping unchanged executions.
  - Seamless foreground service elevation for heavy transfers (>100 MB).
  - Real-time progress and summary notifications.

---

## Phase 10: Biometric Vault Security & In-Memory Protection `[COMPLETED]`

- **10.1 Biometric Sentinel (Configurable Toggle)**
  - Android `BiometricPrompt` integration (Fingerprint, Face Unlock, Device PIN).
  - User toggle guarding app launch (`BiometricLockOverlay.kt`), key export (`KeysDialog.kt`), and cloud wipes (`CloudManagerDialog.kt`).
- **10.2 In-Memory Cryptographic Protection**
  - Key zeroization on app pause, backgrounding, and screen locks (`CryptoEngine.kt`).
  - Auto re-lock during activity `onStop()` when sentinel is active.
  - Ephemeral intermediate buffer wiping for sensitive memory arrays.

---

## Phase 11: Telegram Diagnostics & Feedback Hub `[COMPLETED]`

- **11.1 On-Device Diagnostics & Crash Collector**
  - Uncaught exception interceptor writing stacktrace, device telemetry, and console logs to `crash_dump.txt` (`CrashHandler.kt`).
  - Regex scrubbing redacting bot tokens, chat IDs, master keys, and storage paths (`LogSanitizer.kt`).
  - In-app interactive diagnostic modal (`FeedbackDialog.kt`).
- **11.2 Telegram Bot Report Pipeline**
  - Zero-knowledge client dispatch copying sanitized ticket to clipboard and launching `@telebackfeedback_bot`.
  - Settings sub-grid option `[4] report` (`CommandGrid.kt`).
- **11.3 Interactive Review, Archive & Discard Triage**
  - Server-side triage engine (`tools/feedback_bot/main.py`) with Telegram Inline Keyboard buttons: `[Review]`, `[Archive]`, `[Discard]`, and `[Reopen]`.

---

## Phase 12: Cloudflare Workers Feedback Webhook `[COMPLETED]`

- **12.1 Edge Webhook Architecture**
  - Serverless edge router (`tools/feedback_worker/worker.js`) listening to `@telebackfeedback_bot` updates and posting ticket cards to the developer group.
- **12.2 In-Place Message State Transitions**
  - Handled callback queries in-place updating ticket card headers from `🔴 UNREVIEWED` to `🟢 REVIEWED` and `📦 ARCHIVED`.
- **12.3 Diagnostic Group Commands & Topics Support**
  - Added group `/id` discovery command and `TOPIC_ID` secret support for forum supergroups.
  - Direct failure notifications sent back to users upon Telegram API forwarding errors.

---

## Phase 13: Production Release & Reproducible Packaging `[COMPLETED]`

- **13.1 ProGuard / R8 Optimization & Minification**
  - Enabled `isMinifyEnabled = true` and `isShrinkResources = true` in release builds with comprehensive rules for Room, Kotlinx Serialization, OkHttp, Biometrics, and WorkManager (`app/proguard-rules.pro`).
- **13.2 Signed Release Build Variants & Custom Binary Naming**
  - Automated binary naming to output `TeleBack_v<version>.apk` directly (e.g. `TeleBack_v2.6.apk` and `TeleBack_v2.6-debug.apk`).
  - Configured secure private keystore signing with `keystore.properties` support and deterministic template keystore fallback (`keystore/teleback-release-template.jks`).
- **13.3 Reproducible Build Generation & Verification**
  - Enforced deterministic archive ordering, zeroed ZIP timestamps, and reproducible Kotlin compiler bytecode flags.
  - Built standalone verification tool (`tools/reproducible_build/verify_build.py`) providing automated SHA-256 digest certification and bit-for-bit binary comparison.
