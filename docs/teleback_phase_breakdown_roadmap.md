# TeleBack — Project Roadmap & Phase-by-Phase Breakdown

> [!NOTE]
> **TeleBack** is an Android 15+ (API 35) CLI-inspired decentralized personal backup, restore, and sync engine powered by private Telegram groups with authenticated on-device AES-256-GCM encryption.

---

## 1. Phase 1: Core Engine, Storage & Telegram Ingestion `[COMPLETED]`

- **1.1 Telegram Bot API Integration** `[COMPLETED]`
  - Built direct OkHttp streaming network client ([`TelegramApiClient.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/TelegramApiClient.kt)).
  - Implemented multipart document streaming for chunk uploads up to Telegram limits.
  - Implemented streaming file byte download with real-time transfer progress callbacks.
- **1.2 Android Storage Access Framework (SAF)** `[COMPLETED]`
  - Implemented scoped storage directory tree traversal ([`SafStorageHelper.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/SafStorageHelper.kt)).
  - Handled persistable URI permissions across app restarts and device reboots.
  - Built recursive file scanner calculating file paths, sizes, and SHA-256 digests.
- **1.3 Cryptography & Security Engine** `[COMPLETED]`
  - Implemented authenticated AES-256-GCM encryption with 12-byte IVs ([`CryptoEngine.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/crypto/CryptoEngine.kt)).
  - Built PBKDF2 key derivation (100,000 iterations, SHA-256) for passphrase-based master keys.
  - Structured standard binary file header with 4-byte magic signature (`TBK1`) and prepended IV.
- **1.4 Local Persistence & Metadata** `[COMPLETED]`
  - Implemented Room database architecture ([`AppDatabase.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/AppDatabase.kt)).
  - Created snapshot manifest cache table ([`SnapshotDao.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/SnapshotDao.kt), [`SnapshotEntity.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/SnapshotEntity.kt)).
  - Integrated Jetpack DataStore Preferences for bot token, group ID, target folder, and key persistence ([`AppPreferences.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/AppPreferences.kt)).

---

## 2. Phase 2: CLI Terminal Interface & Interaction `[COMPLETED]`

- **2.1 Monospace Terminal Log Stream** `[COMPLETED]`
  - Defined categorized log levels (`INFO`, `OK`, `WARN`, `ERR`, `SNAP`, `PLAIN`) ([`TerminalLog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/TerminalLog.kt)).
  - Created ANSI-inspired Jetpack Compose terminal log viewport ([`TerminalLogView.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/TerminalLogView.kt)).
  - Implemented automatic scroll-to-bottom pinning upon new log arrival.
- **2.2 Command Dispatch System** `[COMPLETED]`
  - Designed numerical and alias CLI command routing (`[1]` to `[8]`, `sync`, `backup`, `restore`, etc.) ([`CliCommand.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/CliCommand.kt)).
  - Built high-density 2-column command grid requiring no vertical scrolling ([`CommandGrid.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/CommandGrid.kt)).
  - Created bottom input prompt bar supporting command numbers and flags ([`CommandInputBar.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/CommandInputBar.kt)).
- **2.3 Status Header & System Telemetry** `[COMPLETED]`
  - Built live connectivity state monitor ([`ConnectivityObserver.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/ConnectivityObserver.kt)).
  - Created status bar displaying live connectivity, bot authorization, linked group, selected folder, encryption state, and activity indicator ([`TerminalHeader.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/TerminalHeader.kt)).

---

## 3. Phase 3: Large File Streaming & Network Resilience `[COMPLETED]`

- **3.1 19 MB Chunk Streaming** `[COMPLETED]`
  - Engineered fixed 19 MB chunk partitioning with memory-safe buffer streaming ([`FileChunker.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/FileChunker.kt)).
  - Added per-chunk SHA-256 verification and atomic multi-part reassembly.
  - Implemented `.tbpart` chunk naming convention for multi-part large file representations.
- **3.2 Resume Journal & Crash Recovery** `[COMPLETED]`
  - Built Room persistent pending upload journal ([`PendingUploadDao.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/PendingUploadDao.kt), [`PendingUploadEntity.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/PendingUploadEntity.kt)).
  - Checkpointed each chunk upload individually with Telegram message ID and part index.
  - Enabled resumption of interrupted transfers without re-uploading previously uploaded chunks.
- **3.3 Network Fault Tolerance & Throttling** `[COMPLETED]`
  - Implemented Telegram HTTP 429 flood-wait dynamic delay parser and exponential backoff retry policy ([`RetryPolicy.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/RetryPolicy.kt)).
  - Built offline auto-wait listener: uploads pause when device loses connectivity and resume automatically once internet is restored.

