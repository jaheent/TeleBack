import os
import telebot
from telebot.types import InlineKeyboardMarkup, InlineKeyboardButton
from datetime import datetime

# Environment variables for zero-secret deployment
BOT_TOKEN = os.getenv("BOT_TOKEN")
if not BOT_TOKEN:
    raise ValueError("Missing environment variable: BOT_TOKEN must be set.")

GROUP_ID_STR = os.getenv("GROUP_ID", "").strip().strip("\"'")
if not GROUP_ID_STR:
    raise ValueError("Missing environment variable: GROUP_ID must be set.")
# Auto-correct if positive 100... supergroup format
if GROUP_ID_STR.startswith("100") and len(GROUP_ID_STR) >= 11:
    GROUP_ID_STR = f"-{GROUP_ID_STR}"
GROUP_ID = int(GROUP_ID_STR)

TOPIC_ID_STR = os.getenv("TOPIC_ID") or os.getenv("THREAD_ID")
TOPIC_ID = int(TOPIC_ID_STR.strip().strip("\"'")) if TOPIC_ID_STR else None

bot = telebot.TeleBot(BOT_TOKEN)

def build_ticket_markup(status="unreviewed"):
    markup = InlineKeyboardMarkup(row_width=3)
    if status == "unreviewed":
        markup.add(
            InlineKeyboardButton("✅ Review", callback_data="review"),
            InlineKeyboardButton("📁 Archive", callback_data="archive"),
            InlineKeyboardButton("🗑 Discard", callback_data="discard")
        )
    elif status == "reviewed":
        markup.add(
            InlineKeyboardButton("🔄 Reopen", callback_data="reopen"),
            InlineKeyboardButton("📁 Archive", callback_data="archive"),
            InlineKeyboardButton("🗑 Discard", callback_data="discard")
        )
    return markup

@bot.message_handler(func=lambda msg: msg.chat.type in ["group", "supergroup"], commands=['id', 'chatid', 'status', 'help'])
def handle_group_diagnostics(message):
    topic_info = f"\n🧵 *Topic/Thread ID:* `{message.message_thread_id}`" if getattr(message, 'message_thread_id', None) else ""
    text = (
        f"🤖 *TeleBack Feedback Bot Active*\n\n"
        f"📍 *This Group Chat ID:* `{message.chat.id}`\n"
        f"🏷️ *Group Title:* {message.chat.title or 'Group'}"
        f"{topic_info}\n\n"
        f"✅ Bot is connected to this chat.\n"
        f"To route reports here, set `GROUP_ID={message.chat.id}`"
        + (f" and `TOPIC_ID={message.message_thread_id}`" if getattr(message, 'message_thread_id', None) else "")
    )
    bot.reply_to(message, text, parse_mode="Markdown")

@bot.message_handler(commands=['start', 'help'])
def send_welcome(message):
    welcome_text = (
        "👋 *Welcome to TeleBack Feedback & Diagnostics Bot!*\n\n"
        "To submit a report, paste your feedback or diagnostic note below.\n"
        "It will be forwarded directly to the developer group with full privacy."
    )
    bot.reply_to(message, welcome_text, parse_mode="Markdown")

