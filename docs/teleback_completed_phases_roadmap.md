# TeleBack — Completed Phases Roadmap (v2.6)

> [!NOTE]
> **TeleBack** is an Android 15+ (API 35) CLI-inspired decentralized personal backup, restore, and sync engine powered by private Telegram groups with authenticated on-device AES-256-GCM encryption.

---

## Phase 1: Core Engine, Storage & Telegram Ingestion `[COMPLETED]`

- **1.1 Telegram Bot API Integration**
  - Direct OkHttp streaming client ([`TelegramApiClient.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/TelegramApiClient.kt)).
  - Multipart document streaming for chunk uploads up to Telegram limits.
  - Streaming file byte download with real-time transfer progress callbacks.
- **1.2 Android Storage Access Framework (SAF)**
  - Scoped storage directory tree traversal ([`SafStorageHelper.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/SafStorageHelper.kt)).
  - Persistable URI permissions across app restarts and device reboots.
  - Recursive file scanner calculating file paths, sizes, and SHA-256 digests.
- **1.3 Cryptography & Security Engine**
  - Authenticated AES-256-GCM encryption with 12-byte IVs ([`CryptoEngine.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/crypto/CryptoEngine.kt)).
  - PBKDF2 key derivation (100,000 iterations, SHA-256) for passphrase-based master keys.
  - Standard binary file header with 4-byte magic signature (`TBK1`) and prepended IV.
- **1.4 Local Persistence & Metadata**
  - Room database architecture ([`AppDatabase.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/AppDatabase.kt)).
  - Snapshot manifest cache table ([`SnapshotDao.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/SnapshotDao.kt), [`SnapshotEntity.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/SnapshotEntity.kt)).
  - Jetpack DataStore Preferences for tokens, IDs, paths, and preferences ([`AppPreferences.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/AppPreferences.kt)).

---

## Phase 2: CLI Terminal Interface & Interaction `[COMPLETED]`

- **2.1 Monospace Terminal Log Stream**
  - Categorized log levels (`INFO`, `OK`, `WARN`, `ERR`, `SNAP`, `PLAIN`) ([`TerminalLog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/TerminalLog.kt)).
  - Jetpack Compose ANSI-inspired terminal log viewport with auto-scroll pinning ([`TerminalLogView.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/TerminalLogView.kt)).
  - Built-in compact 3dp retro scrollbar with dynamic offset positioning.
- **2.2 Centralized Command Grid & Sub-Menus**
  - Numbered and alias CLI command routing (`[1]` to `[8]`, `[0]`) ([`CliCommand.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/CliCommand.kt)).
  - High-density 2-column command grid ([`CommandGrid.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/CommandGrid.kt)).
  - Centralized `Settings` command `[8]` with a 0.5x retro cyber-glitch transition and dedicated `[<] Back` navigation.
  - Interactive snapshot tagging prompt ([`SnapshotPromptDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/SnapshotPromptDialog.kt)) for human-readable restore identifiers.
- **2.3 Status Header & Telemetry Badges**
  - Live network connectivity monitor ([`ConnectivityObserver.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/ConnectivityObserver.kt)).
  - Read-only horizontally scrollable status badges (`BOT`, `GROUP`, `DIR`, `AUTO`, `CIPHER`) ([`TerminalHeader.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/terminal/components/TerminalHeader.kt)).

---

## Phase 3: Large File Streaming & Network Resilience `[COMPLETED]`

- **3.1 19 MB Chunk Streaming**
  - Fixed 19 MB chunk partitioning with memory-safe buffer streaming ([`FileChunker.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/FileChunker.kt)).
  - Per-chunk SHA-256 verification and atomic multi-part reassembly.
  - `.tbpart` chunk naming convention for multi-part file representations.
- **3.2 Resume Journal & Crash Recovery**
  - Room persistent pending upload journal ([`PendingUploadDao.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/PendingUploadDao.kt), [`PendingUploadEntity.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/local/PendingUploadEntity.kt)).
  - Individual chunk checkpoints enabling transfer resumption without re-uploading completed parts.
- **3.3 Network Fault Tolerance & Throttling**
  - Telegram HTTP 429 flood-wait delay parsing and exponential backoff retry policy ([`RetryPolicy.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/network/RetryPolicy.kt)).
  - Offline auto-wait listener: pauses on connectivity loss and automatically resumes when restored.

---

## Phase 4: Onboarding, Setup Wizard & Visual Walkthrough `[COMPLETED]`

- **4.1 First-Run Welcome Modal**
  - Welcome dialog outlining decentralized personal cloud architecture ([`WelcomeDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/WelcomeDialog.kt)).
- **4.2 Interactive 3-Step Setup Wizard**
  - Bot token entry with direct `@BotFather` intent links ([`OnboardingDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/OnboardingDialog.kt)).
  - Auto-detection of group Chat ID from recent message updates.
  - SAF target folder binding with persistent permissions.
- **4.3 Vector-Based Visual Setup Guide**
  - Visual setup walkthrough dialog ([`SetupWalkthroughDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/onboarding/SetupWalkthroughDialog.kt)) illustrating bot creation, private group creation, admin permissions, and initialization.

---

## Phase 5: Storage Intelligence & Snapshot Retention `[COMPLETED]`

- **5.1 Offline Cloud Usage Telemetry**
  - Local usage calculator ([`UsageCalculator.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/domain/UsageCalculator.kt)) computing stored bytes, deduplicated files, snapshot count, and encryption overhead entirely offline.