---

## 4. Phase 4: Onboarding, Setup Wizard & Visual Walkthrough `[COMPLETED]`

- **4.1 First-Run Welcome Modal** `[COMPLETED]`
  - Built welcome dialog explaining Telegram personal cloud architecture ([`WelcomeDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/WelcomeDialog.kt)).
  - Added one-click launch button to start configuration setup.
- **4.2 Interactive 3-Step Setup Wizard** `[COMPLETED]`
  - Step 1: Telegram Bot Token entry with direct intent link to `@BotFather` ([`OnboardingDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/OnboardingDialog.kt)).
  - Step 2: Auto-detection of Chat ID by querying Telegram updates for new messages.
  - Step 3: Vault setup & Storage Access Framework folder selection.
- **4.3 Vector-Based Visual Setup Guide** `[COMPLETED]`
  - Created vector visual walkthrough dialog guiding beginner users ([`SetupWalkthroughDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/SetupWalkthroughDialog.kt)).
  - Illustrated steps: creating a bot in `@BotFather`, setting up a private group, adding the bot as administrator, and sending a setup message.

---

## 5. Phase 5: Storage Intelligence & Snapshot Retention `[COMPLETED]`

- **5.1 Offline Cloud Usage Telemetry** `[COMPLETED]`
  - Developed offline cloud storage usage calculator ([`UsageCalculator.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/domain/UsageCalculator.kt)).
  - Computes stored bytes, deduplicated unique files, total snapshots, and encryption overhead entirely offline from local manifests.
- **5.2 Rolling 10-Snapshot Retention & Orphan Cleanup** `[COMPLETED]`
  - Enforced a rolling limit of 10 snapshots per vault in [`SyncManager.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/domain/SyncManager.kt).
  - Unpins and deletes oldest manifest messages from Telegram chat via API.
  - Reference-counted garbage collector: deletes only cloud chunk messages that are no longer referenced by any remaining active snapshot.

---

## 6. Phase 6: Visual & Theme Overhaul `[COMPLETED]`

- **6.1 Default Launcher Icon Switch** `[COMPLETED]`
  - Set Icon B as the default app launcher icon in `AndroidManifest.xml` and default preferences.
  - Created activity-alias dynamic switcher for Icon A, Icon B, and Icon C ([`IconSwitcher.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/util/IconSwitcher.kt)).
- **6.2 Dynamic Theme Engine (2 Modes × 3 Accents)** `[COMPLETED]`
  - Implemented 6 reactive theme variations: 2 Modes (`DARK`, `LIGHT`) × 3 Accents (`GREEN`, `AMBER`, `CYAN`) ([`Theme.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/theme/Theme.kt)).
  - Removed hardcoded terminal colors across all UI components and bound them dynamically to `MaterialTheme.colorScheme`.
  - Built customization dialog supporting mode toggles, color chips, and app icon selectors ([`CustomizationDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/customization/CustomizationDialog.kt)).
- **6.3 Horizontally Scrollable Header Badges** `[COMPLETED]`
  - Converted status chips (`BOT`, `GROUP`, `DIR`, `CIPHER`) to a horizontally scrollable container to eliminate text truncation on narrow screens.

---

## 7. Phase 7: Cloud Snapshot Manager `[COMPLETED]`

- **7.1 Command Migration** `[COMPLETED]`
  - Replaced redundant manual snapshot trigger with `[4] cloud` ([`CliCommand.CLOUD`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/CliCommand.kt)).
