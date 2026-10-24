/**
 * TeleBack Feedback Bot Webhook - Cloudflare Worker
 * 
 * Serverless Telegram webhook router for receiving diagnostics & feedback
 * from @telebackfblogs_bot and posting interactive ticket cards
 * with inline review actions to the developer group.
 * 
 * Required Secrets (Cloudflare Environment Variables):
 * - BOT_TOKEN: BotFather token for @telebackfblogs_bot
 * - GROUP_ID: Telegram Group ID (e.g. -100xxxxxxxxxx)
 * - TOPIC_ID / THREAD_ID (Optional): Forum Topic ID if group has Topics enabled
 * - SECRET_TOKEN (Optional): Telegram webhook secret token
 */

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);
    let targetGroupId = String(env.GROUP_ID || "").trim().replace(/^["']|["']$/g, '');
    // Auto-correct: If user entered positive "100...", prepend "-" for Telegram Supergroups
    if (/^100\d{8,}$/.test(targetGroupId)) {
      targetGroupId = `-${targetGroupId}`;
    }
    const topicId = (env.TOPIC_ID || env.THREAD_ID) ? String(env.TOPIC_ID || env.THREAD_ID).trim() : null;

    // 1-Click Automated Webhook Setup Endpoint: GET /set-webhook
    if (request.method === "GET" && url.pathname === "/set-webhook") {
      if (!env.BOT_TOKEN) {
        return new Response(JSON.stringify({ error: "Missing BOT_TOKEN in Cloudflare environment secrets" }, null, 2), {
          status: 400,
          headers: { "Content-Type": "application/json" }
        });
      }
      const webhookTarget = `${url.origin}/`;
      let setWebhookApi = `https://api.telegram.org/bot${env.BOT_TOKEN}/setWebhook?url=${encodeURIComponent(webhookTarget)}&drop_pending_updates=true`;
      if (env.SECRET_TOKEN) {
        setWebhookApi += `&secret_token=${encodeURIComponent(env.SECRET_TOKEN)}`;
      }

      try {
        const whRes = await fetch(setWebhookApi);
        const whData = await whRes.json();
        return new Response(JSON.stringify({
          status: whData.ok ? "SUCCESS" : "FAILED",
          message: whData.ok ? "Webhook successfully registered with Telegram!" : "Telegram rejected the webhook registration.",
          worker_webhook_url: webhookTarget,
          telegram_response: whData
        }, null, 2), {
          status: whData.ok ? 200 : 400,
          headers: { "Content-Type": "application/json" }
        });
      } catch (e) {
        return new Response(JSON.stringify({ error: e.message }, null, 2), {
          status: 500,
          headers: { "Content-Type": "application/json" }
        });
      }
    }

    // Health check & Live Telegram Diagnostic endpoint for browser/GET requests
    if (request.method !== "POST") {
      let botInfo = null;
      let webhookInfo = null;
      let groupInfo = null;

      if (env.BOT_TOKEN) {
        try {
          const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getMe`);
          botInfo = await res.json();
        } catch (e) {
          botInfo = { ok: false, error: e.message };
        }

        try {
          const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getWebhookInfo`);
          webhookInfo = await res.json();
        } catch (e) {
          webhookInfo = { ok: false, error: e.message };
        }

        if (targetGroupId) {
          try {
            const res = await fetch(`https://api.telegram.org/bot${env.BOT_TOKEN}/getChat?chat_id=${encodeURIComponent(targetGroupId)}`);
            groupInfo = await res.json();
          } catch (e) {
            groupInfo = { ok: false, error: e.message };
          }
        }
      }

      const isBotValid = botInfo?.ok === true;
      const isWebhookActive = webhookInfo?.ok === true && webhookInfo.result.url === `${url.origin}/`;
      const isGroupReachable = groupInfo?.ok === true;

      return new Response(
        JSON.stringify({
          status: "online",
          service: "TeleBack Feedback Webhook",
          bot_token_secret: env.BOT_TOKEN ? "present" : "MISSING",
          bot_identity: isBotValid ? `@${botInfo.result.username} (ID: ${botInfo.result.id})` : "INVALID_TOKEN_OR_UNAUTHORIZED",
          group_id_secret: targetGroupId ? targetGroupId : "MISSING",
          topic_id_secret: topicId ? topicId : "none",
          live_diagnostics: {
            telegram_get_me: botInfo,
            telegram_webhook_info: webhookInfo,
            group_access: groupInfo
          },
          health_summary: {
            bot_token_healthy: isBotValid,
            webhook_registered_to_worker: isWebhookActive,
            group_accessible: isGroupReachable
          },
          action_required: !isBotValid
            ? "CRITICAL: The BOT_TOKEN configured in Cloudflare Secrets is rejected by Telegram. Please copy the exact token from @BotFather for @telebackfblogs_bot and update BOT_TOKEN in Cloudflare Settings -> Variables and Secrets."
            : (!isWebhookActive
              ? `ACTION NEEDED: Webhook is NOT connected to this worker. Click or open ${url.origin}/set-webhook to connect Telegram to this worker in 1-click!`
              : (!isGroupReachable
                ? `ACTION NEEDED: Bot cannot post to group ${targetGroupId}. Make sure @${botInfo.result.username} is added to the group as an Administrator.`
                : "ALL SYSTEMS GO: Bot token, webhook routing, and group connectivity are 100% verified!"))
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
      return new Response("OK", { status: 200 });
    }
  }
};

/**
 * Handle messages sent to @telebackfblogs_bot
 */
async function handleIncomingMessage(message, env, targetGroupId, topicId) {
  const text = message.text || "";

  // 1. Group / Supergroup diagnostic handler:
  // If the bot is added to a group, typing /id or /status immediately replies with the chat ID
  if (message.chat?.type === "group" || message.chat?.type === "supergroup") {
    if (text.startsWith("/id") || text.startsWith("/chatid") || text.startsWith("/status") || text.startsWith("/start")) {
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
      "To submit a report, paste your feedback or diagnostic note below.\n" +
      "It will be forwarded directly to the developer group with full privacy.";

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
    `📩 *USER FEEDBACK TICKET* (${userInfo})\n` +
    `━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n` +
    `Status: 🔴 *UNREVIEWED*\n\n`;

  const reportContent = message.text || message.caption || (message.document ? "[Document attached]" : "[Empty note]");
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
            `1. Make sure @telebackfblogs_bot is added as an Administrator to your group.\n` +
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
