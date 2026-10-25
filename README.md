<div align="center">

  <img width="180" alt="Teleback App Icon" src="https://github.com/user-attachments/assets/8fcf493b-8f16-48cd-9377-a28ca010de8c" />

  <h1>TeleBack</h1>

  <p><strong>Decentralized personal backup, restore, and sync engine for Android 15+ powered by private Telegram groups with authenticated on-device AES-256-GCM encryption.</strong></p>

  <p>
    <a href="https://developer.android.com/about/versions/15"><img src="https://img.shields.io/badge/Platform-Android%2015%2B%20(API%2035)-3DDC84?logo=android&logoColor=white" alt="Android" /></a>
    <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
    <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose" /></a>
    <a href="https://en.wikipedia.org/wiki/Galois/Counter_Mode"><img src="https://img.shields.io/badge/Security-AES--256--GCM-critical" alt="Encryption" /></a>
    <a href="LICENSE.md"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
  </p>

</div>

## 💡 What is TeleBack?

Most cloud storage services require account signups, recurring monthly subscriptions, and proprietary sync clients that store your data in corporate silos.

**TeleBack turns your private Telegram group into an encrypted, infinite personal cloud storage vault.**

- **Zero Sign-Up / Zero Account Ties**: Uses standard Telegram Bot API. No personal credentials, emails, or phone numbers are compiled or transmitted.
- **Client-Side Authenticated Encryption**: Every file is encrypted on-device with **AES-256-GCM** before uploading. Telegram servers see only encrypted ciphertext chunks.
- **CLI Hacker / Terminal Aesthetic**: Built with Jetpack Compose featuring a retro monospace terminal log viewport, live connection telemetry, and high-density 2-column command grids.
- **Seamless Restore Anywhere**: Reinstall the app on any device, enter your master passphrase, and instantly restore all snapshots with their nested folder hierarchies intact.

---

## ✨ Key Features

- 🔐 **End-to-End Cryptography**:
  - Authenticated **AES-256-GCM** with 12-byte cryptographically random IVs and 128-bit integrity tags.
  - PBKDF2 master key derivation (`100,000` rounds with SHA-256).
  - Standard binary header format (`TBK1` magic signature).
  - In-memory key zeroization on app pause/lock.
- 📦 **19 MB Chunk Streaming**:
  - Memory-safe fixed chunk partitioning bypassing Telegram's single-file upload limits.
  - Per-chunk SHA-256 verification and atomic multi-part reassembly.
- 🛡️ **Network & Crash Resilience**:
  - Checkpointed Room database upload journal resuming interrupted transfers without re-uploading parts.
  - Exponential backoff handling Telegram HTTP 429 flood-wait rate limits.
  - Automatic pause when offline and immediate resumption upon connectivity restore.
- 🔄 **Smart Differential Sync**:
  - Non-blocking SAF directory tree comparison skipping unmodified files.
  - Rolling 10-snapshot retention with reference-counted orphan chunk garbage collection.
- ⏱️ **Background WorkManager Engine**:
  - Configurable auto-sync intervals (1h, 6h, 12h, 24h, 7d).
  - Constraint filters: charging-only, unmetered Wi-Fi, battery not low.
- 🔒 **Biometric Sentinel**:
  - Android `BiometricPrompt` guarding app launch, key export, and vault wipes.
- 🐛 **Privacy-First Diagnostics**:
  - Serverless feedback pipeline via Cloudflare Workers and `@telebackfblogs_bot`.
  - Zero-knowledge sanitization scrubbing bot tokens, chat IDs, master keys, and file paths before transmission.

---

## 🚀 Quick Start & Setup

### 1. Prerequisites
- Device running **Android 15+ (API 35)**.
- A Telegram Account.

### 2. Configure Telegram Vault (Takes < 2 Minutes)
1. Open Telegram and search for [`@BotFather`](https://t.me/BotFather).
2. Send `/newbot`, choose a name and username, and copy your **Bot Token**.
3. Create a new **Private Group** in Telegram (e.g., `My Private Vault`).
4. Add your bot to the group as an **Administrator** with permission to post and delete messages.
5. Launch **TeleBack**, enter your Bot Token (the app auto-detects your Chat ID), and set your master passphrase.

---

## 🏗️ Architecture

```
App_TeleBack/
├── app/src/main/java/com/teleback/
│   ├── core/
│   │   ├── cli/            # CLI commands, routing & terminal log streams
│   │   ├── crypto/         # AES-256-GCM, PBKDF2 & Checksum utilities
│   │   ├── diagnostics/    # Zero-knowledge LogSanitizer & CrashHandler
│   │   ├── network/        # OkHttp streaming client & ConnectivityObserver
│   │   ├── service/        # WorkManager SyncWorker & Scheduler
│   │   └── storage/        # Storage Access Framework (SAF) & FileChunker
│   ├── data/
│   │   ├── local/          # Room DB (Snapshots, UploadJournal) & DataStore
│   │   └── model/          # Manifests & Chunk models
│   ├── domain/             # SyncManager & UsageCalculator
│   └── presentation/       # Jetpack Compose UI (Terminal, Dialogs, Themes)
└── tools/
    ├── feedback_bot/       # Python interactive triage bot
    └── feedback_worker/    # Cloudflare Workers serverless webhook handler
```

---

## 🛠️ Building from Source

```bash
# Clone the repository
git clone https://github.com/jaheent/TeleBack.git
cd TeleBack

# Run unit tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```
The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🛡️ Security & Privacy Notice

TeleBack operates under a strict **Zero-Knowledge** architecture:
- Encryption keys never leave your device.
- All file contents and directory names are encrypted before network dispatch.
- If you lose your master passphrase or master key, encrypted backups stored on Telegram **cannot be recovered by anyone**.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