- **7.2 Multi-Device Remote Snapshot Discovery** `[COMPLETED]`
  - Added `previousManifestMessageId` pointer chaining into `SnapshotManifest` ([`BackupManifest.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/model/BackupManifest.kt)).
  - Built Telegram cloud crawler (`scanCloudSnapshots`) in [`SyncManager.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/domain/SyncManager.kt) to discover and cache all existing remote snapshots upon app reinstall or device switch.
- **7.3 Cloud Snapshot Manager Dialog** `[COMPLETED]`
  - Created dedicated cloud dialog ([`CloudManagerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/cloud/CloudManagerDialog.kt)).
  - Displays all snapshots with file count, size in MB, and expandable file details preview.
  - Toggle-based selective deletion with automatic orphan chunk garbage collection.
  - Complete vault wipe guarded by typing confirmation `"delete"`.

---

## 8. Phase 8: Passphrase-First Recovery & Task Alerts `[COMPLETED]`

- **8.1 Passphrase-First Vault Unlock** `[COMPLETED]`
  - Redesigned setup wizard Step 3 with active mode selection: **"New Vault"** vs. **"Existing Vault"**.
  - Derives 256-bit AES master key via PBKDF2 passphrase across phone switches without requiring raw 44-character Base64 keys.
  - Added unrecoverable red warning banner and mandatory confirmation checkbox.
  - Updated [`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt) to prioritize passphrase unlock and rotation over raw keys.
- **8.2 Decryption Race Condition Resolution** `[COMPLETED]`
  - Fixed race condition by awaiting master key disk persistence before initiating cloud scans.
  - Added strict magic header validation (`CryptoEngine.isEncrypted`) in [`ManifestManager.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/ManifestManager.kt), preventing raw encrypted ciphertext from leaking into JSON parsing.
- **8.3 Automated Post-Setup Cloud Scan** `[COMPLETED]`
  - Automated initial background cloud scan immediately following setup wizard completion to populate remote snapshots without manual user intervention.
- **8.4 Android Task Completion Notifications** `[COMPLETED]`
  - Built system notification dispatcher ([`NotificationHelper.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/util/NotificationHelper.kt)) posting `"Task Completed"` alerts on command completion.
  - Handled Android 13+ runtime `POST_NOTIFICATIONS` permission in [`MainActivity.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/MainActivity.kt).

---

## 9. Phase 9: Automated Background Sync & Scheduling `[COMPLETED]`

- **9.1 Android WorkManager Architecture** `[COMPLETED]`
  - Implemented `CoroutineWorker` ([`TeleBackSyncWorker.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/TeleBackSyncWorker.kt)) for periodic background backup execution.
  - Configured device constraints: charging requirement, unmetered Wi-Fi / metered data policy, and battery-not-low triggers via [`SyncWorkScheduler.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/SyncWorkScheduler.kt).
- **9.2 Configurable Sync Intervals & Triggers** `[COMPLETED]`
  - Added periodic scheduling preferences in DataStore & interactive dialog ([`ScheduleDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/schedule/ScheduleDialog.kt)) with 1h, 6h, 12h, 24h, and 7d intervals.
  - Supported instant one-off background sync requests triggered by external intents or system events via [`SyncBroadcastReceiver.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/SyncBroadcastReceiver.kt) (`com.teleback.action.TRIGGER_SYNC`).
- **9.3 Background Conflict Resolution & Change Detection** `[COMPLETED]`
  - Non-blocking background folder diff scans before initiating network transfers.
  - Skips chunk uploads if no local files were modified since the last snapshot.
  - Transitions transparently into `SyncForegroundService` / foreground execution if large chunk uploads (>100 MB) are detected during a background worker cycle.
- **9.4 Background Progress Notifications & Error Reporting** `[COMPLETED]`
  - Ongoing background progress notification bar detailing current file and chunk transfer status via [`NotificationHelper.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/util/NotificationHelper.kt).
  - Silent task completion summaries with actionable notifications on network failures.

---

## 10. Phase 10: Biometric Vault Security & In-Memory Protection `[COMPLETED]`

- **10.1 Biometric Authentication (Configurable Toggle)** `[COMPLETED]`
  - Integrated Android `BiometricPrompt` API supporting Fingerprint, Face Unlock, and Device PIN.
  - Dedicated user preference toggle (`Enable / Disable Biometric Security`) in settings.
  - Guards app launch ([`BiometricLockOverlay.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/security/BiometricLockOverlay.kt)), master key export ([`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt)), and cloud vault wipe ([`CloudManagerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/cloud/CloudManagerDialog.kt)) behind biometric authentication when enabled.
- **10.2 In-Memory Cryptographic Protection** `[COMPLETED]`
  - Implemented in-memory cryptographic key zeroization upon app pause, screen lock, or background termination ([`CryptoEngine.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/crypto/CryptoEngine.kt)).
  - Automatic vault re-lock in `onStop()` when Biometric Sentinel is active.
  - Ephemeral intermediate buffer wiping for derived key material and plaintext chunks.

