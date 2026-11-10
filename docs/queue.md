# TeleBack — Feature Execution Queue

This document stages approved features, architectural enhancements, and backlog items to be implemented together in coordinated execution cycles.

---

## 📋 Staged Features for Execution (Next Build: `v2.9`)

### Feature 1: Quick Deep-Link Setup Wizard & Cross-Device QR/Code Clone (`TBSETUP1`)
*(Approved & EGM-Reviewed Plan: [`docs/designs/quick_setup_and_desktop_plan.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/designs/quick_setup_and_desktop_plan.md))*

#### 1. Summary
Replace the legacy 7-step manual setup in [`OnboardingDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/onboarding/OnboardingDialog.kt) with a streamlined **Quick Deep-Link Setup Wizard** and a dedicated **`[Scan QR / Paste Setup Code]`** clone flow:
- **Step 1 (Bot Token & Clone Button)**:
  - Top **`[Scan QR / Paste Setup Code]`** button switching into the 1-step Clone Setup view (`[Scan QR Image]` via photo picker + `[Paste Setup Code]` + `Transfer/Vault Passphrase` + `[Select Target Folder]` → atomic restore of `botToken`, `chatId`, `chatTitle`, and `masterKeyBase64`).
  - Quick Bot Setup: `[Open @BotFather]` + `[Paste & Verify Token]` (`getMe()` verification displaying `Verified Bot: @username`).
- **Step 2 (1-Tap Native Telegram Group & Admin Picker)**:
  - Launches `tg://resolve?domain=<bot_username>&startgroup=teleback_setup&admin=post_messages+edit_messages+delete_messages+pin_messages` to open Telegram's native Group Selector & Admin promotion sheet in one tap.
  - Parses `my_chat_member` updates (`limit=100`, filtering out `"left"` / `"kicked"`) in [`TelegramModels.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/core/network/TelegramModels.kt) and auto-discovers groups on `Lifecycle.Event.ON_RESUME` without requiring a dummy message.
- **Step 3 (Vault & Target Folder)**:
  - `New Vault` / `Existing Vault` passphrase setup + SAF Target Folder picker.
- **Clone Export in [`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt)**:
  - Encrypts `SetupClonePayload(botToken, chatId, chatTitle, masterKeyBase64)` via `CryptoEngine.encryptBytes` under `"TBSETUP1:<base64>"` and renders an in-app QR code (`com.google.zxing:core:3.5.3` via `QrCodeCodec.kt`).

---

### Feature 2: Cross-Platform Desktop Application (`Windows / Linux / macOS` — Layout 1: IDE Split-Commander)
*(Approved Plan: [`docs/designs/quick_setup_and_desktop_plan.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/designs/quick_setup_and_desktop_plan.md) • Layout Preview: [`docs/designs/desktop_layout_1_split_ide.svg`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/designs/desktop_layout_1_split_ide.svg))*

#### 1. Summary
Build the `:desktop` JVM module using **JetBrains Compose for Desktop** (`org.jetbrains.compose` + `kotlinx-coroutines-swing`), sharing `:app`'s pure-JVM cryptography (`TBK1` AES-256-GCM + PBKDF2), 19 MB chunk streamer, manifest manager, `SnapshotTreeBuilder`, and `TBSETUP1` QR/Code clone codec.
- **Selected Layout — Layout 1 (IDE Split-Commander)**:
  - **Left Sidebar (`250dp`)**: Vault telemetry card, vertical command rail (`[1] sync` through `[0] setup`), and file upload drop zone.
  - **Top Main Pane**: Always-open Hierarchical Accordion Tree Explorer with search filter, snapshot selector dropdown, `TriStateCheckbox` selection, and inline `Folder` / `Downloads` extraction buttons.
  - **Bottom Docked Pane (`210dp`)**: Read-only live monospace terminal log stream (no bottom CLI input bar).
- **Native Packaging**: `.msi` / `.exe` (Windows), `.deb` / `.rpm` (Linux), `.dmg` (macOS).

---

### Feature 3 (Applied Locally — Ready for `v2.9` Build): Unified Cyber-Wing Glider Adaptive App Icon

#### 1. Architectural & Aesthetic Summary
Single-silhouette synthesized mark (**The Cyber-Wing Glider**) fusing the Telegram paper plane with trailing signal waves, PCB circuit-board wing terminals, soft cloud-toned ice-blue upper wing (`#E1F0FF`), shield-cyan lower wing (`#00C4EB` → `#0096C7`), and an integrated keyhole fuselage lock core.

#### 2. Applied Files
- [`app/src/main/res/drawable/ic_launcher_background.xml`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/res/drawable/ic_launcher_background.xml): Crisp Ice-to-Sky linear gradient (`#F4F9FF` → `#D8ECFF`).
- [`app/src/main/res/drawable/ic_launcher_foreground.xml`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/res/drawable/ic_launcher_foreground.xml): Centered within the `66dp` Android Adaptive Icon safe zone (`center = (54, 54)`, max radius ≤ `28.4dp`).
- [`assets/icon_cyber_wing_glider.svg`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/assets/icon_cyber_wing_glider.svg) & [`assets/Icon.png`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/assets/Icon.png): Master vector SVG and rendered `512x512` PNG header asset.

---

## ✅ Completed & Verified Features

### Hierarchical Inline Accordion Tree Explorer & Selective Single-File Extraction (`v2.8`)
*(Approved Design: [`docs/designs/hierarchical_accordion_tree_explorer.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/designs/hierarchical_accordion_tree_explorer.md))*
- Interactive inline accordion tree in `SnapshotFileExplorerDialog` with expandable/collapsible folder nodes and aggregate file/byte metrics.
- Capped visual indentation (max 4 levels / 48dp) with direct parent subtitle hints for deeply nested folders.
- Single-file extraction with persistent in-dialog browsing and 3-second auto-dismissing `ExtractionBanner`.
- Sequential execution guarded by existing `_isBusy` lifecycle.
- Tri-state recursive folder checkbox selection (`TriStateCheckbox`) with empty folder safeguards (`totalFilesCount == 0 -> false`).
- Stateless search filter ephemerally expanding matching ancestor folders without mutating manual user expansion.
- Implemented in: [`SnapshotTreeNode.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/restore/SnapshotTreeNode.kt), [`SnapshotFileExplorerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/restore/SnapshotFileExplorerDialog.kt), [`TerminalViewModel.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalViewModel.kt), [`TerminalScreen.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalScreen.kt), and [`SnapshotTreeBuilderTest.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/test/java/com/teleback/presentation/restore/SnapshotTreeBuilderTest.kt).

---

### Production Release & Reproducible Packaging (Phase 13 — `v2.8`)
- ProGuard / R8 code shrinking and resource optimization enabled in [`app/build.gradle.kts`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/build.gradle.kts) and [`app/proguard-rules.pro`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/proguard-rules.pro).
- Private keystore signing pipeline and deterministic APK naming (`TeleBack_v<versionName>.apk`).
- Bit-for-bit reproducible packaging verified via [`tools/reproducible_build/verify_build.py`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/tools/reproducible_build/verify_build.py).

---

## 📦 Archived / Deferred Features

### Archived Feature: High-Speed Mode & Custom Bot API Server Support
- **Summary**: Optional setting to route traffic through a self-hosted `telegram-bot-api` instance (`custom_api_url`, `X-TeleBack-Key`, and `Semaphore(permits = 2)` concurrent chunk uploads) while preserving the 19 MB chunk boundary invariant (`FileChunker.DEFAULT_CHUNK_SIZE`).
- **Status**: Archived/deferred per user direction.
