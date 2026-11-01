# TeleBack — Architectural Blueprint & Refined Delivery Roadmap (v2)
**Target:** Android 15+ (API 35) | **Aesthetic:** Developer CLI / Cyberpunk Terminal | **Cloud Backend:** Telegram Private Group

---

## 1. Executive Summary & Core Updates

TeleBack is an Android 15+ backup, restore, and synchronization utility using a private Telegram Group as an encrypted, decentralized personal cloud with zero account signup.

### Key Refinements (From User Testing & Review)
1. **Full Subfolder & Directory Tree Engine:** Recursive scanning, differential comparison, and full preservation of nested subfolders and empty directories on backup and restore.
2. **10-Snapshot Cloud Pruning:** Automatically keeps the 10 most recent snapshots. When the 11th snapshot is committed, the oldest snapshot's manifest and file messages are deleted from the Telegram group via `deleteMessage` and removed from local storage.
3. **Live Connectivity Status:** Real-time network monitoring via `ConnectivityManager` displaying clean, unambiguous `● ONLINE` (green) or `● OFFLINE` (red).
4. **2-Column Command Chips Grid:** Replaced horizontal scroll and redundant menus with a clean, high-density 2-column command grid requiring zero scrolling.
5. **Customization Suite:** A dedicated command combining:
   - **6 Themes:** 3 Dark (*Matrix Mint*, *OLED Amber*, *Dracula Neon*) + 3 Light (*Solarized Paper*, *Nord Frost*, *Minimal Slate*).
   - **Modern App Icon Switching:** Icon A ("The Neon Terminal Shield") as default, with dynamic switching options between Icon A, B, and C.
6. **Master Key Management:** Prominent Export/Copy Key and Import Key/Custom Passphrase capabilities so users never lose access to their encrypted cloud backups across devices or reinstallations.
7. **First-Run Welcome Popup:** Friendly introductory dialog explaining how TeleBack turns Telegram into an encrypted cloud, introducing key actions.
8. **Command Structure:** Renamed `init` to `setup` and positioned as the final command.

---

## 2. Updated Numbered Command Layout (2-Column Grid)

```text
┌── TELEBACK TERMINAL ──────────────────────────────────────┐
│  [1] sync      Diff update      │  [2] backup    Full backup     │
│  [3] restore   Pick snapshot    │  [4] snapshot  Tagged version  │
│  [5] status    Diff & folder    │  [6] test      Bot & cloud link│
│  [7] custom    Themes & icons   │  [8] keys      Export / import │
│  [9] clear     Clear logs       │  [0] setup     Setup & config  │
└───────────────────────────────────────────────────────────┘
```
*(Typing a number `1`–`0` or tapping any chip executes the command immediately)*

---

## 3. Phased Implementation Roadmap

### Phase 1: Core Engine & Cloud Snapshot Pruning *(Current Phase)*
- [ ] **Recursive Subfolder & Empty Directory Engine:**
  - Enhance `SafStorageHelper` to index directory hierarchy paths and empty folders.
  - Reconstruct full folder tree on restore without flattening or collision.
- [ ] **10-Snapshot Automatic Cloud Pruning:**
  - Implement `deleteMessage` in `TelegramApiClient`.
  - When snapshot count > 10, delete oldest cloud manifest message from Telegram Group and prune Room DB.
- [ ] **Real-Time Network Connectivity Monitor:**
  - Implement `ConnectivityObserver` using Android `ConnectivityManager.NetworkCallback`.
  - Display clean, instant `● ONLINE` / `● OFFLINE` status in the terminal header.
- [ ] **Unit Tests:** Verify subfolder tree generation and pruning threshold logic.

### Phase 2: UI Overhaul, 2-Column Grid & Welcome Modal
- [ ] **2-Column Command Grid:**
  - Remove scrollable horizontal chip row and separate menu card.
  - Implement compact 2-column responsive grid above the prompt.
- [ ] **First-Run Welcome Dialog:**
  - Create high-contrast introductory pop-up for fresh installations explaining core concept, zero-signup architecture, and quick actions.
- [ ] **Command Reordering:**
  - Rename `init` to `setup`, update aliases and positioning to `[0]`.

### Phase 3: Customization Suite & Master Key Management
- [ ] **Customization Menu `[7] custom`:**
  - **Themes (6 Total):**
    - *Matrix Mint* (Default Dark)
    - *OLED Amber* (Pure Black + Cyber Amber)
    - *Dracula Neon* (Deep Navy + Cyan/Purple)
    - *Solarized Paper* (Light Warm Cream + Teal)
    - *Nord Frost* (Light Arctic Snow + Polar Blue)
    - *Minimal Slate* (Light Pure White + Charcoal)
  - **Icon Selection:**
    - Icon A: "Neon Terminal Shield" (Default vector)
    - Icon B: "Dual-Orbit Sync"
    - Icon C: "Monolithic Data Vault"
    - Dynamic activity-alias switcher for Android launcher.
- [ ] **Master Key Export / Import:**
  - Copy Master Key to clipboard with warning prompts.
  - Import existing Master Key or set a custom memorable passphrase with PBKDF2 key derivation.

### Phase 4: Review, Approval & Final APK Build
- [ ] Complete end-to-end testing across all phases.
- [ ] Present changes for user testing and explicit approval.
- [ ] Build and deliver final `app-debug.apk` upon approval.