---

## 11. Phase 11: Telegram Diagnostics & Feedback Hub `[COMPLETED]`

- **11.1 On-Device Diagnostics & Crash Collector** `[COMPLETED]`
  - Built [`CrashHandler.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/diagnostics/CrashHandler.kt) capturing uncaught exceptions, device telemetry, and last 50 console logs into local `crash_dump.txt`.
  - Built [`LogSanitizer.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/diagnostics/LogSanitizer.kt) with zero-knowledge redaction of bot tokens, chat IDs, AES master keys, and storage paths.
  - In-app trigger: [`FeedbackDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/feedback/FeedbackDialog.kt) supporting crash recovery reports and manual feedback.
- **11.2 Telegram Bot Report Pipeline** `[COMPLETED]`
  - Zero-secret client transmission: Copies sanitized ticket to clipboard and deep-links to `@telebackfeedback_bot` with system share fallback.
  - Added option `[4] report` to Settings sub-view in [`CommandGrid.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/CommandGrid.kt).
- **11.3 Interactive Review, Archive & Discard Workflow** `[COMPLETED]`
  - Implemented server-side bot triage engine in [`tools/feedback_bot/main.py`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/tools/feedback_bot/main.py) with Telegram Inline Keyboard buttons: `[✅ Review]`, `[📁 Archive]`, and `[🗑 Discard]`.
  - Dynamic in-place status updates: transitions card header from `🔴 UNREVIEWED` to `🟢 REVIEWED (by @Admin at timestamp)` and `📦 ARCHIVED`.
  - Documented complete deployment workflow in [`tools/feedback_bot/README.md`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/tools/feedback_bot/README.md).

---

## 12. Phase 12: Cloudflare Workers Feedback Webhook `[COMPLETED]`

- **12.1 Worker Receiving Webhook & Forwarding Tickets** `[COMPLETED]`
  - Implemented [`tools/feedback_worker/worker.js`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/tools/feedback_worker/worker.js) receiving `@telebackfeedback_bot` webhook payloads and posting sanitized ticket cards to the TeleBack Feedbacks group.
- **12.2 Inline Review Buttons Handling** `[COMPLETED]`
  - Handled `callback_query` updates directly in the Worker supporting in-place message state edits: `[✅ Review]`, `[📁 Archive]`, `[🗑 Discard]`, and `[🔄 Reopen]`.
- **12.3 Encrypted Environment Variables & Secret Sanitization** `[COMPLETED]`
  - Configured `BOT_TOKEN` and `GROUP_ID` as Cloudflare encrypted secrets; removed token and group ID defaults from [`tools/feedback_bot/main.py`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/tools/feedback_bot/main.py).
- **12.4 Deployment, Webhook Configuration & E2E Verification** `[COMPLETED]`
  - Documented deployment walkthrough via Cloudflare Dashboard / Wrangler CLI and Telegram `setWebhook` API in [`tools/feedback_worker/README.md`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/tools/feedback_worker/README.md).

---

## 13. Phase 13: Production Release & Reproducible Packaging `[COMPLETED]`

- **13.1 ProGuard / R8 Optimization & Minification** `[COMPLETED]`
  - Enabled ProGuard / R8 code shrinking and resource optimization (`isMinifyEnabled = true`, `isShrinkResources = true`).
  - Configured reflection and serialization keep rules in [`app/proguard-rules.pro`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/proguard-rules.pro).
- **13.2 Signed Release Build Variants** `[COMPLETED]`
  - Configured secure private keystore signing via `keystore.properties` / environment variable / fallback template in [`app/build.gradle.kts`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/build.gradle.kts).
  - Configured deterministic output naming `TeleBack_v<versionName>.apk`.
- **13.3 Reproducible Build Generation & Verification** `[COMPLETED]`
  - Enforced deterministic archive ordering and timestamp zeroing in Gradle packaging (`isPreserveFileTimestamps = false`, `isReproducibleFileOrder = true`).
  - Automated verification script checking bit-for-bit identical binary SHA-256 digests in [`tools/reproducible_build/verify_build.py`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/tools/reproducible_build/verify_build.py) and documentation in [`tools/reproducible_build/README.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/tools/reproducible_build/README.md).
