/**
 * TeleBack Feedback Bot Webhook - Cloudflare Worker
 * 
 * Serverless Telegram webhook router for receiving diagnostics & feedback
 * from @telebackfeedback_bot and posting interactive ticket cards
 * with inline review actions to the developer group.
 * 
 * Required Secrets (Cloudflare Environment Variables):
 * - BOT_TOKEN: BotFather token for @telebackfeedback_bot
 * - GROUP_ID: Telegram Group ID (e.g. -100xxxxxxxxxx)
 * - SECRET_TOKEN (Optional): Telegram webhook secret token
 */

export default {
  async fetch(request, env, ctx) {
    // Health check endpoint for browser/GET requests
    if (request.method !== "POST") {
      return new Response(
        JSON.stringify({
          status: "online",
          service: "TeleBack Feedback Webhook",
          bot_token: env.BOT_TOKEN ? "configured" : "MISSING",
          group_id: env.GROUP_ID ? `configured (${env.GROUP_ID})` : "MISSING",
          timestamp: new Date().toISOString()
        }, null, 2),
        {
          status: 200,
          headers: { "Content-Type": "application/json" }
        }
      );
    }

    // Verify secret token if configured
    if (env.SECRET_TOKEN) {
      const headerSecret = request.headers.get("X-Telegram-Bot-Api-Secret-Token");
      if (headerSecret !== env.SECRET_TOKEN) {
        return new Response("Unauthorized", { status: 401 });
      }
    }

    if (!env.BOT_TOKEN || !env.GROUP_ID) {
      console.error("CRITICAL: Missing BOT_TOKEN or GROUP_ID in Worker environment secrets.");
      // Return 200 to acknowledge webhook so Telegram does not loop retries, but log clearly
      return new Response("Misconfigured: Missing BOT_TOKEN or GROUP_ID", { status: 200 });
    }

    try {
      const update = await request.json();

      // 1. Handle incoming user messages (private chat only)
      if (update.message) {
        await handleIncomingMessage(update.message, env);
      }

      // 2. Handle interactive inline button clicks (callbacks)
      if (update.callback_query) {
        await handleCallbackQuery(update.callback_query, env);
      }

      // Always return 200 OK to Telegram to clear the pending update queue
      return new Response("OK", { status: 200 });
    } catch (err) {
      console.error("Webhook processing error:", err);
      // Return 200 to Telegram to prevent retry storms; errors are captured in Cloudflare logs
      return new Response("OK", { status: 200 });
    }
  }
};

/**
 * Handle direct messages sent to @telebackfeedback_bot
 */