@bot.message_handler(func=lambda msg: True, content_types=['text', 'document'])
def handle_user_submission(message):
    # Only forward direct private messages sent to the bot by users
    if message.chat.type != "private":
        return

    user_info = f"@{message.from_user.username}" if message.from_user.username else f"User {message.from_user.id}"
    header = (
        f"📩 *USER FEEDBACK TICKET* ({user_info})\n"
        f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
        f"Status: 🔴 *UNREVIEWED*\n\n"
    )

    report_content = message.text or (message.caption or "[Document attached]")
    full_card = header + report_content

    # If document attached, forward document; otherwise send formatted markdown message
    try:
        if message.document:
            bot.send_document(
                chat_id=GROUP_ID,
                document=message.document.file_id,
                caption=full_card[:1024],
                reply_markup=build_ticket_markup("unreviewed"),
                message_thread_id=TOPIC_ID,
                parse_mode="Markdown"
            )
        else:
            bot.send_message(
                chat_id=GROUP_ID,
                text=full_card[:4096],
                reply_markup=build_ticket_markup("unreviewed"),
                message_thread_id=TOPIC_ID,
                parse_mode="Markdown"
            )
        
        bot.reply_to(message, "✅ *Ticket received!* The TeleBack developer team has been notified.", parse_mode="Markdown")
    except Exception as e:
        err_msg = str(e)
        print(f"Error forwarding report to developer group ({GROUP_ID}): {err_msg}")
        # Send actionable failure message back to user
        failure_text = (
            f"⚠️ *Delivery Failure to Group*\n\n"
            f"The bot could not forward your report to the designated group (`{GROUP_ID}`).\n\n"
            f"*Telegram Error:* `{err_msg}`\n\n"
            f"🔧 *How to fix:*\n"
            f"1. Make sure @telebackfblogs_bot is added as an Administrator to your group.\n"
            f"2. Ensure the Group ID starts with `-100`.\n"
            f"3. Type `/id` inside the group to verify its exact Chat ID."
        )
        try:
            bot.reply_to(message, failure_text, parse_mode="Markdown")
        except Exception:
            bot.reply_to(message, failure_text)

@bot.callback_query_handler(func=lambda call: True)
def handle_ticket_actions(call):
    admin_name = f"@{call.from_user.username}" if call.from_user.username else call.from_user.first_name
    timestamp = datetime.utcnow().strftime("%Y-%m-%d %H:%M UTC")
    current_text = call.message.text or (call.message.caption or "")

    if call.data == "review":
        updated_text = current_text.replace("🔴 *UNREVIEWED*", f"🟢 *REVIEWED* (by {admin_name} at {timestamp})")
        updated_text = updated_text.replace("🔴 UNREVIEWED", f"🟢 REVIEWED (by {admin_name} at {timestamp})")
        
        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=updated_text,
                reply_markup=build_ticket_markup("reviewed"),
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=updated_text[:1024],
                reply_markup=build_ticket_markup("reviewed"),
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket marked as Reviewed! 🟢")

    elif call.data == "reopen":
        updated_text = current_text
        if "🟢 *REVIEWED*" in updated_text:
            updated_text = updated_text.replace("🟢 *REVIEWED*", "🔴 *UNREVIEWED*")
        elif "🟢 REVIEWED" in updated_text:
            updated_text = updated_text.replace("🟢 REVIEWED", "🔴 UNREVIEWED")

        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=updated_text,
                reply_markup=build_ticket_markup("unreviewed"),
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=updated_text[:1024],
                reply_markup=build_ticket_markup("unreviewed"),
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket Reopened 🔴")

    elif call.data == "archive":
        archived_text = current_text.replace("🔴 *UNREVIEWED*", f"📦 *ARCHIVED* (by {admin_name})")
        archived_text = archived_text.replace("🔴 UNREVIEWED", f"📦 ARCHIVED (by {admin_name})")
        archived_text = archived_text.replace(f"🟢 *REVIEWED*", f"📦 *ARCHIVED* (by {admin_name})")
        archived_text = archived_text.replace(f"🟢 REVIEWED", f"📦 ARCHIVED (by {admin_name})")

        if call.message.text:
            bot.edit_message_text(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                text=archived_text,
                reply_markup=None,
                parse_mode="Markdown"
            )
        else:
            bot.edit_message_caption(
                chat_id=call.message.chat.id,
                message_id=call.message.message_id,
                caption=archived_text[:1024],
                reply_markup=None,
                parse_mode="Markdown"
            )
        bot.answer_callback_query(call.id, "Ticket Archived 📦")

    elif call.data == "discard":
        bot.delete_message(chat_id=call.message.chat.id, message_id=call.message.message_id)
        bot.answer_callback_query(call.id, "Ticket Discarded 🗑")

if __name__ == "__main__":
    print(f"🚀 TeleBack Feedback Bot polling active for group: {GROUP_ID} (Topic: {TOPIC_ID})...")
    bot.infinity_polling()
