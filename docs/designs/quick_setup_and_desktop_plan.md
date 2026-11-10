# Quick Deep-Link Setup, QR Clone & Desktop App (Windows / Linux / macOS) Plan

## Goal Description
1. **Replace Manual Setup with Quick Deep-Link Setup + Dedicated Clone Button**:
   - Replace the old Manual setup flow in [`OnboardingDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/onboarding/OnboardingDialog.kt) directly with the **Quick Deep-Link Setup Wizard**:
     - **Step 1**: `[Open @BotFather]` + `[Paste & Verify Token]` (validates token via `getMe()` and displays `@bot_username`) + a dedicated **`[Scan QR / Paste Setup Code]`** button that switches into the 1-step Clone Setup view.
     - **Step 2**: One-tap **`[Choose Group in Telegram (Auto-Admin)]`** button launching `tg://resolve?domain=<bot_username>&startgroup=teleback_setup&admin=post_messages+edit_messages+delete_messages+pin_messages`, backed by `my_chat_member` auto-discovery on app resume and a selectable group list.
     - **Step 3**: Passphrase (`New Vault` / `Existing Vault`) + Target Folder.
   - In [`KeysDialog.kt`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt), add **"Clone Setup to New Device"** to export an AES-256-GCM encrypted `TBSETUP1:<base64>` code and render an in-app QR Code.
2. **Cross-Platform Desktop App (`Windows` / `Linux` / `macOS`)**:
   - Add a **Compose for Desktop (`:desktop` JVM module)** implementing **Layout 1 (IDE Split-Commander)** and sharing TeleBack's `TBK1` AES-256-GCM crypto format, 19 MB chunk streamer, pointer-chained manifest format, and `TBSETUP1:` QR/Code setup codec—so backups created on Android restore on Windows/Linux/macOS and vice versa.
3. **Queue Archival**:
   - Archive **Feature 1: High-Speed Mode & Custom Bot API Server Support** in [`docs/queue.md`](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/queue.md).

---

## Selected Desktop Layout: Layout 1 (IDE Split-Commander)

![Desktop Layout 1: IDE Split-Commander](C:/Users/Jaheen/.gemini/antigravity/brain/dcaeca33-b4e5-4d78-9a6d-9be0bfe03252/desktop_layout_1_split_ide.svg)

- **Left Sidebar (`250dp`)**: Vault telemetry card, vertical command rail (`[1] sync` through `[0] setup`), and drag-and-drop file upload zone.
- **Top Main Pane**: Always-open Hierarchical Accordion Tree Explorer with search filter, snapshot selector dropdown, and inline `Folder` / `Downloads` extraction buttons.
- **Bottom Docked Pane (`210dp`)**: Read-only live monospace terminal log stream (CLI input prompt dropped per review).

---

## Proposed Changes

### Phase 1: Queue Archival & Core Protocol / Codecs
*Stability Checkpoint: `:app` compiles and passes `SetupCloneCodecTest` unit tests.*

#### [MODIFY] [queue.md](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/docs/queue.md)
- Move **Feature 1: High-Speed Mode & Custom Bot API Server Support** from `📋 Staged Features for Execution` to `📦 Archived / Deferred Features` at the bottom of `docs/queue.md`.

#### [MODIFY] [libs.versions.toml](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/gradle/libs.versions.toml) & [app/build.gradle.kts](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/build.gradle.kts)
- Add pure-Java `com.google.zxing:core:3.5.3` (`zxing-core`) to `[versions]` / `[libraries]` in `gradle/libs.versions.toml` and `implementation(libs.zxing.core)` in `app/build.gradle.kts`.

#### [MODIFY] [TelegramModels.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/core/network/TelegramModels.kt) & [TelegramApiClient.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/core/network/TelegramApiClient.kt)
- Update `TelegramApiClient.getUpdates()` query URL from `limit=20` to `limit=100`.
- Add `TelegramChatMember` and `TelegramChatMemberUpdated` to `TelegramModels.kt`:
```kotlin
@Serializable
data class TelegramChatMember(
    val status: String,
    val user: TelegramUser? = null
)

@Serializable
data class TelegramChatMemberUpdated(
    val chat: TelegramChat,
    val date: Long,
    @SerialName("new_chat_member") val newChatMember: TelegramChatMember? = null
)

@Serializable
data class TelegramUpdate(
    @SerialName("update_id") val updateId: Long,
    val message: TelegramMessage? = null,
    @SerialName("channel_post") val channelPost: TelegramMessage? = null,
    @SerialName("my_chat_member") val myChatMember: TelegramChatMemberUpdated? = null
)
```

#### [MODIFY] [SyncManager.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/domain/SyncManager.kt)
- Add `suspend fun verifyBotToken(token: String): Result<TelegramUser>`:
  - Calls `TelegramApiClient(token.trim()).getMe()`. On success, calls `prefs.saveBotToken(token.trim())` and returns the `TelegramUser` (with `@username`).
- Add `suspend fun discoverCandidateChats(): Result<List<TelegramChat>>`:
  - Iterates chronologically through `getUpdates()` (`limit=100`), maintaining a `LinkedHashMap<Long, TelegramChat>()` of active non-private chats (`chat.type != "private"`).
  - For `myChatMember`: if `newChatMember?.status in setOf("left", "kicked")`, removes `chat.id` from the map; otherwise puts `chat.id to chat`.
  - For `message?.chat` and `channelPost?.chat`: puts `chat.id to chat` if `chat.type != "private"`.
  - Returns `activeChats.values.toList().asReversed()` (most recent first).
- Update `detectChatId()` to call `discoverCandidateChats()`, pick the first chat, save via `prefs.saveChat(chat.id, title)`, and return `Pair(chat.id, title)`.

#### [NEW] [SetupCloneCodec.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/core/crypto/SetupCloneCodec.kt)
- Defines the cross-device encrypted clone payload and codec:
```kotlin
@Serializable
data class SetupClonePayload(
    val botToken: String,
    val chatId: Long,
    val chatTitle: String,
    val masterKeyBase64: String
)
```
- `fun encode(payload: SetupClonePayload, transferPassphrase: String): Result<String>`:
  - Requires `transferPassphrase.trim().length >= 6`.
  - Derives a 256-bit transfer key via `CryptoEngine.deriveKeyFromPassphrase(transferPassphrase.trim())`.
  - Serializes `payload` to UTF-8 JSON bytes, encrypts via `CryptoEngine.encryptBytes(jsonBytes, transferKey)` (`TBK1` header + 12B IV + AES-GCM ciphertext + 16B tag), encodes with `Base64.getEncoder().encodeToString(encrypted)`, and returns `"TBSETUP1:$base64"`.
- `fun decode(code: String, transferPassphrase: String): Result<SetupClonePayload>`:
  - Validates and strips `"TBSETUP1:"` prefix, derives `transferKey` via `CryptoEngine.deriveKeyFromPassphrase(transferPassphrase.trim())`, decodes Base64, decrypts via `CryptoEngine.decryptBytes(encrypted, transferKey)`, and decodes `SetupClonePayload` from JSON.

#### [NEW] [QrCodeCodec.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/core/util/QrCodeCodec.kt)
- Platform-agnostic QR wrapper around `com.google.zxing` (`QRCodeWriter` and `QRCodeReader`):
  - `fun encodeToMatrix(content: String, size: Int = 512): Array<BooleanArray>`: Encodes `content` with `ErrorCorrectionLevel.M` and `MARGIN = 1` into a boolean pixel grid (usable by both Android Canvas/`Bitmap` and Desktop Compose Canvas/`BufferedImage`).
  - `fun decodeFromArgbPixels(pixels: IntArray, width: Int, height: Int): Result<String>`: Wraps `RGBLuminanceSource(width, height, pixels)` + `BinaryBitmap(HybridBinarizer(source))` and decodes via `QRCodeReader().decode(bitmap)`.

---

### Phase 2: Android Quick Deep-Link & QR Clone UI
*Stability Checkpoint: `:app:compileDebugKotlin` succeeds with updated `OnboardingDialog` and `KeysDialog`.*

#### [MODIFY] [OnboardingDialog.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/onboarding/OnboardingDialog.kt)
- **Step 1 (Quick Bot Setup + Dedicated Clone Button)**:
  - Top action button: **`[Scan QR / Paste Setup Code]`** toggles `isCloneMode = true`, showing:
    1. `[Scan QR Image]` button using `rememberLauncherForActivityResult(ActivityResultContracts.GetContent())` (`"image/*"`) → `context.contentResolver.openInputStream(uri)` → `BitmapFactory.decodeStream` → extracts `IntArray` pixels → `QrCodeCodec.decodeFromArgbPixels`.
    2. `[Paste Setup Code]` button reading `ClipboardManager.primaryClip` into `cloneCodeInput`.
    3. `Transfer / Vault Passphrase` input field (`PasswordVisualTransformation`).
    4. `[Select Target Folder (SAF)]` button (`onPickFolderClick`).
    5. `[Unlock & Clone Setup]` button calling `onApplyCloneSetup(cloneCodeInput, clonePassphraseInput)`.
  - Standard Step 1 below: `[Open @BotFather in Telegram]` + `[Paste & Verify Token]` (reads clipboard if `tokenInput` is blank, calls `onVerifyToken(token)` to verify via `getMe()`, displays `Verified Bot: @$verifiedBotUsername` in `TerminalMint`, and advances to Step 2).
- **Step 2 (1-Tap Native Group Picker & Auto-Discovery)**:
  - Primary CTA button: **`[Choose Group in Telegram (Auto-Admin)]`** launching `Intent(Intent.ACTION_VIEW, Uri.parse("tg://resolve?domain=$botUsername&startgroup=teleback_setup&admin=post_messages+edit_messages+delete_messages+pin_messages"))` (with fallback to `https://t.me/$botUsername?startgroup=...`).
  - Uses `DisposableEffect(lifecycleOwner, step)` with `LifecycleEventObserver` to invoke `onDiscoverChats()` automatically on `Lifecycle.Event.ON_RESUME` when `step == 2`.
  - Renders `candidateChats: List<TelegramChat>` as selectable group rows (`onSelectChat(chat)`), plus a `[Refresh / Auto-Detect Groups]` button.
- **Step 3 (Security & Target Folder)**:
  - Preserves `New Vault` / `Existing Vault` passphrase setup and `[Select Target Folder (SAF)]`.

#### [MODIFY] [KeysDialog.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/settings/KeysDialog.kt)
- Extend parameters with `botToken: String? = null`, `chatId: Long? = null`, `chatTitle: String? = null`.
- Add **"Clone Setup to New Device (QR / Code)"** section:
  - Prompts for a `Transfer / Vault Passphrase` (min 6 chars) → calls `SetupCloneCodec.encode(SetupClonePayload(botToken, chatId, chatTitle, currentMasterKey), passphrase)` → renders the QR matrix via Compose `Canvas` (`QrCodeCodec.encodeToMatrix(code, 256)`) along with `[Copy Setup Code]` and `[Share Code]`.

#### [MODIFY] [TerminalViewModel.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalViewModel.kt) & [TerminalScreen.kt](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/app/src/main/java/com/teleback/presentation/terminal/TerminalScreen.kt)
- In `TerminalViewModel.kt`:
  - Expose `val chatId = prefs.chatId.stateIn(viewModelScope, SharingStarted.Eagerly, null)`.
  - Expose `val verifiedBotUsername: StateFlow<String?>` and `val candidateChats: StateFlow<List<TelegramChat>>`.
  - Add `fun verifyAndSaveBotToken(token: String, onVerified: () -> Unit = {})`, `fun discoverCandidateChats()`, `fun selectCandidateChat(chat: TelegramChat)`, and `fun applyClonedSetup(code: String, passphrase: String): Result<Unit>`.
- In `TerminalScreen.kt`:
  - Pass the new states and callbacks into `OnboardingDialog` and pass `botToken`, `chatId`, and `chatTitle` into `KeysDialog`.

---

### Phase 3: Cross-Platform `:desktop` Module & IDE Split-Commander UI
*Stability Checkpoint: `:desktop:compileKotlin` succeeds and desktop application launches with Layout 1.*

#### [MODIFY] [settings.gradle.kts](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/settings.gradle.kts), [build.gradle.kts](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/build.gradle.kts), & [libs.versions.toml](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/gradle/libs.versions.toml)
- Add `include(":desktop")` in `settings.gradle.kts`.
- Declare `kotlin-jvm`, `jetbrains-compose` (`1.7.1`), and `kotlinx-coroutines-swing` (`1.9.0`) in `gradle/libs.versions.toml` and root `build.gradle.kts`.

#### [NEW] [desktop/build.gradle.kts](file:///c:/Users/Jaheen/Documents/Projects/TeleBack/desktop/build.gradle.kts)
- Apply `org.jetbrains.kotlin.jvm`, `org.jetbrains.kotlin.plugin.compose`, `org.jetbrains.compose`, and `org.jetbrains.kotlin.plugin.serialization`.
- Share pure-JVM sources from `:app` by including specific package directories (`core/crypto`, `core/network/TelegramApiClient.kt`, `core/network/TelegramModels.kt`, `core/network/RetryPolicy.kt`, `core/storage/FileChunker.kt`, `core/storage/ManifestManager.kt`, `core/util/QrCodeCodec.kt`, `core/util/SizeFormat.kt`, `core/cli`, `core/diagnostics/LogSanitizer.kt`, `data/model`, `domain/UsageCalculator.kt`, `presentation/restore/SnapshotTreeNode.kt`, `presentation/theme`) via `sourceSets["main"].kotlin` filtering out Android-only classes (`ConnectivityObserver.kt`, `BiometricHelper.kt`, `NotificationHelper.kt`, `SafStorageHelper.kt`, `Theme.kt` Android `LocalContext` branch).
- Configure `compose.desktop.application` with `mainClass = "com.teleback.desktop.MainKt"` and `TargetFormat.Msi`, `TargetFormat.Exe`, `TargetFormat.Deb`, `TargetFormat.Rpm`, `TargetFormat.Dmg`.

#### [NEW] `desktop/src/main/kotlin/com/teleback/desktop/`
- **`Main.kt`**: Initializes Compose Desktop `Window(title = "TeleBack Desktop v2.9")`, wires global keyboard shortcuts (`0`–`8`), and mounts `DesktopAppScreen`.
- **`DesktopPreferences.kt`**: Persists `preferences.json` (`botToken`, `chatId`, `chatTitle`, `folderPath`, `masterKeyBase64`, `themeId`, `autoSyncEnabled`, `syncIntervalHours`) in the OS app-data folder (`%APPDATA%/TeleBack` on Windows, `~/.config/teleback` on Linux, `~/Library/Application Support/TeleBack` on macOS).
- **`DesktopCatalogStore.kt`**: Persists `catalog.json` (snapshots list and pending upload checkpoints) atomically using `kotlinx.serialization.json.Json`.
- **`DesktopStorageHelper.kt`**: Native `java.nio.file.Path` implementation for recursive SHA-256 directory scanning, 19 MB chunk slicing, and atomic file extraction to the linked target folder or `~/Downloads/TeleBack`.
- **`DesktopSyncManager.kt`**: Desktop-native equivalent of `SyncManager` orchestrating `TelegramApiClient`, `CryptoEngine`, `FileChunker`, `ManifestManager`, `DesktopStorageHelper`, and `DesktopCatalogStore` (full backup, differential sync, selective single-file/batch extraction, cloud snapshot scan, 10-snapshot retention pruning, `verifyBotToken`, and `discoverCandidateChats`).
- **`DesktopAppScreen.kt`**: Implements **Layout 1 (IDE Split-Commander)**:
  - Left Sidebar (`250dp`): Vault telemetry card, vertical command rail (`[1] sync` .. `[0] setup`), and file upload picker/drop zone.
  - Top Main Pane: Always-open Hierarchical Accordion Tree Explorer (`SnapshotTreeBuilder`) with search filter, snapshot selector dropdown, `TriStateCheckbox` folder selection, and inline `[Folder]` / `[Downloads]` extraction buttons + 3s `ExtractionBanner`.
  - Bottom Docked Pane (`210dp`): Read-only live monospace terminal log stream with level coloring (`INFO`, `OK`, `WARN`, `ERR`, `SYNC`).
  - Dialogs for `[0] Quick Setup / Clone QR`, `[4] Cloud Snapshot Manager`, and `[8] Settings / Keys / Clone QR Export`.

---

## Verification Plan

### Automated Tests
*(Executed only after explicit user confirmation per `AGENTS.md`)*
- Create `app/src/test/java/com/teleback/core/crypto/SetupCloneCodecTest.kt` with 5 unit tests:
  1. `encryptAndDecrypt_roundTrip_preservesAllFields()`
  2. `decrypt_withWrongPassphrase_returnsFailure()`
  3. `decrypt_withInvalidPrefixOrCorruptBase64_returnsFailure()`
  4. `qrCodeCodec_roundTrip_encodesAndDecodesSetupPayload()`
  5. `telegramUpdate_deserializesMyChatMemberAndFiltersKickedStatus()`
- Command (upon user approval):
  ```powershell
  .\gradlew.bat testDebugUnitTest --console=plain
  ```

### Manual Verification
1. Verify Step 1 `[Scan QR / Paste Setup Code]` button and Quick Deep-Link flow in `OnboardingDialog`.
2. Verify Desktop compilation and launch (`.\gradlew.bat :desktop:run` upon approval) and cross-platform restore of Android snapshots.
