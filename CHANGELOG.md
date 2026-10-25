# Changelog

All notable changes to the TeleBack project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.7.0] - 2026-10-25

### Added
- **Selective Snapshot Download**: Interactive file explorer with search filtering, multi-file selection, and dual export destinations (linked folder or system Downloads).
- **Selective File Upload**: Direct device file picker to upload and merge individual files into the latest cloud snapshot.
- **Sync Mode Selector**: Prompt on sync to choose between linked folder sync or selective file upload.
- **In-Place Snapshot Merging**: Differential syncs update the latest snapshot directly, preserving history without redundant records.
- **Non-Destructive Sync**: Existing remote files are retained when syncing changes from a newly linked or partially populated folder.

### Changed & Refined
- **Adaptive Launcher Icon**: Standardized full-bleed vector icon within the launcher safe zone to eliminate clipping across all Android devices.
- **Leaner App Size**: Reduced release package size down to 4.59 MB (from 4.63 MB in v2.6.0) through asset consolidation.

---

## [2.6.0] - 2026-10-24

### Added
- **Cloudflare Edge Feedback Pipeline**: Serverless diagnostics webhook with interactive status buttons and encrypted environment secrets.
- **Crash Recovery & Diagnostics**: Automated sanitization for sensitive keys/tokens, uncaught exception logging, and in-app feedback reporting.
- **Reproducible Release Build**: Bit-for-bit build verification and R8 optimization, reducing footprint to 4.63 MB.

### Fixed & Refined
- **Webhook Resilience**: Immediate edge acknowledgment to drain Telegram flood retries.
- **Message Formatting**: Safe escaping across ticket cards to prevent markdown parsing errors.
- **Code Shrinking Rules**: Hardened retention rules for network, persistence, and biometric libraries.

---

## [2.5.0] - 2026-10-22

### Added
- **Custom Snapshot Labels**: Optional custom tags during backup and sync for clearer restore points.
- **Centralized Settings Navigation**: Unified sub-menu for configuration, theming, and diagnostics with smooth retro transitions.
- **Biometric Sentinel**: Fingerprint, Face Unlock, and PIN authentication guarding app entry, key exports, and vault reset.
- **Key Zeroization**: Automatic in-memory clearing of cryptographic keys when the app leaves the foreground.
- **Automated Background Sync**: Periodic background sync with charging, Wi-Fi, and battery constraints, plus broadcast trigger support.

### Changed & Refined
- **Simplified Icon System**: Streamlined to a unified adaptive brand icon and cleaned up legacy dynamic switcher code.
- **Navigation Polish**: Clearer return prompts and modernized settings navigation.

---

## [2.0.0] - 2026-10-20

### Added
- **Multi-Device Snapshot Discovery**: Pointer-chained manifests enabling full historical snapshot recovery on new devices.
- **Cloud Snapshot Manager**: Dedicated dialog to inspect, prune, or delete snapshots and safely wipe vaults.
- **Passphrase-First Vault Unlock**: PBKDF2 passphrase derivation across devices without manual raw key entry.
- **Task Completion Notifications**: System notifications on completed backups, restores, and cloud scans.

### Fixed
- **Key Persistence Race Condition**: Guaranteed on-disk key verification before initiating remote manifest scans.

---

## [1.0.0] - 2026-10-18

### Added
- **Chunk Streaming Engine**: 19 MB partitioned chunk streaming with memory-efficient buffers and atomic multi-part reassembly.
- **Crash-Resilient Upload Journal**: Checkpoint persistence with auto-resumption of interrupted transfers.
- **Network Resilience**: Adaptive flood-wait delay parsing and exponential backoff retry policies.
- **Scoped Storage Access**: Storage Access Framework integration with recursive SHA-256 scanning and persistent permissions.
- **Authenticated Encryption**: On-device AES-256-GCM encryption with PBKDF2 key derivation.
- **Terminal CLI Viewport**: Retro monospace activity log with color-coded levels, dual-column command grid, and live telemetry.
- **Snapshot Retention**: Automatic 10-snapshot pruning and garbage collection for unreferenced chunks.
