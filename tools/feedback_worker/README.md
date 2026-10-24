# TeleBack Feedback Bot — Cloudflare Worker Webhook

Serverless Telegram Webhook handler running on Cloudflare Workers (100% free, 24/7 uptime, zero servers to maintain).

---

## Architecture Flow

```
[ Android App / User ]
         │
         ▼  (User sends diagnostic / feedback via Telegram)
[@telebackfblogs_bot]
         │
         ▼  (Telegram Webhook POST)
[ Cloudflare Worker (worker.js) ]
         │
         ├─► Forwards card with [Review] [Archive] [Discard] to TeleBack Feedbacks Group
         │
         └─► Handles button clicks (in-place status update & deletion)
```

---

## Deployment Option A: Cloudflare Web Dashboard (Quickest, No CLI needed)

1. **Log in** to your free [Cloudflare Dashboard](https://dash.cloudflare.com/).
2. In the left sidebar, navigate to **Compute (Workers & Pages)** -> **Overview** -> **Create application** -> **Create Worker**.
3. Name it `teleback-feedback-bot` and click **Deploy**.
4. Click **Edit code** in the top right:
   - Clear existing code and paste the full contents of [`worker.js`](./worker.js).
   - Click **Deploy** (or **Save and Deploy**).
5. Add Encrypted Secrets:
   - Go back to the Worker page -> **Settings** tab -> **Variables and Secrets**.
   - Under **Secrets**, click **Add**:
     - Variable name: `BOT_TOKEN`  
       Value: `<Your @telebackfblogs_bot token from BotFather>`  
       Click **Encrypt**.
     - Variable name: `GROUP_ID`  
       Value: `<Your TeleBack Feedbacks Group ID, e.g. -100xxxxxxxxxx>`  
       Click **Encrypt**.
     - *(Optional)* Variable name: `TOPIC_ID`  
       Value: `<Topic ID if forum supergroup has Topics enabled>`  
       Click **Encrypt**.
   - Click **Save and deploy**.
6. **Verify Group Connection**:
   - Add `@telebackfblogs_bot` into your Telegram group.
   - In that group, type `/id` or `/status`.
   - The bot will reply with the exact Group Chat ID (and Topic ID) to confirm connection!
7. Copy your Worker URL:
   - It will look like: `https://teleback-feedback-bot.<your-subdomain>.workers.dev`

---

## Deployment Option B: Wrangler CLI

1. Open a terminal in `tools/feedback_worker/`:
   ```bash
   cd tools/feedback_worker
   ```
2. Log in to Cloudflare:
   ```bash
   npx wrangler login
   ```
3. Set your encrypted secrets:
   ```bash
   npx wrangler secret put BOT_TOKEN
   # (Paste your bot token when prompted)

   npx wrangler secret put GROUP_ID
   # (Paste your group ID when prompted)
   ```
4. Deploy:
   ```bash
   npx wrangler deploy
   ```
5. Copy the output Worker URL (e.g. `https://teleback-feedback-bot.<subdomain>.workers.dev`).

---

## Step 2: 1-Click Webhook Registration & Live Diagnostics

### Option 1 (Easiest - 1 Click!):
Open this link in your browser:
```text
https://teleback-feedback-bot.jaheen-built.workers.dev/set-webhook
```
The Worker automatically registers itself with Telegram using your stored `BOT_TOKEN` and clears any old pending queues.

### Option 2 (Manual):
Replace `<YOUR_BOT_TOKEN>` with your token from `@BotFather` and open in your browser:
```text
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=https://teleback-feedback-bot.jaheen-built.workers.dev/&drop_pending_updates=true
```

---

## Step 3: End-to-End Live Verification

1. **Check Live Diagnostic Health**:
   - Open your Worker URL directly in your browser:
     `https://teleback-feedback-bot.jaheen-built.workers.dev/`
   - It will run a real-time health check directly against Telegram's API (`getMe`, `getWebhookInfo`, and `getChat`) and tell you:
     - Is your `BOT_TOKEN` valid and accepted by Telegram?
     - Is the webhook connected to the worker?
     - Can the bot access your feedback group (`GROUP_ID`)?
     - The exact actionable fix if anything is misconfigured.

2. **Verify in Telegram**:
   - In DM with `@telebackfblogs_bot`: Send `/start` (Bot should reply immediately with the welcome prompt).
   - In your Telegram feedback group: Add `@telebackfblogs_bot` as an Administrator, then send `/id` or `/id@telebackfblogs_bot` (Bot will reply with the chat ID).
   - Send feedback from the Android app (`TeleBack_v2.6.apk`) and confirm ticket card appears in the feedback group with interactive review buttons.
   - The bot will immediately reply: `✅ Ticket received! The TeleBack developer team has been notified.`
3. **Verify Interactive Buttons in Developer Group**:
   - Look inside your **TeleBack Feedbacks** private group.
   - You should see the message card with status `🔴 UNREVIEWED` and 3 inline buttons:
     - Click **`✅ Review`** ➔ Card status updates in-place to `🟢 REVIEWED (by @your_name at timestamp)` and the button swaps to `🔄 Reopen`.
     - Click **`📁 Archive`** ➔ Card updates to `📦 ARCHIVED` and buttons are stripped for permanent history.
     - Click **`🗑 Discard`** ➔ Card is instantly deleted from the group.
