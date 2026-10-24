# Changelog

All notable changes to the TeleBack project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.6.0] - 2026-10-23

### Added
- **Cloudflare Workers Webhook**: Serverless edge webhook handler (`tools/feedback_worker/worker.js`) listening to `@telebackfeedback_bot` updates and forwarding diagnostics to the developer group with zero server runtime costs.
- **Serverless Interactive Inline Buttons**: Handled `callback_query` actions directly at the edge (`[Mark Reviewed]`, `[Archive]`, `[Discard]`, `[Reopen]`) with live message header updates via `editMessageText`.
- **Zero-Secret Cloudflare Environment Variables**: Migrated `BOT_TOKEN` and `GROUP_ID` to encrypted Cloudflare secrets (`wrangler secret put`), fully eliminating hardcoded token defaults from the repository.
- **Crash Recovery & Diagnostics Pipeline**: Built `LogSanitizer.kt` with regex scrubbing for bot tokens, chat IDs, AES master keys, passphrases, and personal folder paths.
- **Uncaught Exception Interceptor**: App-wide crash interception (`CrashHandler.kt`) writing stacktraces, last 50 console log entries, and device telemetry to `crash_dump.json`.
- **Diagnostics Modal (`[4] report`)**: Settings feedback modal (`FeedbackDialog.kt`) supporting user comments, previewing sanitized logs, and offering 1-tap clipboard copy + Telegram deep link.
- **Unit Test Suite**: Added `LogSanitizerTest.kt`; 34 / 34 unit tests passing cleanly with 100% success rate.
- **Build Output**: Successfully assembled `v2.6` (versionCode 8) debug APK (`app-debug.apk`, 66.08 MB).

### Fixed & Refined
- **Telegram Webhook Retry Queue Drain**: Configured the Cloudflare Worker to always return HTTP 200 immediately to drain Telegram flood retries triggered by unescaped Markdown symbols.
- **Defensive Message Formatting**: Implemented HTML/Markdown escaping across ticket cards to avoid Telegram parse errors.

---

## [2.5.0] - 2026-10-22

### Added
- **Custom Snapshot Labels**: Interactive prompt dialog (`SnapshotPromptDialog.kt`) on `[1] sync` and `[2] backup` allowing optional human-readable tags stored in manifests and displayed in restore sheets.
- **Centralized Settings Navigation**: Replaced the separate menu card with a centralized `[8] Settings` command displaying a 2-column sub-grid with a 0.5x retro cyber-glitch transition and dedicated `[<] Back` return action.
- **Biometric Sentinel**: Added `BiometricLockOverlay.kt` supporting Fingerprint, Face Unlock, and Device PIN to guard app launch, master key export, and vault wipe actions.
- **In-Memory Zeroization**: Memory wiping of cryptographic key material upon app backgrounding, screen lock, or activity `onStop()`.
- **Automated WorkManager Background Sync**: Configured periodic sync jobs (`TeleBackSyncWorker.kt`, `SyncWorkScheduler.kt`) with constraints for charging, unmetered Wi-Fi, and battery levels, plus external intent broadcast support (`com.teleback.action.TRIGGER_SYNC`).

### Changed & Cleaned Up
- **Single Vector Launcher Icon**: Extracted and finalized a single adaptive icon (`ic_launcher_foreground.png`) centered in the 66dp safe zone over `#070A0F` across all density buckets (mdpi to xxxhdpi).
- **Removed Deprecated Custom Icon Switcher**: Deleted `IconSwitcher.kt` and multi-icon dynamic aliases from `AndroidManifest.xml` and `CustomizationDialog.kt`.
- **Terminal Navigation Prompt**: Removed `"type 'back' to exit"` and replaced it with `"Tap [<] to return"`. Renamed option `[1]` to `"Themes"`.

---

## [2.0.0] - 2026-10-20

### Added
- **Multi-Device Remote Snapshot Discovery**: Added `previousManifestMessageId` pointer chaining into `BackupManifest.kt` and implemented `scanCloudSnapshots` in `SyncManager.kt` to crawl and recover historical snapshots on new devices.
- **Cloud Snapshot Manager (`[4] cloud`)**: Dedicated management dialog (`CloudManagerDialog.kt`) with snapshot file inspection, selective snapshot deletion, orphan chunk cleanup, and safe vault wipe guarded by typing confirmation.
- **Passphrase-First Vault Unlock**: Redesigned setup wizard Step 3 to support PBKDF2 passphrase key derivation across devices without requiring raw 44-character Base64 keys.
- **Decryption Race Fix**: Ensured cryptographic keys are persisted and verified on disk before triggering remote manifest scans, preventing raw ciphertext leaks into JSON parsers.
- **Task Completion Notifications**: Added system alert notifications via `NotificationHelper.kt` on backup, restore, and cloud scan completion (Android 13+ runtime permissions handled).

---

## [1.0.0] - 2026-10-18

### Added
- **Chunk Streaming Engine**: 19 MB fixed chunk partitioning (`FileChunker.kt`) with memory-safe buffer streams and atomic `.tbpart` multi-part reassembly.
- **Resume Journal & Crash Recovery**: Room database pending upload journal (`PendingUploadDao.kt`) with part-level checkpoints.
- **Network Resilience**: Telegram HTTP 429 flood-wait dynamic delay parser and exponential backoff retry policy (`RetryPolicy.kt`).
- **Storage Access Framework (SAF)**: Scoped storage recursive directory scanning with SHA-256 digests and persistable URI permissions.
- **Authenticated Encryption**: On-device AES-256-GCM encryption with 12-byte IVs, PBKDF2 derivation (100k rounds, SHA-256), and `TBK1` binary header.
- **Monospace CLI Terminal Viewport**: Retro ANSI terminal log stream with categorized levels (`INFO`, `OK`, `WARN`, `ERR`, `SNAP`), 2-column command grid, and real-time connectivity telemetry.
- **Rolling 10-Snapshot Retention**: Automatic cloud manifest pruning and reference-counted garbage collection for unreferenced chunks.
