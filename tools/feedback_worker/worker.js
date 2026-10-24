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
 * - TOPIC_ID / THREAD_ID (Optional): Forum Topic ID if group has Topics enabled
 * - SECRET_TOKEN (Optional): Telegram webhook secret token
 */

export default {
  async fetch(request, env, ctx) {
    const targetGroupId = String(env.GROUP_ID || "").trim().replace(/^["']|["']$/g, '');
    const topicId = (env.TOPIC_ID || env.THREAD_ID) ? String(env.TOPIC_ID || env.THREAD_ID).trim() : null;

    // Health check endpoint for browser/GET requests
    if (request.method !== "POST") {
      return new Response(
        JSON.stringify({
          status: "online",
          service: "TeleBack Feedback Webhook",
          bot_token: env.BOT_TOKEN ? "configured" : "MISSING",
          group_id: targetGroupId ? `configured (${targetGroupId})` : "MISSING",
          topic_id: topicId ? `configured (${topicId})` : "none",
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

    if (!env.BOT_TOKEN || !targetGroupId) {
      console.error("CRITICAL: Missing BOT_TOKEN or GROUP_ID in Worker environment secrets.");
      // Return 200 to acknowledge webhook so Telegram does not loop retries, but log clearly
      return new Response("Misconfigured: Missing BOT_TOKEN or GROUP_ID", { status: 200 });
    }

    try {
      const update = await request.json();

      // 1. Handle incoming user messages
      if (update.message) {
        await handleIncomingMessage(update.message, env, targetGroupId, topicId);
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
 * Handle messages sent to @telebackfeedback_bot
 */
async function handleIncomingMessage(message, env, targetGroupId, topicId) {
  const text = message.text || "";

  // 1. Group / Supergroup diagnostic handler:
  // If the bot is added to a group, typing /id or /status immediately replies with the chat ID
  if (message.chat?.type === "group" || message.chat?.type === "supergroup") {
    if (text.startsWith("/id") || text.startsWith("/chatid") || text.startsWith("/status") || text.startsWith("/help")) {
      const topicStr = message.message_thread_id ? `\n🧵 *Topic/Thread ID:* \`${message.message_thread_id}\`` : "";
      await safeSendMessage(env.BOT_TOKEN, {
        chat_id: message.chat.id,
        message_thread_id: message.message_thread_id,
        text: `🤖 *TeleBack Feedback Bot Active*\n\n` +
              `📍 *This Group Chat ID:* \`${message.chat.id}\`\n` +
              `🏷️ *Group Title:* ${message.chat.title || "Group"}` +
              topicStr + `\n\n` +
              `✅ Bot is connected to this chat.\n` +
              `To route feedback tickets here, configure in Cloudflare Secrets:\n` +
              `• \`GROUP_ID\` = \`${message.chat.id}\`` +
              (message.message_thread_id ? `\n• \`TOPIC_ID\` = \`${message.message_thread_id}\`` : ""),
        parse_mode: "Markdown"
      });
    }
    return;
  }

  // Only forward direct private chats from users
  if (message.chat?.type !== "private") {
    return;
  }

  // Handle /start, /help, or /status commands in DM
  if (text.startsWith("/start") || text.startsWith("/help") || text.startsWith("/status")) {
    const welcomeText = 
      "👋 *Welcome to TeleBack Feedback & Diagnostics Bot!*\n\n" +
      "To submit a report, simply paste your copied crash dump or type your feedback below.\n" +
      "It will be forwarded directly to the developer team with zero-knowledge privacy.\n\n" +
      "⚙️ *Group Config Check:* Target Group ID is configured.";

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

  const parsedThreadId = topicId ? parseInt(topicId, 10) : undefined;

  // Forward document or message to developer group
  let sendResult;
  if (message.document) {
    const payload = {
      chat_id: targetGroupId,
      document: message.document.file_id,
      caption: fullCard.slice(0, 1024),
      reply_markup: buildTicketMarkup("unreviewed"),
      parse_mode: "Markdown"
    };
    if (parsedThreadId) payload.message_thread_id = parsedThreadId;
    sendResult = await safeSendDocument(env.BOT_TOKEN, payload);
  } else {
    const payload = {
      chat_id: targetGroupId,
      text: fullCard.slice(0, 4096),
      reply_markup: buildTicketMarkup("unreviewed"),
      parse_mode: "Markdown"
    };
    if (parsedThreadId) payload.message_thread_id = parsedThreadId;
    sendResult = await safeSendMessage(env.BOT_TOKEN, payload);
  }

  // Handle send failure with actionable diagnostic message
  if (!sendResult.ok) {
    console.error("Failed to post ticket card to group:", sendResult.description);
    await safeSendMessage(env.BOT_TOKEN, {
      chat_id: message.chat.id,
      text: `⚠️ *Delivery Failure to Group*\n\n` +
            `The bot could not forward your report to the designated group (\`${targetGroupId}\`).\n\n` +
            `*Telegram Error:* \`${sendResult.description || "Unknown error"}\`\n\n` +
            `🔧 *How to fix:*\n` +
            `1. Make sure @telebackfeedback_bot is added as a member (or Administrator) to your group.\n` +
            `2. If your group is a Supergroup, verify the Group ID starts with \`-100\`.\n` +
            `3. If Topics/Forums are enabled in your group, configure the \`TOPIC_ID\` secret.\n` +
            `4. You can type \`/id\` inside the group to verify its exact Chat ID!`,
      reply_to_message_id: message.message_id,
      parse_mode: "Markdown"
    });
    return;
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
  if (!res.ok && payload.parse_mode) {
    const desc = (res.description || "").toLowerCase();
    if (desc.includes("parse") || desc.includes("entity") || desc.includes("entities") || desc.includes("markdown") || desc.includes("tag")) {
      console.warn("Markdown parse failed, retrying plain text:", res.description);
      const plainPayload = { ...payload };
      delete plainPayload.parse_mode;
      res = await telegramApi(botToken, "sendMessage", plainPayload);
    }
  }
  return res;
}

/**
 * Robust SendDocument with fallback if Markdown entity parsing fails
 */
async function safeSendDocument(botToken, payload) {
  let res = await telegramApi(botToken, "sendDocument", payload);
  if (!res.ok && payload.parse_mode) {
    const desc = (res.description || "").toLowerCase();
    if (desc.includes("parse") || desc.includes("entity") || desc.includes("entities") || desc.includes("markdown") || desc.includes("tag")) {
      console.warn("Document caption markdown parse failed, retrying plain text:", res.description);
      const plainPayload = { ...payload };
      delete plainPayload.parse_mode;
      res = await telegramApi(botToken, "sendDocument", plainPayload);
    }
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
