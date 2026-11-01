# TeleBack — Feature Execution Queue

This document stages approved features, architectural enhancements, and backlog items to be implemented together in coordinated execution cycles.

---

## 📋 Staged Features for Execution

### Feature 1: High-Speed Mode & Custom Bot API Server Support

#### 1. Architectural Summary
Provide users an optional setting in TeleBack to route traffic through a self-hosted `telegram-bot-api` instance (e.g. running on a VPS or local home server) to unlock high transfer rates while maintaining **100% data portability and recovery safety**.

#### 2. Key Design Decisions
- **Universal 19 MB Chunk Boundary (Invariant)**:
  - All chunks remain strictly at 19 MB (`FileChunker.DEFAULT_CHUNK_SIZE`).
  - **Why**: Chunks larger than 20 MB can *never* be downloaded via official `api.telegram.org` (`getFile` fails with HTTP 400). By preserving 19 MB chunks, backups made via high-speed servers can always be restored directly on standard Telegram anywhere in the world, even if the custom server goes offline.
  - **RAM & OOM Safety**: Prevents fatal Android `OutOfMemoryError` heap crashes that would occur with 100 MB–1.9 GB in-memory allocations.
- **Concurrent Coroutine Multiplexing**:
  - In High-Speed mode, upload 2–3 chunks concurrently over HTTP/2 using Kotlin Coroutines (`Semaphore(permits = 2)`).
  - Synchronizes with `PendingUploadDao` atomically to ensure crash-recovery and resume integrity.
- **Client Endpoint Configuration**:
  - Store `customApiUrl: Flow<String?>` and `customRelayKey: Flow<String?>` in `AppPreferences.kt` via DataStore.
  - Parameterize both `baseUrl = "$serverUrl/bot$botToken"` and `fileBaseUrl = "$serverUrl/file/bot$botToken"` in `TelegramApiClient.kt`.
  - Pass custom auth header (`X-TeleBack-Key`) when configured to guard the private server from unauthorized public use.
- **Settings UI Integration**:
  - Add a **Cloud Speed Mode** toggle / configuration dialog in the terminal settings sub-grid:
    - `[1] Standard Mode (api.telegram.org, $0, Zero Setup, Universal Restore)`
    - `[2] High-Speed Mode (Custom Server URL, Multi-Stream Uploads)`

#### 3. Scope of File Changes
- `app/src/main/java/com/teleback/data/local/AppPreferences.kt`: Add keys for `custom_api_url` and `relay_auth_key`.
- `app/src/main/java/com/teleback/core/network/TelegramApiClient.kt`: Refactor constructor to accept custom server endpoint and auth interceptor; update `baseUrl` and `fileBaseUrl`.
- `app/src/main/java/com/teleback/domain/SyncManager.kt`: Implement concurrent chunk worker pipeline via `Semaphore(permits = 2)` when high-speed mode is enabled.
- `app/src/main/java/com/teleback/presentation/terminal/components/CommandGrid.kt`: Add CLI command routing for relay server configuration in Settings.
- `docs/recipes/self_hosted_bot_api.md`: Provide standalone Docker and Caddy deployment instructions with automatic 2-minute temp-file pruning (`tmpfs` / cron).

---

## ✅ Completed & Verified Features

### Feature 2: Hierarchical Inline Accordion Tree Explorer & Selective Single-File Extraction
*(Approved Design: [`docs/designs/hierarchical_accordion_tree_explorer.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/designs/hierarchical_accordion_tree_explorer.md))*
- Interactive inline accordion tree in `SnapshotFileExplorerDialog` with expandable/collapsible folder nodes and aggregate file/byte metrics.
- Capped visual indentation (max 4 levels / 48dp) with direct parent subtitle hints for deeply nested folders.
- Single-file extraction with persistent in-dialog browsing and 3-second auto-dismissing `ExtractionBanner`.
- Sequential execution guarded by existing `_isBusy` lifecycle (no complex coroutine queues).
- Tri-state recursive folder checkbox selection (`TriStateCheckbox`) with empty folder safeguards (`totalFilesCount == 0 -> false`).
- Stateless search filter ephemerally expanding matching ancestor folders without mutating manual user expansion.
- Implemented in: [`SnapshotTreeNode.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/restore/SnapshotTreeNode.kt), [`SnapshotFileExplorerDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/restore/SnapshotFileExplorerDialog.kt), [`TerminalViewModel.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalViewModel.kt), [`TerminalScreen.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalScreen.kt), and unit test suite [`SnapshotTreeBuilderTest.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/test/java/com/teleback/presentation/restore/SnapshotTreeBuilderTest.kt).

---

### Feature 3: Production Release & Reproducible Packaging (Phase 13)
*(Certified from `docs/teleback_phase_breakdown_roadmap.md` & `tools/reproducible_build/`)*
- ProGuard / R8 code shrinking and resource optimization enabled in [`app/build.gradle.kts`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/build.gradle.kts) and [`app/proguard-rules.pro`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/proguard-rules.pro).
- Private keystore signing pipeline and deterministic APK naming (`TeleBack_v<versionName>.apk`).
- Bit-for-bit reproducible packaging verified via [`tools/reproducible_build/verify_build.py`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/tools/reproducible_build/verify_build.py).

---

### Feature 4: Modernized Android 15 App Icon (Concept 3: Decentralized Mesh Vault)

#### 1. Architectural & Aesthetic Summary
Replaced legacy/placeholder app icon drawables with the finalized modern vector artwork ("Concept 3: Decentralized Mesh Vault"), symbolizing encrypted cloud backup over the Telegram decentralized protocol.

#### 2. Implemented Assets
- **Adaptive Launcher Assets**:
  - [`app/src/main/res/drawable/ic_launcher_background.xml`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/res/drawable/ic_launcher_background.xml): Deep Oceanic linear gradient (`#0891B2` at 0% → `#0369A1` at 50% → `#0C1425` at 100%).
  - [`app/src/main/res/drawable/ic_launcher_foreground.xml`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/res/drawable/ic_launcher_foreground.xml): Multi-layer vector graphic comprising:
    1. Hexagonal 6-node interconnected data mesh network (`#00E5FF`, stroke width 2dp, node radius 3.5dp).
    2. Steel vault padlock shackle loop (`#FFFFFF`, opacity 0.9, stroke width 3dp).
    3. Ascending 3D Telegram paper plane (`#FFFFFF` to `#BAE6FD` gradient wing, `#0284C7` shadow fold, `#00E5FF` keel).
    4. Real-time active status beacon (`#10B981` emerald core with `#FFFFFF` halo ring).
- **Adaptive Safe Zone Compliance (108dp Canvas)**:
  - All visual elements are strictly bounded within radius ≤ 29.58dp from the center (54, 54), well inside the mandatory Android 66dp safe zone (radius 33dp).
  - Guarantees 0% clipping and uniform visual appearance across Google Pixel circles, Samsung squircles, Xiaomi rounded rectangles, and Motorola teardrops.
- **Master Asset Preservation**:
  - Standalone SVG preserved at [`assets/icon_concept_3_mesh_vault.svg`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/assets/icon_concept_3_mesh_vault.svg). Temporary preview and scratch exploration files pruned.