async function handleIncomingMessage(message, env) {
  // Only process private chats (ignore messages inside groups)
  if (message.chat?.type !== "private") {
    return;
  }

  const text = message.text || "";

  // Handle /start and /help commands
  if (text.startsWith("/start") || text.startsWith("/help")) {
    const welcomeText = 
      "👋 *Welcome to TeleBack Feedback & Diagnostics Bot!*\n\n" +
      "To submit a report, simply paste your copied crash dump or type your feedback below.\n" +
      "It will be forwarded directly to the developer team with zero-knowledge privacy.";

    await safeSendMessage(env.BOT_TOKEN, {
      chat_id: message.chat.id,
      text: welcomeText,
      parse_mode: "Markdown"
    });
    return;
  }

  // Build ticket card
  const userInfo = message.from?.username 
    ? `@${message.from.username}` 
    : `User ${message.from?.id || "Unknown"}`;

  const header = 
    `📩 *NEW TICKET* from ${userInfo}\n` +
    `━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
    `Status: 🔴 *UNREVIEWED*\n\n`;

  const reportContent = message.text || message.caption || (message.document ? "[Document attached]" : "[Empty report]");
  const fullCard = header + reportContent;

  // Forward document or message to developer group
  let sendResult;
  if (message.document) {
    sendResult = await safeSendDocument(env.BOT_TOKEN, {
      chat_id: env.GROUP_ID,
      document: message.document.file_id,
      caption: fullCard.slice(0, 1024),
      reply_markup: buildTicketMarkup("unreviewed"),
      parse_mode: "Markdown"
    });
  } else {
    sendResult = await safeSendMessage(env.BOT_TOKEN, {
      chat_id: env.GROUP_ID,
      text: fullCard.slice(0, 4096),
      reply_markup: buildTicketMarkup("unreviewed"),
      parse_mode: "Markdown"
    });
  }

  if (!sendResult.ok) {
    console.error("Failed to post ticket card to group:", sendResult.description);
  }

  // Send acknowledgement back to user
  await safeSendMessage(env.BOT_TOKEN, {
    chat_id: message.chat.id,
    text: "✅ *Ticket received!* The TeleBack developer team has been notified.",
    reply_to_message_id: message.message_id,
    parse_mode: "Markdown"
  });
}

/**
 * Handle inline button actions in the developer group
 */
async function handleCallbackQuery(call, env) {
  const adminName = call.from?.username 
    ? `@${call.from.username}` 
    : (call.from?.first_name || "Admin");

  const now = new Date();
  const timestamp = now.toISOString().replace(/T/, " ").replace(/\..+/, "") + " UTC";
  const currentText = call.message?.text || call.message?.caption || "";

  if (call.data === "review") {
    let updatedText = currentText
      .replace("🔴 *UNREVIEWED*", `🟢 *REVIEWED* (by ${adminName} at ${timestamp})`)
      .replace("🔴 UNREVIEWED", `🟢 REVIEWED (by ${adminName} at ${timestamp})`);

    await editMessage(env.BOT_TOKEN, call.message, updatedText, buildTicketMarkup("reviewed"));
    await telegramApi(env.BOT_TOKEN, "answerCallbackQuery", {
      callback_query_id: call.id,
      text: "Ticket marked as Reviewed! 🟢"
    });
  } else if (call.data === "reopen") {
    let updatedText = currentText;
    if (updatedText.includes("🟢 *REVIEWED*")) {
      updatedText = updatedText.replace(/🟢 \*REVIEWED\* \(by .*?\)/, "🔴 *UNREVIEWED*");
    } else if (updatedText.includes("🟢 REVIEWED")) {
      updatedText = updatedText.replace(/🟢 REVIEWED \(by .*?\)/, "🔴 UNREVIEWED");
    }

    await editMessage(env.BOT_TOKEN, call.message, updatedText, buildTicketMarkup("unreviewed"));
    await telegramApi(env.BOT_TOKEN, "answerCallbackQuery", {
      callback_query_id: call.id,
      text: "Ticket Reopened 🔴"
    });
  } else if (call.data === "archive") {
    let archivedText = currentText
      .replace("🔴 *UNREVIEWED*", `📦 *ARCHIVED* (by ${adminName})`)
      .replace("🔴 UNREVIEWED", `📦 ARCHIVED (by ${adminName})`)
      .replace(/🟢 \*REVIEWED\* \(by .*?\)/, `📦 *ARCHIVED* (by ${adminName})`)
      .replace(/🟢 REVIEWED \(by .*?\)/, `📦 ARCHIVED (by ${adminName})`);

    // Archived: remove inline buttons
    await editMessage(env.BOT_TOKEN, call.message, archivedText, null);
    await telegramApi(env.BOT_TOKEN, "answerCallbackQuery", {
      callback_query_id: call.id,
      text: "Ticket Archived 📦"
    });
  } else if (call.data === "discard") {
    await telegramApi(env.BOT_TOKEN, "deleteMessage", {
      chat_id: call.message.chat.id,
      message_id: call.message.message_id
    });
    await telegramApi(env.BOT_TOKEN, "answerCallbackQuery", {
      callback_query_id: call.id,
      text: "Ticket Discarded 🗑"
    });
  }
}

/**
 * Robust SendMessage with fallback if Markdown entity parsing fails
 */
async function safeSendMessage(botToken, payload) {
  let res = await telegramApi(botToken, "sendMessage", payload);
  if (!res.ok && payload.parse_mode && res.description?.toLowerCase().includes("parse")) {
    console.warn("Markdown parse failed, retrying plain text:", res.description);
    const plainPayload = { ...payload };
    delete plainPayload.parse_mode;
    res = await telegramApi(botToken, "sendMessage", plainPayload);
  }
  return res;
}

/**
 * Robust SendDocument with fallback if Markdown entity parsing fails
 */
async function safeSendDocument(botToken, payload) {
  let res = await telegramApi(botToken, "sendDocument", payload);
  if (!res.ok && payload.parse_mode && res.description?.toLowerCase().includes("parse")) {
    console.warn("Document caption markdown parse failed, retrying plain text:", res.description);
    const plainPayload = { ...payload };
    delete plainPayload.parse_mode;
    res = await telegramApi(botToken, "sendDocument", plainPayload);
  }
  return res;
}

/**
 * Edit message text or caption with fallback
 */
async function editMessage(botToken, message, text, replyMarkup) {
  if (message.text) {
    const payload = {
      chat_id: message.chat.id,
      message_id: message.message_id,
      text: text.slice(0, 4096),
      parse_mode: "Markdown"
    };
    if (replyMarkup) payload.reply_markup = replyMarkup;
    let res = await telegramApi(botToken, "editMessageText", payload);
    if (!res.ok && res.description?.toLowerCase().includes("parse")) {
      delete payload.parse_mode;
      res = await telegramApi(botToken, "editMessageText", payload);
    }
    return res;
  } else {
    const payload = {
      chat_id: message.chat.id,
      message_id: message.message_id,
      caption: text.slice(0, 1024),
      parse_mode: "Markdown"
    };
    if (replyMarkup) payload.reply_markup = replyMarkup;
    let res = await telegramApi(botToken, "editMessageCaption", payload);
    if (!res.ok && res.description?.toLowerCase().includes("parse")) {
      delete payload.parse_mode;
      res = await telegramApi(botToken, "editMessageCaption", payload);
    }
    return res;
  }
}

/**
 * Construct Inline Keyboard markup for ticket status
 */
function buildTicketMarkup(status = "unreviewed") {
  if (status === "unreviewed") {
    return {
      inline_keyboard: [
        [
          { text: "✅ Review", callback_data: "review" },
          { text: "📁 Archive", callback_data: "archive" },
          { text: "🗑 Discard", callback_data: "discard" }
        ]
      ]
    };
  } else if (status === "reviewed") {
    return {
      inline_keyboard: [
        [
          { text: "🔄 Reopen", callback_data: "reopen" },
          { text: "📁 Archive", callback_data: "archive" },
          { text: "🗑 Discard", callback_data: "discard" }
        ]
      ]
    };
  }
  return null;
}

/**
 * Helper to execute Telegram Bot API calls
 */
async function telegramApi(botToken, method, payload) {
  try {
    const url = `https://api.telegram.org/bot${botToken}/${method}`;
    const res = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!data.ok) {
      console.error(`Telegram API Error [${method}]:`, data.error_code, data.description);
    }
    return data;
  } catch (err) {
    console.error(`Network fetch failed for [${method}]:`, err);
    return { ok: false, description: err.message };
  }
}
