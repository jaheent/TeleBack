# TeleBack Feedback Bot — Cloudflare Worker Webhook

Serverless Telegram Webhook handler running on Cloudflare Workers (100% free, 24/7 uptime, zero servers to maintain).

---

## Architecture Flow

```
[ Android App / User ]
         │
         ▼  (User sends diagnostic / feedback via Telegram)
[@telebackfeedback_bot]
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
       Value: `<Your @telebackfeedback_bot token from BotFather>`  
       Click **Encrypt**.
     - Variable name: `GROUP_ID`  
       Value: `<Your TeleBack Feedbacks Group ID, e.g. -100xxxxxxxxxx>`  
       Click **Encrypt**.
   - Click **Save and deploy**.
6. Copy your Worker URL:
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

## Step 2: Register the Webhook with Telegram

After deploying the worker, instruct Telegram to send updates to your Cloudflare Worker URL:

Replace `<YOUR_BOT_TOKEN>` and `<YOUR_WORKER_URL>` in this URL and open it in your browser:

```text
https://api.telegram.org/bot<YOUR_BOT_TOKEN>/setWebhook?url=<YOUR_WORKER_URL>
```

#### Expected Telegram API Response:
```json
{
  "ok": true,
  "result": true,
  "description": "Webhook was set"
}
```

---

## Step 3: End-to-End Verification

1. **Test Webhook Status**:
   - Open your Worker URL directly in your web browser: `https://teleback-feedback-bot.<subdomain>.workers.dev`
   - You should see:
     ```json
     {
       "status": "online",
       "service": "TeleBack Feedback Webhook",
       "target_group": "configured"
     }
     ```
2. **Send a Test Feedback**:
   - Open Telegram and message `@telebackfeedback_bot`: `Test diagnostic feedback report`.
   - The bot will immediately reply: `✅ Ticket received! The TeleBack developer team has been notified.`
3. **Verify Interactive Buttons in Developer Group**:
   - Look inside your **TeleBack Feedbacks** private group.
   - You should see the message card with status `🔴 UNREVIEWED` and 3 inline buttons:
     - Click **`✅ Review`** ➔ Card status updates in-place to `🟢 REVIEWED (by @your_name at timestamp)` and the button swaps to `🔄 Reopen`.
     - Click **`📁 Archive`** ➔ Card updates to `📦 ARCHIVED` and buttons are stripped for permanent history.
     - Click **`🗑 Discard`** ➔ Card is instantly deleted from the group.
