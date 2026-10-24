# TeleBack Feedback Bot Server

A lightweight triage bot forwarding incoming diagnostics and crash reports into your private **`TeleBack Feedbacks`** group with interactive inline buttons (`Review`, `Archive`, `Discard`).

## Features
- **Zero App Secrets**: The Android app contains no tokens or credentials.
- **Interactive Inline Buttons**:
  - `[ ✅ Review ]`: Stamps ticket with admin name and timestamp, swaps button to `[ 🔄 Reopen ]`.
  - `[ 📁 Archive ]`: Marks the ticket as archived and locks it into history.
  - `[ 🗑 Discard ]`: Deletes spam/test messages from the group instantly.
- **Document & Text Support**: Automatically handles both text tickets and attached `.txt` crash logs.

## Quick Start (Local)

1. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
2. Run the bot:
   ```bash
   python main.py
   ```

## 24/7 Free Deployment on Render.com

1. Create a free account at [render.com](https://render.com).
2. Click **New +** -> **Background Worker**.
3. Point to your repository root or subdirectory `tools/feedback_bot`.
4. Set Build Command: `pip install -r requirements.txt`
5. Set Start Command: `python main.py`
6. Add Environment Variables:
   - `BOT_TOKEN`: `<your_bot_token_from_botfather>`
   - `GROUP_ID`: `<your_feedback_group_id>`
7. Click **Deploy**.