- **5.2 Rolling 10-Snapshot Retention & Orphan Cleanup**
  - 10-snapshot rolling limit enforcement in [`SyncManager.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/domain/SyncManager.kt).
  - Manifest message unpinning and deletion via Telegram API.
  - Reference-counted garbage collector deleting only cloud chunk messages unreferenced by active snapshots.

---

## Phase 6: Visual & Theme Overhaul `[COMPLETED]`

- **6.1 Pure Vector Launcher Icons (Zero-Squircle)**
  - Cleaned launcher icons centered strictly within the 66dp safe zone over `#070A0F` dark background ([`ic_launcher_foreground.xml`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/res/drawable/ic_launcher_foreground.xml), [`ic_launcher_foreground_b.xml`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/res/drawable/ic_launcher_foreground_b.xml)).
  - Dual approved icon options (*Dynamic Fluid Contour* as Default, *Dual-Contour Cyber Cloud* as Secondary) switched dynamically via [`IconSwitcher.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/util/IconSwitcher.kt).
- **6.2 Dynamic Theme Engine (2 Modes × 3 Accents)**
  - 6 reactive theme variations: 2 Modes (`DARK`, `LIGHT`) × 3 Accents (`GREEN`, `AMBER`, `CYAN`) bound to `MaterialTheme.colorScheme` ([`Theme.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/theme/Theme.kt)).
  - Clean customization suite ([`CustomizationDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/customization/CustomizationDialog.kt)).

---

## Phase 7: Cloud Snapshot Manager `[COMPLETED]`

- **7.1 Command Integration**
  - Dedicated `[4] cloud` command ([`CliCommand.CLOUD`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/cli/CliCommand.kt)).
- **7.2 Multi-Device Remote Snapshot Discovery**
  - `previousManifestMessageId` pointer chaining in `SnapshotManifest` ([`BackupManifest.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/data/model/BackupManifest.kt)).
  - Automated cloud snapshot scanning (`scanCloudSnapshots`) recovering all remote history on fresh device installs.
- **7.3 Cloud Snapshot Manager Dialog**
  - Interactive management sheet ([`CloudManagerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/cloud/CloudManagerDialog.kt)) with file preview, selective snapshot deletion, orphan cleanup, and full vault wipe confirmation.

---

## Phase 8: Passphrase-First Recovery & Task Alerts `[COMPLETED]`

- **8.1 Passphrase-First Vault Unlock**
  - "New Vault" vs. "Existing Vault" setup modes deriving AES master keys via PBKDF2 passphrases ([`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt)).
- **8.2 Decryption Race Condition Resolution**
  - Key persistence synchronization ensuring cryptographic keys are verified on disk before manifest scanning starts.
  - Magic header validation preventing raw ciphertext leaks into JSON parsers ([`ManifestManager.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/storage/ManifestManager.kt)).
- **8.3 Post-Setup Cloud Scan & Notifications**
  - Automatic background cloud catalog sync upon setup completion.
  - Android 13+ system completion alerts via [`NotificationHelper.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/util/NotificationHelper.kt).

---

## Phase 9: Automated Background Sync & Scheduling `[COMPLETED]`

- **9.1 WorkManager Background Architecture**
  - `CoroutineWorker` implementation ([`TeleBackSyncWorker.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/TeleBackSyncWorker.kt)) with charging, Wi-Fi/unmetered, and battery constraints ([`SyncWorkScheduler.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/SyncWorkScheduler.kt)).
- **9.2 Configurable Sync Intervals & External Triggers**
  - 1h, 6h, 12h, 24h, and 7d intervals configured via [`ScheduleDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/schedule/ScheduleDialog.kt).
  - External broadcast intent trigger (`com.teleback.action.TRIGGER_SYNC`) via [`SyncBroadcastReceiver.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/service/SyncBroadcastReceiver.kt).
- **9.3 Background Conflict Detection & Ongoing Notifications**
  - Non-blocking folder diff scans skipping unchanged executions.
  - Seamless foreground service elevation for heavy transfers (>100 MB).
  - Real-time progress and summary notifications.

---

## Phase 10: Biometric Vault Security & In-Memory Protection `[COMPLETED]`

- **10.1 Biometric Sentinel (Configurable Toggle)**
  - Android `BiometricPrompt` integration (Fingerprint, Face Unlock, Device PIN).
  - User toggle guarding app launch ([`BiometricLockOverlay.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/security/BiometricLockOverlay.kt)), key export ([`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt)), and cloud wipes ([`CloudManagerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/presentation/cloud/CloudManagerDialog.kt)).
- **10.2 In-Memory Cryptographic Protection**
  - Key zeroization on app pause, backgrounding, and screen locks ([`CryptoEngine.kt`](file:///c:/Users/Jaheen/Documents/Projects/App_TeleBack/app/src/main/java/com/teleback/core/crypto/CryptoEngine.kt)).
  - Auto re-lock during activity `onStop()` when sentinel is active.
  - Ephemeral intermediate buffer wiping for sensitive memory arrays.
